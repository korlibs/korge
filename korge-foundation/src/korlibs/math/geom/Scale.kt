package korlibs.math.geom

//@KormaValueApi
//inline class Scale internal constructor(internal val raw: Float2Pack) {
data class Scale(val scaleX: Double, val scaleY: Double) {
    companion object {
        val IDENTITY = Scale(1f, 1f)
    }

    //val scaleX: Float get() = raw.f0
    //val scaleY: Float get() = raw.f1
    val scaleAvg: Double get() = scaleX * .5 + scaleY * .5

    @Deprecated("", ReplaceWith("scaleAvg"))
    val avg: Double get() = scaleAvg

    constructor() : this(1f, 1f)
    constructor(scale: Float) : this(scale, scale)
    constructor(scale: Double) : this(scale, scale)
    constructor(scale: Int) : this(scale.toDouble())
    //constructor(scaleX: Float, scaleY: Float) : this(float2PackOf(scaleX, scaleY))
    constructor(scaleX: Float, scaleY: Float) : this(scaleX.toDouble(), scaleY.toDouble())
    constructor(scaleX: Int, scaleY: Int) : this(scaleX.toDouble(), scaleY.toDouble())

    operator fun unaryMinus(): Scale = Scale(-scaleX, -scaleY)
    operator fun unaryPlus(): Scale = this

    operator fun plus(other: Scale): Scale = Scale(scaleX + other.scaleX, scaleY + other.scaleY)
    operator fun minus(other: Scale): Scale = Scale(scaleX - other.scaleX, scaleY - other.scaleY)

    operator fun times(other: Scale): Scale = Scale(scaleX * other.scaleX, scaleY * other.scaleY)
    operator fun times(other: Float): Scale = Scale(scaleX * other, scaleY * other)
    operator fun div(other: Scale): Scale = Scale(scaleX / other.scaleX, scaleY / other.scaleY)
    operator fun div(other: Float): Scale = Scale(scaleX / other, scaleY / other)
    operator fun rem(other: Scale): Scale = Scale(scaleX % other.scaleX, scaleY % other.scaleY)
    operator fun rem(other: Float): Scale = Scale(scaleX % other, scaleY % scaleY)
}

operator fun Vector2D.times(other: Scale): Vector2D = Vector2D(x * other.scaleX, y * other.scaleY)
operator fun Vector2D.div(other: Scale): Vector2D = Vector2D(x / other.scaleX, y / other.scaleY)
operator fun Vector2D.rem(other: Scale): Vector2D = Vector2D(x % other.scaleX, y % other.scaleY)

operator fun Vector2F.times(other: Scale): Vector2F = Vector2F(x * other.scaleX, y * other.scaleY)
operator fun Vector2F.div(other: Scale): Vector2F = Vector2F(x / other.scaleX, y / other.scaleY)
operator fun Vector2F.rem(other: Scale): Vector2F = Vector2F(x % other.scaleX, y % other.scaleY)

fun Vector2F.toScale(): Scale = Scale(x, y)
fun Vector2D.toScale(): Scale = Scale(x, y)

fun Scale.toPoint(): Point = Point(scaleX, scaleY)
fun Scale.toVector2(): Vector2D = Vector2D(scaleX, scaleY)
fun Scale.toVector2F(): Vector2F = Vector2F(scaleX, scaleY)
