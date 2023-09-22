package korlibs.math.geom

import korlibs.number.*
import kotlin.math.*

/**
 * A class representing a size with a [width] and a [height] as Float.
 */
//@KormaExperimental
//@KormaValueApi
//inline class Size internal constructor(internal val raw: Float2Pack) {//: Sizeable {
data class Size(val width: Float, val height: Float) {//: Sizeable {
    //val width: Float get() = raw.f0
    //val height: Float get() = raw.f1
    //operator fun component1(): Float = width
    //operator fun component2(): Float = height
    //fun copy(width: Float = this.width, height: Float = this.height): Size = Size(width, height)
    //constructor(width: Float, height: Float) : this(float2PackOf(width, height))

    companion object {
        val ZERO = Size(0f, 0f)
        fun square(value: Int): Size = Size(value, value)
        fun square(value: Double): Size = Size(value, value)
    }

    fun isEmpty(): Boolean = width == 0f || height == 0f

    fun avgComponent(): Float = width * 0.5f + height * 0.5f
    fun minComponent(): Float = min(width, height)
    fun maxComponent(): Float = max(width, height)

    val widthF: Float get() = width
    val heightF: Float get() = height
    val widthD: Double get() = width.toDouble()
    val heightD: Double get() = height.toDouble()

    val area: Float get() = width * height
    val perimeter: Float get() = width * 2 + height * 2

    //(val width: Double, val height: Double) {
    constructor() : this(0f, 0f)
    constructor(width: Double, height: Double) : this(width.toFloat(), height.toFloat())
    constructor(width: Int, height: Int) : this(width.toFloat(), height.toFloat())

    operator fun unaryMinus(): Size = Size(-width, -height)
    operator fun unaryPlus(): Size = this

    operator fun minus(other: Size): Size = Size(width - other.width, height - other.height)
    operator fun plus(other: Size): Size = Size(width + other.width, height + other.height)
    operator fun times(scale: Scale): Size = Size(width * scale.scaleX, height * scale.scaleY)
    operator fun times(scale: Vector2): Size = Size(width * scale.x, height * scale.y)
    operator fun times(s: Float): Size = Size(width * s, height * s)
    operator fun times(s: Double): Size = times(s.toFloat())
    operator fun times(s: Int): Size = times(s.toFloat())
    operator fun div(other: Size): Scale = Scale(width / other.width, height / other.height)
    operator fun div(s: Float): Size = Size(width / s, height / s)
    operator fun div(s: Double): Size = div(s.toFloat())
    operator fun div(s: Int): Size = div(s.toFloat())

    //override val size: Size get() = this

    override fun toString(): String = "Size(width=${width.niceStr}, height=${height.niceStr})"
}

operator fun Vector2.plus(other: Size): Vector2 = Vector2(x + other.width, y + other.height)
operator fun Vector2.minus(other: Size): Vector2 = Vector2(x - other.width, y - other.height)
operator fun Vector2.times(other: Size): Vector2 = Vector2(x * other.width, y * other.height)
operator fun Vector2.div(other: Size): Vector2 = Vector2(x / other.width, y / other.height)
operator fun Vector2.rem(other: Size): Vector2 = Vector2(x % other.width, y % other.height)


val Size.mutable: MSize get() = MSize(width, height)

val MSize.immutable: Size get() = Size(width, height)

fun MSize.asInt(): MSizeInt = MSizeInt(this)
fun MSizeInt.asDouble(): MSize = this.float

fun MPoint.asSize(): MSize = MSize(this)

fun Point.toSize(): Size = Size(x, y)
fun Vector2Int.toSize(): SizeInt = SizeInt(x, y)

fun Size.toInt(): SizeInt = SizeInt(width.toInt(), height.toInt())
fun SizeInt.toFloat(): Size = Size(width.toFloat(), height.toFloat())
fun SizeInt.toVector(): Vector2Int = Vector2Int(width, height)
fun Size.toPoint(): Point = Point(width, height)
fun Size.toVector(): Vector2 = Vector2(width, height)

interface Sizeable {
    val size: Size

    companion object {
        operator fun invoke(size: Size): Sizeable = object : Sizeable {
            override val size: Size get() = size
        }
    }
}

interface SizeableInt {
    val size: SizeInt
    companion object {
        operator fun invoke(size: SizeInt): SizeableInt = object : SizeableInt {
            override val size: SizeInt get() = size
        }
        operator fun invoke(width: Int, height: Int): SizeableInt = invoke(SizeInt(width, height))
    }
}

data class SizeInt(val width: Int, val height: Int) {
    constructor() : this(0, 0)

    fun avgComponent(): Int = (width + height) / 2
    fun minComponent(): Int = kotlin.math.min(width, height)
    fun maxComponent(): Int = kotlin.math.max(width, height)

    val area: Int get() = width * height
    val perimeter: Int get() = width * 2 + height * 2

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
