package korlibs.math.geom

import korlibs.math.annotations.*
import korlibs.memory.pack.*

//@KormaValueApi
//inline class Scale internal constructor(internal val raw: Float2Pack) {
data class Scale(val scaleX: Float, val scaleY: Float) {
    companion object {
        val IDENTITY = Scale(1f, 1f)
    }

    //val scaleX: Float get() = raw.f0
    //val scaleY: Float get() = raw.f1
    val scaleAvg: Float get() = scaleX * .5f + scaleY * .5f

    @Deprecated("", ReplaceWith("scaleAvg"))
    val avg: Float get() = scaleAvg
    @Deprecated("", ReplaceWith("scaleAvgD"))
    val avgD: Double get() = scaleAvgD

    val scaleXD: Double get() = scaleX.toDouble()
    val scaleYD: Double get() = scaleY.toDouble()
    val scaleAvgD: Double get() = scaleAvg.toDouble()

    constructor() : this(1f, 1f)
    constructor(scale: Float) : this(scale, scale)
    constructor(scale: Double) : this(scale.toFloat())
    constructor(scale: Int) : this(scale.toFloat())
    //constructor(scaleX: Float, scaleY: Float) : this(float2PackOf(scaleX, scaleY))
    constructor(scaleX: Double, scaleY: Double) : this(scaleX.toFloat(), scaleY.toFloat())
    constructor(scaleX: Int, scaleY: Int) : this(scaleX.toFloat(), scaleY.toFloat())

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

operator fun Vector2.times(other: Scale): Vector2 = Vector2(x * other.scaleX, y * other.scaleY)
operator fun Vector2.div(other: Scale): Vector2 = Vector2(x / other.scaleX, y / other.scaleY)
operator fun Vector2.rem(other: Scale): Vector2 = Vector2(x % other.scaleX, y % other.scaleY)

fun Vector2.toScale(): Scale = Scale(x, y)

fun Scale.toPoint(): Point = Point(scaleX, scaleY)
fun Scale.toVector2(): Vector2 = Vector2(scaleX, scaleY)

/////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////

@KormaMutableApi
sealed interface IScale {
    val scaleX: Double
    val scaleY: Double
}

@KormaMutableApi
data class MScale(
    override var scaleX: Double,
    override var scaleY: Double,
) : IScale {
    constructor() : this(1.0, 1.0)
}

fun Scale.toMutable(out: MScale = MScale()): MScale {
    out.scaleX = scaleXD
    out.scaleY = scaleYD
    return out
}
fun MScale.toImmutable(): Scale = Scale(scaleX, scaleY)
