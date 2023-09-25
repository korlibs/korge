package korlibs.math.geom

import korlibs.number.*
import kotlin.math.*

typealias Size = Size2D
typealias Size3 = Size2F

data class Size2F(val width: Float, val height: Float)
data class Size3F(val width: Float, val height: Float, val depth: Float)
data class Size3D(val width: Double, val height: Double, val depth: Double)

/**
 * A class representing a size with a [width] and a [height] as Float.
 */
data class Size2D(val width: Double, val height: Double) {//: Sizeable {
    companion object {
        inline operator fun invoke(width: Number, height: Number): Size2D = Size2D(width.toDouble(), height.toDouble())
        val ZERO = Size(0.0, 0.0)
        fun square(value: Int): Size = Size(value, value)
        fun square(value: Double): Size = Size(value, value)
    }

    fun isEmpty(): Boolean = width == 0.0 || height == 0.0

    fun avgComponent(): Double = width * 0.5 + height * 0.5
    fun minComponent(): Double = min(width, height)
    fun maxComponent(): Double = max(width, height)

    val area: Double get() = width * height
    val perimeter: Double get() = width * 2 + height * 2

    //(val width: Double, val height: Double) {
    constructor() : this(0.0, 0.0)
    constructor(width: Float, height: Float) : this(width.toDouble(), height.toDouble())
    constructor(width: Int, height: Int) : this(width.toDouble(), height.toDouble())

    operator fun unaryMinus(): Size = Size(-width, -height)
    operator fun unaryPlus(): Size = this

    operator fun minus(other: Size): Size = Size(width - other.width, height - other.height)
    operator fun plus(other: Size): Size = Size(width + other.width, height + other.height)
    operator fun times(scale: Scale): Size = Size(width * scale.scaleX, height * scale.scaleY)
    operator fun times(scale: Vector2F): Size = Size(width * scale.x, height * scale.y)
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

operator fun Vector2D.plus(other: Size): Vector2D = Vector2D(x + other.width, y + other.height)
operator fun Vector2D.minus(other: Size): Vector2D = Vector2D(x - other.width, y - other.height)
operator fun Vector2D.times(other: Size): Vector2D = Vector2D(x * other.width, y * other.height)
operator fun Vector2D.div(other: Size): Vector2D = Vector2D(x / other.width, y / other.height)
operator fun Vector2D.rem(other: Size): Vector2D = Vector2D(x % other.width, y % other.height)

operator fun Vector2F.plus(other: Size): Vector2F = Vector2F(x + other.width, y + other.height)
operator fun Vector2F.minus(other: Size): Vector2F = Vector2F(x - other.width, y - other.height)
operator fun Vector2F.times(other: Size): Vector2F = Vector2F(x * other.width, y * other.height)
operator fun Vector2F.div(other: Size): Vector2F = Vector2F(x / other.width, y / other.height)
operator fun Vector2F.rem(other: Size): Vector2F = Vector2F(x % other.width, y % other.height)

fun Point.toSize(): Size = Size(x, y)

fun Size.toInt(): SizeInt = SizeInt(width.toInt(), height.toInt())
fun Size.toPoint(): Point = Point(width, height)
fun Size.toVector(): Vector2D = Vector2D(width, height)
fun Size.toVector2D(): Vector2D = Vector2D(width, height)
fun Size.toVector2F(): Vector2F = Vector2F(width, height)

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

typealias SizeI = SizeInt

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
    operator fun times(scale: Vector2F): SizeInt = SizeInt((width * scale.x).toInt(), (height * scale.y).toInt())
    operator fun times(scale: Scale): SizeInt = SizeInt((width * scale.scaleX).toInt(), (height * scale.scaleY).toInt())

    operator fun div(other: SizeInt): SizeInt = SizeInt(width / other.width, height / other.height)
    operator fun div(s: Float): SizeInt = SizeInt((width / s).toInt(), (height / s).toInt())
    operator fun div(s: Double): SizeInt = div(s.toFloat())
    operator fun div(s: Int): SizeInt = div(s.toFloat())

    override fun toString(): String = "${width}x${height}"
}

fun Vector2I.toSize(): SizeInt = SizeInt(x, y)
fun SizeInt.toFloat(): Size = Size(width.toFloat(), height.toFloat())
fun SizeInt.toDouble(): Size = Size(width.toDouble(), height.toDouble())
fun SizeInt.toVector(): Vector2I = Vector2I(width, height)

operator fun Vector2D.plus(other: SizeInt): Vector2D = Vector2D(x + other.width, y + other.height)
operator fun Vector2D.minus(other: SizeInt): Vector2D = Vector2D(x - other.width, y - other.height)
operator fun Vector2D.times(other: SizeInt): Vector2D = Vector2D(x * other.width, y * other.height)
operator fun Vector2D.div(other: SizeInt): Vector2D = Vector2D(x / other.width, y / other.height)
operator fun Vector2D.rem(other: SizeInt): Vector2D = Vector2D(x % other.width, y % other.height)

operator fun Vector2F.plus(other: SizeInt): Vector2F = Vector2F(x + other.width, y + other.height)
operator fun Vector2F.minus(other: SizeInt): Vector2F = Vector2F(x - other.width, y - other.height)
operator fun Vector2F.times(other: SizeInt): Vector2F = Vector2F(x * other.width, y * other.height)
operator fun Vector2F.div(other: SizeInt): Vector2F = Vector2F(x / other.width, y / other.height)
operator fun Vector2F.rem(other: SizeInt): Vector2F = Vector2F(x % other.width, y % other.height)

operator fun Vector2I.plus(other: SizeInt): Vector2I = Vector2I(x + other.width, y + other.height)
operator fun Vector2I.minus(other: SizeInt): Vector2I = Vector2I(x - other.width, y - other.height)
operator fun Vector2I.times(other: SizeInt): Vector2I = Vector2I(x * other.width, y * other.height)
operator fun Vector2I.div(other: SizeInt): Vector2I = Vector2I(x / other.width, y / other.height)
operator fun Vector2I.rem(other: SizeInt): Vector2I = Vector2I(x % other.width, y % other.height)
