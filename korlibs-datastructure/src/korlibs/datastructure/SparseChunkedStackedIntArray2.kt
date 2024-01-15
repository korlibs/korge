package korlibs.datastructure

import korlibs.datastructure.ds.BVH
import korlibs.datastructure.ds.BVHIntervals
import korlibs.datastructure.iterators.fastForEach
import kotlin.math.max
import kotlin.math.min

class SparseChunkedStackedIntArray2(override var empty: Int = -1) : IStackedIntArray2 {

    constructor(vararg layers: IStackedIntArray2, empty: Int = -1) : this(empty) {
        layers.fastForEach { putChunk(it) }
    }

    var minX = 0
    var minY = 0
    var maxX = 0
    var maxY = 0
    override var maxLevel: Int = 0

    val bvh = BVH<IStackedIntArray2>(dimensions = 2)
    var first: IStackedIntArray2? = null
    var last: IStackedIntArray2? = null

    fun putChunk(chunk: IStackedIntArray2) {
        if (first == null) {
            first = chunk
            empty = chunk.empty
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
    }

    override val startX: Int get() = minX
    override val startY: Int get() = minY
    override val width: Int get() = maxX - minX
    override val height: Int get() = maxY - minY

    fun findAllChunks(): List<IStackedIntArray2> = bvh.findAllValues()

    private var lastSearchChunk: IStackedIntArray2? = null

    private fun IStackedIntArray2.chunkX(x: Int): Int = x - this.startX
    private fun IStackedIntArray2.chunkY(y: Int): Int = y - this.startY
    private fun IStackedIntArray2.containsChunk(x: Int, y: Int): Boolean {
        return x in startX until endX && y in startY until endY
    }

    fun getChunkAt(x: Int, y: Int): IStackedIntArray2? {
        // Cache to be much faster while iterating rows
        lastSearchChunk?.let {
            if (it.containsChunk(x, y)) return it
        }
        lastSearchChunk = bvh.searchValues(BVHIntervals(x, 1, y, 1)).firstOrNull()
        return lastSearchChunk
    }

    override fun inside(x: Int, y: Int): Boolean = getChunkAt(x, y) != null

    override fun set(x: Int, y: Int, level: Int, value: Int) {
        getChunkAt(x, y)?.let { chunk ->
            chunk[chunk.chunkX(x), chunk.chunkY(y), level] = value
        }
    }

    override fun get(x: Int, y: Int, level: Int): Int {
        getChunkAt(x, y)?.let { chunk ->
            return chunk[chunk.chunkX(x), chunk.chunkY(y), level]
        }
        return empty
    }

    override fun getStackLevel(x: Int, y: Int): Int {
        getChunkAt(x, y)?.let { chunk ->
            return chunk.getStackLevel(chunk.chunkX(x), chunk.chunkY(y))
        }
        return 0
    }

    override fun push(x: Int, y: Int, value: Int) {
        getChunkAt(x, y)?.let { chunk ->
            chunk.push(chunk.chunkX(x), chunk.chunkY(y), value)
        }
    }

    override fun removeLast(x: Int, y: Int) {
        getChunkAt(x, y)?.let { chunk ->
            chunk.removeLast(chunk.chunkX(x), chunk.chunkY(y))
        }
    }

    override fun clone(): SparseChunkedStackedIntArray2 = SparseChunkedStackedIntArray2(empty).also { sparse ->
        findAllChunks().fastForEach {
            sparse.putChunk(it.clone())
        }
    }
}
