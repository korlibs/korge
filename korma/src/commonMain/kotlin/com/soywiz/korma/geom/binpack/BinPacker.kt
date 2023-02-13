package com.soywiz.korma.geom.binpack

import com.soywiz.kds.FastArrayList
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korma.geom.MRectangle
import com.soywiz.korma.geom.MSize
import com.soywiz.korma.geom.Sizeable
import kotlin.collections.Iterable
import kotlin.collections.List
import kotlin.collections.filterNotNull
import kotlin.collections.hashMapOf
import kotlin.collections.map
import kotlin.collections.maxOrNull
import kotlin.collections.plusAssign
import kotlin.collections.set
import kotlin.collections.sortedByDescending
import kotlin.collections.toList

class BinPacker(val width: Double, val height: Double, val algo: Algo = MaxRects(width, height)) {
    interface Algo {
        fun add(width: Double, height: Double): MRectangle?
    }

    class Result<T>(val maxWidth: Double, val maxHeight: Double, val items: List<Pair<T, MRectangle?>>) {
        private val rectanglesNotNull = items.map { it.second }.filterNotNull()
        val width: Double = rectanglesNotNull.map { it.right }.maxOrNull() ?: 0.0
        val height: Double = rectanglesNotNull.map { it.bottom }.maxOrNull() ?: 0.0
        val rects: List<MRectangle?> get() = items.map { it.second }
        val rectsStr: String get() = rects.toString()
    }

    val allocated = FastArrayList<MRectangle>()

    fun <T> Algo.addBatch(items: Iterable<T>, getSize: (T) -> MSize): List<Pair<T, MRectangle?>> {
        val its = items.toList()
        val out = hashMapOf<T, MRectangle?>()
        val sorted = its.map { it to getSize(it) }.sortedByDescending { it.second.area }
        for ((i, size) in sorted) out[i] = this.add(size.width, size.height)
        return its.map { it to out[it] }
    }

    fun add(width: Double, height: Double): MRectangle = addOrNull(width, height)
        ?: throw ImageDoNotFitException(width, height, this)
    fun add(width: Int, height: Int): MRectangle = add(width.toDouble(), height.toDouble())
    fun add(width: Float, height: Float): MRectangle = add(width.toDouble(), height.toDouble())

    fun addOrNull(width: Double, height: Double): MRectangle? {
        val rect = algo.add(width, height) ?: return null
        allocated += rect
        return rect
    }


    fun addOrNull(width: Int, height: Int): MRectangle? = addOrNull(width.toDouble(), height.toDouble())
    fun addOrNull(width: Float, height: Float): MRectangle? = addOrNull(width.toDouble(), height.toDouble())

    fun <T> addBatch(items: Iterable<T>, getSize: (T) -> MSize): Result<T> {
        return Result(width, height, algo.addBatch(items, getSize))
    }

    fun addBatch(items: Iterable<MSize>): List<MRectangle?> = algo.addBatch(items) { it }.map { it.second }

    companion object {
        operator fun invoke(width: Double, height: Double, algo: Algo = MaxRects(width, height)) = BinPacker(width, height, algo)
        operator fun invoke(width: Int, height: Int, algo: Algo = MaxRects(width.toDouble(), height.toDouble())) = BinPacker(width.toDouble(), height.toDouble(), algo)
        operator fun invoke(width: Float, height: Float, algo: Algo = MaxRects(width.toDouble(), height.toDouble())) = BinPacker(width.toDouble(), height.toDouble(), algo)

        fun <T> pack(width: Double, height: Double, items: Iterable<T>, getSize: (T) -> MSize): Result<T> = BinPacker(width, height).addBatch(items, getSize)
        fun <T> pack(width: Int, height: Int, items: Iterable<T>, getSize: (T) -> MSize): Result<T> = pack(width.toDouble(), height.toDouble(), items, getSize)
        fun <T> pack(width: Float, height: Float, items: Iterable<T>, getSize: (T) -> MSize): Result<T> = pack(width.toDouble(), height.toDouble(), items, getSize)

        fun <T> packSeveral(
            maxWidth: Double,
            maxHeight: Double,
            items: Iterable<T>,
            getSize: (T) -> MSize
        ): List<Result<T>> {
            var currentBinPacker = BinPacker(maxWidth, maxHeight)
            var currentPairs = FastArrayList<Pair<T, MRectangle>>()
            val sortedItems = items.sortedByDescending { getSize(it).area }
            sortedItems.fastForEach {
                val size = getSize(it)
                if (size.width > maxWidth || size.height > maxHeight) {
                    throw ImageDoNotFitException(size.width, size.height, currentBinPacker)
                }
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

    class ImageDoNotFitException(val width: Double, val height: Double, val packer: BinPacker) : Throwable(
        "Size '${width}x${height}' doesn't fit in '${packer.width}x${packer.height}'"
    )
}
