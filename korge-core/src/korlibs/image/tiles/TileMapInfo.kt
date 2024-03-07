package korlibs.image.tiles

import korlibs.datastructure.*

class TileMapInfo(val data: IStackedLongArray2) : BaseDelegatedStackedArray2(data), IStackedArray2<Tile> {
    constructor(width: Int, height: Int) : this(StackedLongArray2(width, height, StackedLongArray2.EMPTY))

    /** The [empty] value that will be returned if the specified cell it out of bounds, or empty */
    val empty: Tile get() = Tile(data.empty)

    operator fun set(x: Int, y: Int, data: Tile) = setLast(x, y, data)

    operator fun set(x: Int, y: Int, level: Int, data: Tile) {
        if (inside(x, y)) {
            this.data[x, y, level] = data.raw
        }
    }

    operator fun get(x: Int, y: Int): Tile = getLast(x, y)

    operator fun get(x: Int, y: Int, level: Int): Tile = Tile.fromRaw(this.data[x, y, level])

    /** Number of values available at this [x], [y] */
    override fun getStackLevel(x: Int, y: Int): Int = this.data.getStackLevel(x, y)

    /** Adds a new [value] on top of [x], [y] */
    fun push(x: Int, y: Int, value: Tile) {
        this.data.push(x, y, value.raw)
    }

    /** Set the first [value] of a stack in the cell [x], [y] */
    fun setFirst(x: Int, y: Int, value: Tile) {
        set(x, y, 0, value)
    }

    /** Gets the first value of the stack in the cell [x], [y] */
    fun getFirst(x: Int, y: Int): Tile {
        val level = getStackLevel(x, y)
        if (level == 0) return empty
        return get(x, y, 0)
    }

    /** Gets the last value of the stack in the cell [x], [y] */
    fun getLast(x: Int, y: Int): Tile {
        val level = getStackLevel(x, y)
        if (level == 0) return empty
        return get(x, y, level - 1)
    }

    fun setLast(x: Int, y: Int, value: Tile) {
        if (!inside(x, y)) return
        val level = (getStackLevel(x, y) - 1).coerceAtLeast(0)
        set(x, y, level, value)
    }
}
