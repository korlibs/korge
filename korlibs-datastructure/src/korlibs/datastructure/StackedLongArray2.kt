package korlibs.datastructure

import korlibs.datastructure.ds.*
import korlibs.datastructure.internal.memory.Memory.arraycopy
import korlibs.datastructure.iterators.*
import kotlin.math.*

interface IStackedLongArray2 : IStackedArray2<Long> {
    /** The [empty] value that will be returned if the specified cell it out of bounds, or empty */
    val empty: Long

    /** Duplicates the contents of this [IStackedLongArray2] keeping its contents data */
    fun clone(): IStackedLongArray2

    /** Sets the [value] at [x], [y] at [level], [startX] and [startY] are NOT used here so 0,0 means the top-left element */
    operator fun set(x: Int, y: Int, level: Int, value: Long)
    /** Gets the value at [x], [y] at [level], [startX] and [startY] are NOT used here so 0,0 means the top-left element */
    operator fun get(x: Int, y: Int, level: Int): Long

    /** Adds a new [value] on top of [x], [y] */
    fun push(x: Int, y: Int, value: Long) {
        set(x, y, getStackLevel(x, y), value)
    }

    /** Set the first [value] of a stack in the cell [x], [y] */
    fun setFirst(x: Int, y: Int, value: Long) {
        set(x, y, 0, value)
    }

    /** Gets the first value of the stack in the cell [x], [y] */
    fun getFirst(x: Int, y: Int): Long {
        val level = getStackLevel(x, y)
        if (level == 0) return empty
        return get(x, y, 0)
    }

    /** Gets the last value of the stack in the cell [x], [y] */
    fun getLast(x: Int, y: Int): Long {
        val level = getStackLevel(x, y)
        if (level == 0) return empty
        return get(x, y, level - 1)
    }
}

/** Shortcut for [IStackedLongArray2.startX] + [IStackedLongArray2.width] */
val IStackedLongArray2.endX: Int get() = startX + width
/** Shortcut for [IStackedLongArray2.startY] + [IStackedLongArray2.height] */
val IStackedLongArray2.endY: Int get() = startY + height

class StackedLongArray2(
    override val width: Int,
    override val height: Int,
    override val empty: Long = EMPTY,
    override val startX: Int = 0,
    override val startY: Int = 0,
) : IStackedLongArray2 {
    override fun clone(): StackedLongArray2 {
        return StackedLongArray2(width, height, empty, startX, startY).also { out ->
            arraycopy(this.level.data, 0, out.level.data, 0, out.level.data.size)
            out.data.addAll(this.data.map { it.clone() })
        }
    }

    val level = IntArray2(width, height, 0)
    val data = fastArrayListOf<LongArray2>()

    override val maxLevel: Int get() = data.size

    companion object {
        const val EMPTY: Long = -1L

        operator fun invoke(
            vararg layers: LongArray2,
            width: Int = layers.first().width,
            height: Int = layers.first().height,
            empty: Long = EMPTY,
            startX: Int = 0,
            startY: Int = 0,
        ): StackedLongArray2 {
            val stacked = StackedLongArray2(width, height, empty, startX = startX, startY = startY)
            stacked.level.fill { layers.size }
            stacked.data.addAll(layers)
            return stacked
        }
    }

    fun ensureLevel(level: Int) {
        while (level >= data.size) data.add(LongArray2(width, height, empty))
    }

    fun setLayer(level: Int, data: LongArray2) {
        ensureLevel(level)
        this.data[level] = data
    }

    override operator fun set(x: Int, y: Int, level: Int, value: Long) {
        ensureLevel(level)
        data[level][x, y] = value
        this.level[x, y] = maxOf(this.level[x, y], level + 1)
    }

    override operator fun get(x: Int, y: Int, level: Int): Long {
        if (level > this.level[x, y]) return empty
        return data[level][x, y]
    }

    override fun getStackLevel(x: Int, y: Int): Int {
        return this.level[x, y]
    }

    override fun removeLast(x: Int, y: Int) {
        level[x, y] = (level[x, y] - 1).coerceAtLeast(0)
    }
}

fun LongArray2.toStacked(): StackedLongArray2 = StackedLongArray2(this)

class SparseChunkedStackedLongArray2(override var empty: Long = StackedLongArray2.EMPTY) : IStackedLongArray2 {
    constructor(vararg layers: IStackedLongArray2, empty: Long = StackedLongArray2.EMPTY) : this(empty) {
        layers.fastForEach { putChunk(it) }
    }

    var minX = 0
    var minY = 0
    var maxX = 0
    var maxY = 0
    override var maxLevel: Int = 0

    val bvh = BVH<IStackedLongArray2>(dimensions = 2)
    var first: IStackedLongArray2? = null
    var last: IStackedLongArray2? = null

    fun putChunk(chunk: IStackedLongArray2) {
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

    fun findAllChunks(): List<IStackedLongArray2> = bvh.findAllValues()

    private var lastSearchChunk: IStackedLongArray2? = null

    private fun IStackedLongArray2.chunkX(x: Int): Int = x - this.startX
    private fun IStackedLongArray2.chunkY(y: Int): Int = y - this.startY
    private fun IStackedLongArray2.containsChunk(x: Int, y: Int): Boolean {
        return x in startX until endX && y in startY until endY
    }

    fun getChunkAt(x: Int, y: Int): IStackedLongArray2? {
        // Cache to be much faster while iterating rows
        lastSearchChunk?.let {
            if (it.containsChunk(x, y)) return it
        }
        lastSearchChunk = bvh.searchValues(BVHIntervals(x, 1, y, 1)).firstOrNull()
        return lastSearchChunk
    }

    override fun inside(x: Int, y: Int): Boolean = getChunkAt(x, y) != null

    override fun set(x: Int, y: Int, level: Int, value: Long) {
        getChunkAt(x, y)?.let { chunk ->
            chunk[chunk.chunkX(x), chunk.chunkY(y), level] = value
        }
    }

    override fun get(x: Int, y: Int, level: Int): Long {
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

    override fun removeLast(x: Int, y: Int) {
        getChunkAt(x, y)?.let { chunk ->
            chunk.removeLast(chunk.chunkX(x), chunk.chunkY(y))
        }
    }

    override fun clone(): SparseChunkedStackedLongArray2 = SparseChunkedStackedLongArray2(empty).also { sparse ->
        findAllChunks().fastForEach {
            sparse.putChunk(it.clone())
        }
    }
}
