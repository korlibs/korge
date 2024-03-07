package korlibs.image.tiles

import korlibs.memory.*

inline class Tile(val raw: Long) {
    val rawLow: Int get() = raw.low
    val rawHigh: Int get() = raw.high

    @Deprecated("Use raw, rawLow or rawHigh")
    val data: Int get() = rawLow

    val isValid: Boolean get() = raw != INVALID_VALUE
    val isInvalid: Boolean get() = raw == INVALID_VALUE

    val tile: Int get() = rawLow.extract(0, 26)
    //val tile: Int get() = rawLow.extract(0, 18)

    //val extra: Int get() = rawLow.extract10(18)
    //val offsetX: Int get() = rawLow.extract5(18)
    //val offsetY: Int get() = rawLow.extract5(23)

    val rotate: Boolean get() = rawLow.extract(29)
    val flipY: Boolean get() = rawLow.extract(30)
    val flipX: Boolean get() = rawLow.extract(31)
    val offsetX: Int get() = rawHigh.extract16Signed(0)
    val offsetY: Int get() = rawHigh.extract16Signed(16)

    fun toStringInfo(): String = "Tile(tile=$tile, offsetX=$offsetX, offsetY=$offsetY, flipX=$flipX, flipY=$flipY, rotate=$rotate)"

    companion object {
        const val INVALID_VALUE = -1L
        val ZERO = Tile(0L)
        val INVALID = Tile(INVALID_VALUE)

        fun fromRaw(raw: Long): Tile = Tile(raw)
        fun fromRaw(low: Int, high: Int): Tile = Tile(Long.fromLowHigh(low, high))

        operator fun invoke(tile: Int, offsetX: Int = 0, offsetY: Int = 0, flipX: Boolean = false, flipY: Boolean = false, rotate: Boolean = false): Tile = fromRaw(
            0.insert(tile, 0, 26).insert(rotate, 29).insert(flipY, 30).insert(flipX, 31),
            0.insert16(offsetX, 0).insert16(offsetY, 16)
        )
    }
}
