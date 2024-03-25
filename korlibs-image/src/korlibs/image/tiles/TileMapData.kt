package korlibs.image.tiles

import korlibs.datastructure.*
import korlibs.math.*
import korlibs.memory.*

data class TileMapData(
    val data: IStackedLongArray2,
    val tileSet: TileSet = TileSet.EMPTY,
    val repeatX: TileMapRepeat = TileMapRepeat.NONE,
    val repeatY: TileMapRepeat = TileMapRepeat.NONE,
    val offsetScale: Float = 1f,
) : BaseDelegatedStackedArray2(data), IStackedArray2<Tile> {

    companion object {
        operator fun invoke(
            data: IntArray2,
            tileSet: TileSet? = null,
            maskData: Int = 0x0fffffff,
            maskFlipX: Int = 1.mask(31),
            maskFlipY: Int = 1.mask(30),
            maskRotate: Int = 1.mask(29),
            maskOffsetX: Int = 0,
            maskOffsetY: Int = 0,
            //maskOffsetX: Int = 5.mask(18),
            //maskOffsetY: Int = 5.mask(23),
            offsetSigned: Boolean = true,
            repeatX: TileMapRepeat = TileMapRepeat.NONE,
            repeatY: TileMapRepeat = TileMapRepeat.NONE,
            offsetScale: Float = 1f,
        ): TileMapData {
            val map = TileMapData(data.width, data.height, tileSet = tileSet ?: TileSet.EMPTY, offsetScale = offsetScale, repeatX = repeatX, repeatY = repeatY)
            val offsetXRange = IntMaskRange.fromMask(maskOffsetX)
            val offsetYRange = IntMaskRange.fromMask(maskOffsetY)
            data.each { x, y, v ->
                val offsetX = offsetXRange.extractSigned(v, offsetSigned)
                val offsetY = offsetYRange.extractSigned(v, offsetSigned)
                map[x, y] = Tile(v and maskData, offsetX, offsetY, (v and maskFlipX) != 0, (v and maskFlipY) != 0, (v and maskRotate) != 0)
            }
            return map
        }

    }

    constructor(
        width: Int, height: Int,
        tileSet: TileSet = TileSet.EMPTY,
        empty: Tile = Tile(0),
        repeatX: TileMapRepeat = TileMapRepeat.NONE,
        repeatY: TileMapRepeat = TileMapRepeat.NONE,
        offsetScale: Float = 1f,
    ) : this(StackedLongArray2(width, height, empty.raw), tileSet, repeatX, repeatY, offsetScale)

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

fun TileMapData.toStringListSimplified(func: (Tile) -> Char): List<String> {
    val lines = arrayListOf<StringBuilder>()
    eachPosition { x, y ->
        while (lines.size <= y) lines.add(StringBuilder())
        val line = lines[y]
        while (line.length <= x) line.append(' ')
        line[x] = func(this[x, y])
    }
    return lines.map { it.toString() }
}

enum class TileMapRepeat(val get: (v: Int, max: Int) -> Int) {
    NONE({ v, max -> v }),
    REPEAT({ v, max -> v umod max }),
    MIRROR({ v, max ->
        val r = v umod max
        if ((v / max) % 2 == 0) r else max - 1 - r
    })
}
