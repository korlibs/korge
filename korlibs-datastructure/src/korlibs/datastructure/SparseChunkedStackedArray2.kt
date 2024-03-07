package korlibs.datastructure

import korlibs.datastructure.ds.*
import kotlin.math.*

abstract class SparseChunkedStackedArray2<TStackedArray2 : IStackedArray2Base>() : IStackedArray2Base {
    override var contentVersion: Int = 0 ; protected set
    var minX = 0
    var minY = 0
    var maxX = 0
    var maxY = 0
    override var maxLevel: Int = 0

    val bvh = BVH<TStackedArray2>(dimensions = 2)
    var first: TStackedArray2? = null
    var last: TStackedArray2? = null

    protected abstract fun setEmptyFromChunk(chunk: TStackedArray2)

    fun putChunk(chunk: TStackedArray2) {
        if (first == null) {
            first = chunk
            setEmptyFromChunk(chunk)
            minX = Int.MAX_VALUE
            minY = Int.MAX_VALUE
            maxX = Int.MIN_VALUE
            maxY = Int.MIN_VALUE
        }
        last = chunk
        bvh.insertOrUpdate(
            BVHIntervals(chunk.startX, chunk.width, chunk.startY, chunk.height),
            chunk
        )
        minX = min(minX, chunk.startX)
        minY = min(minY, chunk.startY)
        maxX = max(maxX, chunk.endX)
        maxY = max(maxY, chunk.endY)
        maxLevel = max(maxLevel, chunk.maxLevel)
        contentVersion++
    }

    override val startX: Int get() = minX
    override val startY: Int get() = minY
    override val width: Int get() = maxX - minX
    override val height: Int get() = maxY - minY

    fun findAllChunks(): List<TStackedArray2> = bvh.findAllValues()
    protected var lastSearchChunk: TStackedArray2? = null

    protected fun TStackedArray2.chunkX(x: Int): Int = x - this.startX
    protected fun TStackedArray2.chunkY(y: Int): Int = y - this.startY
    protected fun TStackedArray2.containsChunk(x: Int, y: Int): Boolean {
        return x in startX until endX && y in startY until endY
    }

    fun getChunkAt(x: Int, y: Int): TStackedArray2? {
        // Cache to be much faster while iterating rows
        lastSearchChunk?.let {
            if (it.containsChunk(x, y)) return it
        }
        lastSearchChunk = bvh.searchValues(BVHIntervals(x, 1, y, 1)).firstOrNull()
        return lastSearchChunk
    }

    override fun inside(x: Int, y: Int): Boolean = getChunkAt(x, y) != null

    override fun getStackLevel(x: Int, y: Int): Int {
        getChunkAt(x, y)?.let { chunk ->
            return chunk.getStackLevel(chunk.chunkX(x), chunk.chunkY(y))
        }
        return 0
    }

    override fun removeLast(x: Int, y: Int) {
        getChunkAt(x, y)?.let { chunk ->
            chunk.removeLast(chunk.chunkX(x), chunk.chunkY(y))
            contentVersion++
        }
    }

    override fun eachPosition(block: (x: Int, y: Int) -> Unit) {
        for (chunk in findAllChunks()) {
            chunk.eachPosition { x, y ->
                block(chunk.startX + x, chunk.startY + y)
            }
        }
    }
}
