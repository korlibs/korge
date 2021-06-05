package com.soywiz.korma.geom.binpack

import com.soywiz.kds.*
import com.soywiz.korma.geom.*

class BinPacker(val width: Double, val height: Double, val algo: Algo = MaxRects(width, height)) {
    interface Algo {
        fun add(width: Double, height: Double): Rectangle?
    }

    class Result<T>(val maxWidth: Double, val maxHeight: Double, val items: List<Pair<T, Rectangle?>>) {
        private val rectanglesNotNull = items.map { it.second }.filterNotNull()
        val width: Double = rectanglesNotNull.map { it.right }.maxOrNull() ?: 0.0
        val height: Double = rectanglesNotNull.map { it.bottom }.maxOrNull() ?: 0.0
        val rects: List<Rectangle?> get() = items.map { it.second }
        val rectsStr: String get() = rects.toString()
    }

    val allocated = FastArrayList<Rectangle>()

    fun <T> Algo.addBatch(items: Iterable<T>, getSize: (T) -> Size): List<Pair<T, Rectangle?>> {
        val its = items.toList()
        val out = hashMapOf<T, Rectangle?>()
        val sorted = its.map { it to getSize(it) }.sortedByDescending { it.second.area }
        for ((i, size) in sorted) out[i] = this.add(size.width, size.height)
        return its.map { it to out[it] }
    }

    fun add(width: Double, height: Double): Rectangle = addOrNull(width, height)
        ?: throw IllegalStateException("Size '${this.width}x${this.height}' doesn't fit in '${this.width}x${this.height}'")
    fun add(width: Int, height: Int): Rectangle = add(width.toDouble(), height.toDouble())
    fun add(width: Float, height: Float): Rectangle = add(width.toDouble(), height.toDouble())

    fun addOrNull(width: Double, height: Double): Rectangle? {
        val rect = algo.add(width, height) ?: return null
        allocated += rect
        return rect
    }


    fun addOrNull(width: Int, height: Int): Rectangle? = addOrNull(width.toDouble(), height.toDouble())
    fun addOrNull(width: Float, height: Float): Rectangle? = addOrNull(width.toDouble(), height.toDouble())

    fun <T> addBatch(items: Iterable<T>, getSize: (T) -> Size): Result<T> {
        return Result(width, height, algo.addBatch(items, getSize))
    }

    fun addBatch(items: Iterable<Size>): List<Rectangle?> = algo.addBatch(items) { it }.map { it.second }

    companion object {
        operator fun invoke(width: Double, height: Double, algo: Algo = MaxRects(width, height)) = BinPacker(width, height, algo)
        operator fun invoke(width: Int, height: Int, algo: Algo = MaxRects(width.toDouble(), height.toDouble())) = BinPacker(width.toDouble(), height.toDouble(), algo)
        operator fun invoke(width: Float, height: Float, algo: Algo = MaxRects(width.toDouble(), height.toDouble())) = BinPacker(width.toDouble(), height.toDouble(), algo)

        fun <T> pack(width: Double, height: Double, items: Iterable<T>, getSize: (T) -> Size): Result<T> = BinPacker(width, height).addBatch(items, getSize)
        fun <T> pack(width: Int, height: Int, items: Iterable<T>, getSize: (T) -> Size): Result<T> = pack(width.toDouble(), height.toDouble(), items, getSize)
        fun <T> pack(width: Float, height: Float, items: Iterable<T>, getSize: (T) -> Size): Result<T> = pack(width.toDouble(), height.toDouble(), items, getSize)

        fun <T> packSeveral(
            maxWidth: Double,
            maxHeight: Double,
            items: Iterable<T>,
            getSize: (T) -> Size
        ): List<Result<T>> {
            var currentBinPacker = BinPacker(maxWidth, maxHeight)
            var currentPairs = FastArrayList<Pair<T, Rectangle>>()
            val sortedItems = items.sortedByDescending { getSize(it).area }
            if (sortedItems.any { getSize(it).let { size -> size.width > maxWidth || size.height > maxHeight } }) {
                throw IllegalArgumentException("Item is bigger than max size")
            }

            val out = FastArrayList<Result<T>>()

            fun emit() {
                if (currentPairs.isEmpty()) return
                out += Result(maxWidth, maxHeight, currentPairs.toList())
                currentPairs = FastArrayList()
                currentBinPacker = BinPacker(maxWidth, maxHeight)
            }

            //for (item in items) {
            //	var done = false
            //	while (!done) {
            //		try {
            //			val size = getSize(item)
            //			val rect = currentBinPacker.add(size.width, size.height)
            //			currentPairs.add(item to rect)
            //			done = true
            //		} catch (e: IllegalStateException) {
            //			emit()
            //		}
            //	}
            //}

            for (item in items) {
                var done = false
                while (!done) {
                    val size = getSize(item)
                    val rect = currentBinPacker.addOrNull(size.width, size.height)
                    if (rect != null) {
                        currentPairs.add(item to rect)
                        done = true
                    } else {
                        emit()
                    }
                }
            }
            emit()

            return out
        }
        fun <T : Sizeable> packSeveral(maxWidth: Int, maxHeight: Int, items: Iterable<T>): List<Result<T>> = packSeveral(maxWidth.toDouble(), maxHeight.toDouble(), items) { it.size }
        fun <T : Sizeable> packSeveral(maxWidth: Float, maxHeight: Float, items: Iterable<T>): List<Result<T>> = packSeveral(maxWidth.toDouble(), maxHeight.toDouble(), items) { it.size }

    }
}
