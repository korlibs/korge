package korlibs.math.geom

import korlibs.math.internal.*
import korlibs.memory.pack.*
import kotlin.math.*

/**
 * A class representing a size with a [width] and a [height] as Float.
 */
//@KormaExperimental
//@KormaValueApi
inline class Size internal constructor(internal val raw: Float2Pack) {//: Sizeable {
    companion object {
        val ZERO = Size(0f, 0f)
        fun square(value: Int): Size = Size(value, value)
        fun square(value: Float): Size = Size(value, value)
        fun square(value: Double): Size = Size(value, value)
    }

    fun isEmpty(): Boolean = width == 0f || height == 0f

    operator fun component1(): Float = width
    operator fun component2(): Float = height

    fun copy(width: Float = this.width, height: Float = this.height): Size = Size(width, height)

    fun avgComponent(): Float = width * 0.5f + height * 0.5f
    fun minComponent(): Float = min(width, height)
    fun maxComponent(): Float = max(width, height)

    val width: Float get() = raw.f0
    val height: Float get() = raw.f1

    val widthF: Float get() = width
    val heightF: Float get() = height
    val widthD: Double get() = width.toDouble()
    val heightD: Double get() = height.toDouble()

    val area: Float get() = width * height
    val perimeter: Float get() = width * 2 + height * 2

    //(val width: Double, val height: Double) {
    constructor() : this(0f, 0f)
    constructor(width: Float, height: Float) : this(float2PackOf(width, height))
    constructor(width: Double, height: Double) : this(float2PackOf(width.toFloat(), height.toFloat()))
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

val Size.mutable: MSize get() = MSize(width, height)

val MSize.immutable: Size get() = Size(width, height)

fun MSize.asInt(): MSizeInt = MSizeInt(this)
fun MSizeInt.asDouble(): MSize = this.float

fun MPoint.asSize(): MSize = MSize(this)

fun Point.toSize(): Size = Size(raw)
fun Vector2Int.toSize(): SizeInt = SizeInt(raw)

fun Size.toInt(): SizeInt = SizeInt(width.toInt(), height.toInt())
fun SizeInt.toFloat(): Size = Size(width.toFloat(), height.toFloat())
fun SizeInt.toVector(): Vector2Int = Vector2Int(width, height)
fun Size.toPoint(): Point = Point(width, height)
fun Size.toVector(): Vector2 = Vector2(width, height)
