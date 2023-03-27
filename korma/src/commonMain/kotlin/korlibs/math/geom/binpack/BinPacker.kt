package korlibs.math.geom.binpack

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.math.geom.*
import kotlin.collections.set

class BinPacker(val size: Size, val algo: Algo = MaxRects(size)) {
    val width: Float get() = size.width
    val height: Float get() = size.height

    interface Algo {
        fun add(size: Size): Rectangle?
    }

    class Result<T>(val maxWidth: Float, val maxHeight: Float, val items: List<Pair<T, Rectangle?>>) {
        private val rectanglesNotNull = items.map { it.second }.filterNotNull()
        val width: Float = rectanglesNotNull.map { it.right }.maxOrNull() ?: 0f
        val height: Float = rectanglesNotNull.map { it.bottom }.maxOrNull() ?: 0f
        val rects: List<Rectangle?> get() = items.map { it.second }
        val rectsStr: String get() = rects.toString()
    }

    val allocated = FastArrayList<Rectangle>()

    fun <T> Algo.addBatch(items: Iterable<T>, getSize: (T) -> Size): List<Pair<T, Rectangle?>> {
        val its = items.toList()
        val out = hashMapOf<T, Rectangle?>()
        val sorted = its.map { it to getSize(it) }.sortedByDescending { it.second.area }
        for ((i, size) in sorted) out[i] = this.add(size)
        return its.map { it to out[it] }
    }

    fun add(width: Double, height: Double): Rectangle = addOrNull(width, height)
        ?: throw ImageDoNotFitException(width, height, this)
    fun add(width: Int, height: Int): Rectangle = add(width.toDouble(), height.toDouble())
    fun add(width: Float, height: Float): Rectangle = add(width.toDouble(), height.toDouble())

    fun addOrNull(width: Double, height: Double): Rectangle? {
        val rect = algo.add(Size(width, height)) ?: return null
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
        operator fun invoke(width: Double, height: Double, algo: Algo = MaxRects(width, height)) = BinPacker(Size(width, height), algo)
        operator fun invoke(width: Int, height: Int, algo: Algo = MaxRects(width.toDouble(), height.toDouble())) = BinPacker(width.toDouble(), height.toDouble(), algo)
        operator fun invoke(width: Float, height: Float, algo: Algo = MaxRects(width.toDouble(), height.toDouble())) = BinPacker(width.toDouble(), height.toDouble(), algo)

        fun <T> pack(width: Double, height: Double, items: Iterable<T>, getSize: (T) -> Size): Result<T> = BinPacker(width, height).addBatch(items, getSize)
        fun <T> pack(width: Int, height: Int, items: Iterable<T>, getSize: (T) -> Size): Result<T> = pack(width.toDouble(), height.toDouble(), items, getSize)
        fun <T> pack(width: Float, height: Float, items: Iterable<T>, getSize: (T) -> Size): Result<T> = pack(width.toDouble(), height.toDouble(), items, getSize)

        fun <T> packSeveral(
            maxSize: Size,
            items: Iterable<T>,
            getSize: (T) -> Size
        ): List<Result<T>> {
            val (maxWidth, maxHeight) = maxSize
            var currentBinPacker = BinPacker(maxWidth, maxHeight)
            var currentPairs = FastArrayList<Pair<T, Rectangle>>()
            val sortedItems = items.sortedByDescending { getSize(it).area }
            sortedItems.fastForEach {
                val size = getSize(it)
                if (size.width > maxWidth || size.height > maxHeight) {
                    throw ImageDoNotFitException(size.widthD, size.heightD, currentBinPacker)
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

        fun <T : Sizeable> packSeveral(maxSize: Size, items: Iterable<T>): List<Result<T>> = packSeveral(maxSize, items) { it.size }
    }

    class ImageDoNotFitException(val width: Double, val height: Double, val packer: BinPacker) : Throwable(
        "Size '${width}x${height}' doesn't fit in '${packer.width}x${packer.height}'"
    )
}
