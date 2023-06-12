package korlibs.math.geom

import korlibs.memory.pack.*

inline class SizeInt internal constructor(internal val raw: Int2Pack) {
    operator fun component1(): Int = width
    operator fun component2(): Int = height

    fun avgComponent(): Int = (width + height) / 2
    fun minComponent(): Int = kotlin.math.min(width, height)
    fun maxComponent(): Int = kotlin.math.max(width, height)

    val width: Int get() = raw.i0
    val height: Int get() = raw.i1

    val area: Int get() = width * height
    val perimeter: Int get() = width * 2 + height * 2

    constructor() : this(0, 0)
    constructor(width: Int, height: Int) : this(int2PackOf(width, height))


    operator fun unaryMinus(): SizeInt = SizeInt(-width, -height)
    operator fun unaryPlus(): SizeInt = this

    operator fun minus(other: SizeInt): SizeInt = SizeInt(width - other.width, height - other.height)
    operator fun plus(other: SizeInt): SizeInt = SizeInt(width + other.width, height + other.height)
    operator fun times(s: Float): SizeInt = SizeInt((width * s).toInt(), (height * s).toInt())
    operator fun times(s: Double): SizeInt = times(s.toFloat())
    operator fun times(s: Int): SizeInt = times(s.toFloat())
    operator fun times(scale: Vector2): SizeInt = SizeInt((width * scale.x).toInt(), (height * scale.y).toInt())
    operator fun times(scale: Scale): SizeInt = SizeInt((width * scale.scaleX).toInt(), (height * scale.scaleY).toInt())

    operator fun div(other: SizeInt): SizeInt = SizeInt(width / other.width, height / other.height)
    operator fun div(s: Float): SizeInt = SizeInt((width / s).toInt(), (height / s).toInt())
    operator fun div(s: Double): SizeInt = div(s.toFloat())
    operator fun div(s: Int): SizeInt = div(s.toFloat())
}

operator fun Vector2.plus(other: SizeInt): Vector2 = Vector2(x + other.width, y + other.height)
operator fun Vector2.minus(other: SizeInt): Vector2 = Vector2(x - other.width, y - other.height)
operator fun Vector2.times(other: SizeInt): Vector2 = Vector2(x * other.width, y * other.height)
operator fun Vector2.div(other: SizeInt): Vector2 = Vector2(x / other.width, y / other.height)
operator fun Vector2.rem(other: SizeInt): Vector2 = Vector2(x % other.width, y % other.height)

operator fun Vector2Int.plus(other: SizeInt): Vector2Int = Vector2Int(x + other.width, y + other.height)
operator fun Vector2Int.minus(other: SizeInt): Vector2Int = Vector2Int(x - other.width, y - other.height)
operator fun Vector2Int.times(other: SizeInt): Vector2Int = Vector2Int(x * other.width, y * other.height)
operator fun Vector2Int.div(other: SizeInt): Vector2Int = Vector2Int(x / other.width, y / other.height)
operator fun Vector2Int.rem(other: SizeInt): Vector2Int = Vector2Int(x % other.width, y % other.height)
