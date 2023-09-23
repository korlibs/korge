@file:Suppress("PackageDirectoryMismatch")

package korlibs.math.geom.binpack

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.math.geom.*
import kotlin.collections.set

class BinPacker(val size: Size, val algo: Algo = MaxRects(size)) {
    val width: Double get() = size.width
    val height: Double get() = size.height

    interface Algo {
        fun add(size: Size): Rectangle?
    }

    class Result<T>(val maxWidth: Double, val maxHeight: Double, val items: List<Pair<T, Rectangle?>>) {
        private val rectanglesNotNull = items.mapNotNull { it.second }
        val width: Double = rectanglesNotNull.maxOfOrNull { it.right } ?: 0.0
        val height: Double = rectanglesNotNull.maxOfOrNull { it.bottom } ?: 0.0
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

        fun <T : Sizeable> packSeveral(maxSize: Size, items: Iterable<T>): List<Result<T>> = packSeveral(maxSize, items) { it.size }
    }

    class ImageDoNotFitException(val width: Double, val height: Double, val packer: BinPacker) : Throwable(
        "Size '${width}x${height}' doesn't fit in '${packer.width}x${packer.height}'"
    )
}


class MaxRects(maxSize: Size) : BinPacker.Algo {
    constructor(width: Float, height: Float) : this(Size(width, height))
    constructor(width: Double, height: Double) : this(Size(width, height))

    var freeRectangles = fastArrayListOf(Rectangle(Point.ZERO, maxSize))

    override fun add(size: Size): Rectangle? = quickInsert(size)

    fun quickInsert(size: Size): Rectangle? {
        val (width, height) = size
        if (width <= 0.0 && height <= 0.0) return Rectangle(0, 0, 0, 0)
        val newNode = quickFindPositionForNewNodeBestAreaFit(width, height)

        if (newNode.height == 0.0) return null

        var numRectanglesToProcess = freeRectangles.size
        var i = 0
        while (i < numRectanglesToProcess) {
            if (splitFreeNode(freeRectangles[i], newNode)) {
                freeRectangles.removeAt(i)
                --numRectanglesToProcess
                --i
            }
            i++
        }

        pruneFreeList()
        return newNode
    }

    private fun quickFindPositionForNewNodeBestAreaFit(width: Double, height: Double): Rectangle {
        var score = Double.MAX_VALUE
        var areaFit: Double
        var bestNode = Rectangle()

        for (r in freeRectangles) {
            // Try to place the rectangle in upright (non-flipped) orientation.
            if (r.width >= width && r.height >= height) {
                areaFit = (r.width * r.height - width * height).toDouble()
                if (areaFit < score) {
                    bestNode = Rectangle(r.x, r.y, width, height)
                    score = areaFit
                }
            }
        }

        return bestNode
    }

    private fun splitFreeNode(freeNode: Rectangle, usedNode: Rectangle): Boolean {
        var newNode: Rectangle
        // Test with SAT if the rectangles even intersect.
        if (usedNode.left >= freeNode.right || usedNode.right <= freeNode.x || usedNode.top >= freeNode.bottom || usedNode.bottom <= freeNode.top) {
            return false
        }
        if (usedNode.x < freeNode.right && usedNode.right > freeNode.x) {
            // New node at the top side of the used node.
            if (usedNode.y > freeNode.y && usedNode.y < freeNode.bottom) {
                newNode = freeNode.copy(height = usedNode.y - freeNode.y)
                freeRectangles.add(newNode)
            }
            // New node at the bottom side of the used node.
            if (usedNode.bottom < freeNode.bottom) {
                newNode = freeNode.copy(
                    y = usedNode.bottom,
                    height = freeNode.bottom - usedNode.bottom
                )
                freeRectangles.add(newNode)
            }
        }
        if (usedNode.y < freeNode.bottom && usedNode.bottom > freeNode.y) {
            // New node on the left side of the used node.
            if (usedNode.x > freeNode.x && usedNode.x < freeNode.right) {
                newNode = freeNode.copy(width = usedNode.x - freeNode.x)
                freeRectangles.add(newNode)
            }
            // New node on the right side of the used node.
            if (usedNode.right < freeNode.right) {
                newNode = freeNode.copy(
                    x = usedNode.right,
                    width = freeNode.right - usedNode.right
                )
                freeRectangles.add(newNode)
            }
        }
        return true
    }

    private fun pruneFreeList() {
        // Go through each pair and remove any rectangle that is redundant.
        var len = freeRectangles.size
        var i = 0
        while (i < len) {
            var j = i + 1
            val tmpRect = freeRectangles[i]
            while (j < len) {
                val tmpRect2 = freeRectangles[j]
                if (Rectangle.isContainedIn(tmpRect, tmpRect2)) {
                    freeRectangles.removeAt(i)
                    --i
                    --len
                    break
                }
                if (Rectangle.isContainedIn(tmpRect2, tmpRect)) {
                    freeRectangles.removeAt(j)
                    --len
                    --j
                }
                j++
            }
            i++
        }
    }
}
