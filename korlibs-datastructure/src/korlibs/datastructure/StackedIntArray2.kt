package korlibs.datastructure

import korlibs.datastructure.ds.*
import korlibs.datastructure.internal.memory.Memory.arraycopy
import korlibs.datastructure.iterators.*
import kotlin.math.*

interface IStackedIntArray2 : IStackedArray2<Int> {
    /** The [empty] value that will be returned if the specified cell it out of bounds, or empty */
    val empty: Int

    /** Duplicates the contents of this [IStackedIntArray2] keeping its contents data */
    fun clone(): IStackedIntArray2

    /** Sets the [value] at [x], [y] at [level], [startX] and [startY] are NOT used here so 0,0 means the top-left element */
    operator fun set(x: Int, y: Int, level: Int, value: Int)
    /** Gets the value at [x], [y] at [level], [startX] and [startY] are NOT used here so 0,0 means the top-left element */
    operator fun get(x: Int, y: Int, level: Int): Int

    /** Adds a new [value] on top of [x], [y] */
    fun push(x: Int, y: Int, value: Int) {
        set(x, y, getStackLevel(x, y), value)
    }

    /** Set the first [value] of a stack in the cell [x], [y] */
    fun setFirst(x: Int, y: Int, value: Int) {
        set(x, y, 0, value)
    }

    /** Gets the first value of the stack in the cell [x], [y] */
    fun getFirst(x: Int, y: Int): Int {
        val level = getStackLevel(x, y)
        if (level == 0) return empty
        return get(x, y, 0)
    }

    /** Gets the last value of the stack in the cell [x], [y] */
    fun getLast(x: Int, y: Int): Int {
        val level = getStackLevel(x, y)
        if (level == 0) return empty
        return get(x, y, level - 1)
    }
}

class StackedIntArray2(
    override val width: Int,
    override val height: Int,
    override val empty: Int = EMPTY,
    override val startX: Int = 0,
    override val startY: Int = 0,
) : IStackedIntArray2 {
    override var contentVersion: Int = 0 ; private set

    override fun clone(): StackedIntArray2 {
        return StackedIntArray2(width, height, empty, startX, startY).also { out ->
            arraycopy(this.level.data, 0, out.level.data, 0, out.level.data.size)
            out.data.addAll(this.data.map { it.clone() })
        }
    }

    val level = IntArray2(width, height, 0)
    val data = fastArrayListOf<IntArray2>()

    override val maxLevel: Int get() = data.size

    companion object {
        const val EMPTY = -1

        operator fun invoke(
            vararg layers: IntArray2,
            width: Int = layers.first().width,
            height: Int = layers.first().height,
            empty: Int = EMPTY,
            startX: Int = 0,
            startY: Int = 0,
        ): StackedIntArray2 {
            val stacked = StackedIntArray2(width, height, empty, startX = startX, startY = startY)
            stacked.level.fill { layers.size }
            stacked.data.addAll(layers)
            return stacked
        }
    }

    fun ensureLevel(level: Int) {
        while (level >= data.size) data.add(IntArray2(width, height, empty))
    }

    fun setLayer(level: Int, data: IntArray2) {
        ensureLevel(level)
        this.data[level] = data
        contentVersion++
    }

    override operator fun set(x: Int, y: Int, level: Int, value: Int) {
        ensureLevel(level)
        data[level][x, y] = value
        this.level[x, y] = maxOf(this.level[x, y], level + 1)
        contentVersion++
    }

    override operator fun get(x: Int, y: Int, level: Int): Int {
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

fun IntArray2.toStacked(): StackedIntArray2 = StackedIntArray2(this)

class SparseChunkedStackedIntArray2(override var empty: Int = StackedIntArray2.EMPTY) : SparseChunkedStackedArray2<IStackedIntArray2>(), IStackedIntArray2 {
    constructor(vararg layers: IStackedIntArray2, empty: Int = StackedIntArray2.EMPTY) : this(empty) {
        layers.fastForEach { putChunk(it) }
    }

    override fun setEmptyFromChunk(chunk: IStackedIntArray2) {
        empty = chunk.empty
    }

    override fun set(x: Int, y: Int, level: Int, value: Int) {
        getChunkAt(x, y)?.let { chunk ->
            chunk[chunk.chunkX(x), chunk.chunkY(y), level] = value
            contentVersion++
        }
    }

    override fun get(x: Int, y: Int, level: Int): Int {
        getChunkAt(x, y)?.let { chunk ->
            return chunk[chunk.chunkX(x), chunk.chunkY(y), level]
        }
        return empty
    }

    override fun clone(): SparseChunkedStackedIntArray2 = SparseChunkedStackedIntArray2(empty).also { sparse ->
        findAllChunks().fastForEach {
            sparse.putChunk(it.clone())
        }
    }
}
