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
    override var contentVersion: Int = 0 ; private set

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
        contentVersion++
    }

    override operator fun set(x: Int, y: Int, level: Int, value: Double) {
        ensureLevel(level)
        data[level][x, y] = value
        this.level[x, y] = maxOf(this.level[x, y], level + 1)
        contentVersion++
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
        contentVersion++
    }
}

fun DoubleArray2.toStacked(): StackedDoubleArray2 = StackedDoubleArray2(this)

class SparseChunkedStackedDoubleArray2(override var empty: Double = StackedDoubleArray2.EMPTY) : SparseChunkedStackedArray2<IStackedDoubleArray2>(), IStackedDoubleArray2 {
    constructor(vararg layers: IStackedDoubleArray2, empty: Double = StackedDoubleArray2.EMPTY) : this(empty) {
        layers.fastForEach { putChunk(it) }
    }

    override fun setEmptyFromChunk(chunk: IStackedDoubleArray2) {
        empty = chunk.empty
    }

    override fun set(x: Int, y: Int, level: Int, value: Double) {
        getChunkAt(x, y)?.let { chunk ->
            chunk[chunk.chunkX(x), chunk.chunkY(y), level] = value
            contentVersion++
        }
    }

    override fun get(x: Int, y: Int, level: Int): Double {
        getChunkAt(x, y)?.let { chunk ->
            return chunk[chunk.chunkX(x), chunk.chunkY(y), level]
        }
        return empty
    }

    override fun clone(): SparseChunkedStackedDoubleArray2 = SparseChunkedStackedDoubleArray2(empty).also { sparse ->
        findAllChunks().fastForEach {
            sparse.putChunk(it.clone())
        }
    }
}
