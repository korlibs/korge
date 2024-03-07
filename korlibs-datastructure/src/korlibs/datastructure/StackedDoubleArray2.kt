package korlibs.datastructure

import korlibs.datastructure.ds.*
import korlibs.datastructure.internal.memory.Memory.arraycopy
import korlibs.datastructure.iterators.*
import kotlin.math.*

interface IStackedDoubleArray2 : IStackedArray2<Double> {
    /** The [empty] value that will be returned if the specified cell it out of bounds, or empty */
    val empty: Double

    /** Duplicates the contents of this [IStackedDoubleArray2] keeping its contents data */
    fun clone(): IStackedDoubleArray2

    /** Sets the [value] at [x], [y] at [level], [startX] and [startY] are NOT used here so 0,0 means the top-left element */
    operator fun set(x: Int, y: Int, level: Int, value: Double)
    /** Gets the value at [x], [y] at [level], [startX] and [startY] are NOT used here so 0,0 means the top-left element */
    operator fun get(x: Int, y: Int, level: Int): Double

    /** Adds a new [value] on top of [x], [y] */
    fun push(x: Int, y: Int, value: Double) {
        set(x, y, getStackLevel(x, y), value)
    }

    /** Set the first [value] of a stack in the cell [x], [y] */
    fun setFirst(x: Int, y: Int, value: Double) {
        set(x, y, 0, value)
    }

    /** Gets the first value of the stack in the cell [x], [y] */
    fun getFirst(x: Int, y: Int): Double {
        val level = getStackLevel(x, y)
        if (level == 0) return empty
        return get(x, y, 0)
    }

    /** Gets the last value of the stack in the cell [x], [y] */
    fun getLast(x: Int, y: Int): Double {
        val level = getStackLevel(x, y)
        if (level == 0) return empty
        return get(x, y, level - 1)
    }
}

/** Shortcut for [IStackedDoubleArray2.startX] + [IStackedDoubleArray2.width] */
val IStackedDoubleArray2.endX: Int get() = startX + width
/** Shortcut for [IStackedDoubleArray2.startY] + [IStackedDoubleArray2.height] */
val IStackedDoubleArray2.endY: Int get() = startY + height

class StackedDoubleArray2(
    override val width: Int,
    override val height: Int,
    override val empty: Double = EMPTY,
    override val startX: Int = 0,
    override val startY: Int = 0,
) : IStackedDoubleArray2 {
    override fun clone(): StackedDoubleArray2 {
        return StackedDoubleArray2(width, height, empty, startX, startY).also { out ->
            arraycopy(this.level.data, 0, out.level.data, 0, out.level.data.size)
            out.data.addAll(this.data.map { it.clone() })
        }
    }

    val level = IntArray2(width, height, 0)
    val data = fastArrayListOf<DoubleArray2>()

    override val maxLevel: Int get() = data.size

    companion object {
        const val EMPTY = Double.NaN

        operator fun invoke(
            vararg layers: DoubleArray2,
            width: Int = layers.first().width,
            height: Int = layers.first().height,
            empty: Double = EMPTY,
            startX: Int = 0,
            startY: Int = 0,
        ): StackedDoubleArray2 {
            val stacked = StackedDoubleArray2(width, height, empty, startX = startX, startY = startY)
            stacked.level.fill { layers.size }
            stacked.data.addAll(layers)
            return stacked
        }
    }

    fun ensureLevel(level: Int) {
        while (level >= data.size) data.add(DoubleArray2(width, height, empty))
    }

    fun setLayer(level: Int, data: DoubleArray2) {
        ensureLevel(level)
        this.data[level] = data
    }

    override operator fun set(x: Int, y: Int, level: Int, value: Double) {
        ensureLevel(level)
        data[level][x, y] = value
        this.level[x, y] = maxOf(this.level[x, y], level + 1)
    }

    override operator fun get(x: Int, y: Int, level: Int): Double {
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

fun DoubleArray2.toStacked(): StackedDoubleArray2 = StackedDoubleArray2(this)

class SparseChunkedStackedDoubleArray2(override var empty: Double = StackedDoubleArray2.EMPTY) : IStackedDoubleArray2 {
    constructor(vararg layers: IStackedDoubleArray2, empty: Double = StackedDoubleArray2.EMPTY) : this(empty) {
        layers.fastForEach { putChunk(it) }
    }

    var minX = 0
    var minY = 0
    var maxX = 0
    var maxY = 0
    override var maxLevel: Int = 0

    val bvh = BVH<IStackedDoubleArray2>(dimensions = 2)
    var first: IStackedDoubleArray2? = null
    var last: IStackedDoubleArray2? = null

    fun putChunk(chunk: IStackedDoubleArray2) {
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

    fun findAllChunks(): List<IStackedDoubleArray2> = bvh.findAllValues()

    private var lastSearchChunk: IStackedDoubleArray2? = null

    private fun IStackedDoubleArray2.chunkX(x: Int): Int = x - this.startX
    private fun IStackedDoubleArray2.chunkY(y: Int): Int = y - this.startY
    private fun IStackedDoubleArray2.containsChunk(x: Int, y: Int): Boolean {
        return x in startX until endX && y in startY until endY
    }

    fun getChunkAt(x: Int, y: Int): IStackedDoubleArray2? {
        // Cache to be much faster while iterating rows
        lastSearchChunk?.let {
            if (it.containsChunk(x, y)) return it
        }
        lastSearchChunk = bvh.searchValues(BVHIntervals(x, 1, y, 1)).firstOrNull()
        return lastSearchChunk
    }

    override fun inside(x: Int, y: Int): Boolean = getChunkAt(x, y) != null

    override fun set(x: Int, y: Int, level: Int, value: Double) {
        getChunkAt(x, y)?.let { chunk ->
            chunk[chunk.chunkX(x), chunk.chunkY(y), level] = value
        }
    }

    override fun get(x: Int, y: Int, level: Int): Double {
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

    override fun clone(): SparseChunkedStackedDoubleArray2 = SparseChunkedStackedDoubleArray2(empty).also { sparse ->
        findAllChunks().fastForEach {
            sparse.putChunk(it.clone())
        }
    }
}
