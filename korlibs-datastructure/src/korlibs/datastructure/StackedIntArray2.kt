package korlibs.datastructure

import korlibs.memory.arraycopy

interface IStackedIntArray2 {
    /** Annotation of where in [startX] this stack would be placed in a bigger container, not used for set or get methods */
    val startX: Int
    /** Annotation of where in [startY] this stack would be placed in a bigger container, not used for set or get methods */
    val startY: Int

    /** [width] of the data available here, get and set methods use values in the range x=0 until [width] */
    val width: Int
    /** [height] of the data available here, get and set methods use values in the range y=0 until [height] */
    val height: Int

    /** The [empty] value that will be returned if the specified cell it out of bounds, or empty */
    val empty: Int
    /** The maximum level of layers available on the whole stack */
    val maxLevel: Int

    /** Duplicates the contents of this [IStackedIntArray2] keeping its contents data */
    fun clone(): IStackedIntArray2

    /** Sets the [value] at [x], [y] at [level], [startX] and [startY] are NOT used here so 0,0 means the top-left element */
    operator fun set(x: Int, y: Int, level: Int, value: Int)
    /** Gets the value at [x], [y] at [level], [startX] and [startY] are NOT used here so 0,0 means the top-left element */
    operator fun get(x: Int, y: Int, level: Int): Int

    /** Number of values available at this [x], [y] */
    fun getStackLevel(x: Int, y: Int): Int

    /** Adds a new [value] on top of [x], [y] */
    fun push(x: Int, y: Int, value: Int)

    /** Removes the last value at [x], [y] */
    fun removeLast(x: Int, y: Int)

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

    /** Checks if [x] and [y] are inside this array in the range x=0 until [width] and y=0 until [height] ignoring startX and startY */
    fun inside(x: Int, y: Int): Boolean = x >= 0 && y >= 0 && x < width && y < height
}

/** Shortcut for [IStackedIntArray2.startX] + [IStackedIntArray2.width] */
val IStackedIntArray2.endX: Int get() = startX + width
/** Shortcut for [IStackedIntArray2.startY] + [IStackedIntArray2.height] */
val IStackedIntArray2.endY: Int get() = startY + height

class StackedIntArray2(
    override val width: Int,
    override val height: Int,
    override val empty: Int = -1,
    override val startX: Int = 0,
    override val startY: Int = 0,
) : IStackedIntArray2 {

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
        operator fun invoke(
            vararg layers: IntArray2,
            width: Int = layers.first().width,
            height: Int = layers.first().height,
            empty: Int = -1,
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
    }

    override operator fun set(x: Int, y: Int, level: Int, value: Int) {
        ensureLevel(level)
        data[level][x, y] = value
    }

    override operator fun get(x: Int, y: Int, level: Int): Int {
        if (level > this.level[x, y]) return empty
        return data[level][x, y]
    }

    override fun getStackLevel(x: Int, y: Int): Int {
        return this.level[x, y]
    }

    override fun push(x: Int, y: Int, value: Int) {
        set(x, y, level[x, y]++, value)
    }

    override fun removeLast(x: Int, y: Int) {
        level[x, y] = (level[x, y] - 1).coerceAtLeast(0)
    }
}

fun IntArray2.toStacked(): StackedIntArray2 = StackedIntArray2(this)
