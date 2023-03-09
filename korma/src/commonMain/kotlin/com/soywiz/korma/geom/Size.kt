package com.soywiz.korma.geom

import com.soywiz.kds.pack.*
import com.soywiz.korma.annotations.*
import com.soywiz.korma.internal.*
import com.soywiz.korma.interpolation.*
import kotlin.math.*

@KormaValueApi
interface Sizeable {
    val size: Size
}

//@KormaExperimental
//@KormaValueApi
inline class Size internal constructor(internal val raw: Float2Pack) {
    val width: Float get() = raw.x
    val height: Float get() = raw.y

    val widthF: Float get() = width
    val heightF: Float get() = height
    val widthD: Double get() = width.toDouble()
    val heightD: Double get() = height.toDouble()

    val area: Float get() = width * height
    val perimeter: Float get() = width * 2 + height * 2

    //(val width: Double, val height: Double) {
    constructor() : this(0f, 0f)
    constructor(width: Float, height: Float) : this(Float2Pack(width, height))
    constructor(width: Double, height: Double) : this(Float2Pack(width.toFloat(), height.toFloat()))
    constructor(width: Int, height: Int) : this(width.toFloat(), height.toFloat())

    operator fun component1(): Float = width
    operator fun component2(): Float = height

    fun avgComponent(): Float = width * 0.5f + height * 0.5f
    fun minComponent(): Float = min(width, height)
    fun maxComponent(): Float = max(width, height)

    operator fun unaryMinus(): Size = Size(-width, -height)
    operator fun unaryPlus(): Size = this

    operator fun minus(other: Size): Size = Size(width - other.width, height - other.height)
    operator fun plus(other: Size): Size = Size(width + other.width, height + other.height)
    operator fun times(s: Float): Size = Size(width * s, height * s)
    operator fun times(s: Double): Size = times(s.toFloat())
    operator fun times(s: Int): Size = times(s.toFloat())
    operator fun div(other: Size): Size = Size(width / other.width, height / other.height)
    operator fun div(s: Float): Size = Size(width / s, height / s)
    operator fun div(s: Double): Size = div(s.toFloat())
    operator fun div(s: Int): Size = div(s.toFloat())

    override fun toString(): String = "Size(width=${width.niceStr}, height=${height.niceStr})"
}

val Size.mutable: MSize get() = MSize(width, height)

inline class SizeInt internal constructor(internal val raw: Int2Pack) {
    val width: Int get() = raw.x
    val height: Int get() = raw.y

    val area: Int get() = width * height
    val perimeter: Int get() = width * 2 + height * 2

    constructor() : this(0, 0)
    constructor(width: Int, height: Int) : this(Int2Pack(width, height))

    operator fun component1(): Int = width
    operator fun component2(): Int = height
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

val MSize.immutable: Size get() = Size(width, height)

interface MSizeable {
    val mSize: MSize
}

@KormaMutableApi
@Deprecated("Use Size instead")
inline class MSize(val p: MPoint) : MutableInterpolable<MSize>, Interpolable<MSize>, Sizeable, MSizeable {
    companion object {
        operator fun invoke(): MSize = MSize(MPoint(0, 0))
        operator fun invoke(width: Double, height: Double): MSize = MSize(MPoint(width, height))
        operator fun invoke(width: Int, height: Int): MSize = MSize(MPoint(width, height))
        operator fun invoke(width: Float, height: Float): MSize = MSize(MPoint(width, height))
    }

    fun copy() = MSize(p.copy())

    override val size: Size get() = immutable
    override val mSize: MSize get() = this


    val area: Double get() = width * height
    val perimeter: Double get() = width * 2 + height * 2
    val min: Double get() = min(width, height)
    val max: Double get() = max(width, height)

    var width: Double
        set(value) { p.x = value }
        get() = p.x
    var height: Double
        set(value) { p.y = value }
        get() = p.y

    fun setTo(width: Double, height: Double): MSize {
        this.width = width
        this.height = height
        return this
    }
    fun setTo(width: Int, height: Int) = setTo(width.toDouble(), height.toDouble())
    fun setTo(width: Float, height: Float) = setTo(width.toDouble(), height.toDouble())
    fun setTo(that: MSize) = setTo(that.width, that.height)
    fun setTo(that: Size) = setTo(that.width, that.height)

    fun setToScaled(sx: Double, sy: Double) = setTo((this.width * sx), (this.height * sy))
    fun setToScaled(sx: Float, sy: Float) = setToScaled(sx.toDouble(), sy.toDouble())
    fun setToScaled(sx: Int, sy: Int) = setToScaled(sx.toDouble(), sy.toDouble())

    fun clone() = MSize(width, height)

    override fun interpolateWith(ratio: Ratio, other: MSize): MSize = MSize(0, 0).setToInterpolated(ratio, this, other)

    override fun setToInterpolated(ratio: Ratio, l: MSize, r: MSize): MSize = this.setTo(
        ratio.interpolate(l.width, r.width),
        ratio.interpolate(l.height, r.height)
    )

    override fun toString(): String = "Size(width=${width.niceStr}, height=${height.niceStr})"
}

@KormaMutableApi
@Deprecated("Use SizeInt instead")
inline class MSizeInt(val float: MSize) {
    companion object {
        operator fun invoke(): MSizeInt = MSizeInt(MSize(0, 0))
        operator fun invoke(width: Int, height: Int): MSizeInt = MSizeInt(MSize(width, height))
        operator fun invoke(that: SizeInt): MSizeInt = MSizeInt(MSize(that.width, that.height))
    }

    val immutable: SizeInt get() = SizeInt(width, height)
    fun clone(): MSizeInt = MSizeInt(float.clone())

    fun setTo(width: Int, height: Int) : MSizeInt {
        this.width = width
        this.height = height

        return this
    }

    fun setTo(that: SizeInt) = setTo(that.width, that.height)
    fun setTo(that: MSizeInt) = setTo(that.width, that.height)

    fun setToScaled(sx: Double, sy: Double) = setTo((this.width * sx).toInt(), (this.height * sy).toInt())
    fun setToScaled(sx: Int, sy: Int) = setToScaled(sx.toDouble(), sy.toDouble())
    fun setToScaled(sx: Float, sy: Float) = setToScaled(sx.toDouble(), sy.toDouble())

    fun anchoredIn(container: MRectangleInt, anchor: Anchor, out: MRectangleInt = MRectangleInt()): MRectangleInt {
        return out.setTo(
            ((container.width - this.width) * anchor.doubleX).toInt(),
            ((container.height - this.height) * anchor.doubleY).toInt(),
            width,
            height
        )
    }

    operator fun contains(v: MSizeInt): Boolean = (v.width <= width) && (v.height <= height)
    operator fun times(v: Double) = MSizeInt(MSize((width * v).toInt(), (height * v).toInt()))
    operator fun times(v: Int) = this * v.toDouble()
    operator fun times(v: Float) = this * v.toDouble()

    fun getAnchorPosition(anchor: Anchor, out: MPointInt = MPointInt(0, 0)): MPointInt =
        out.setTo((width * anchor.doubleX).toInt(), (height * anchor.doubleY).toInt())

    var width: Int
        set(value) { float.width = value.toDouble() }
        get() = float.width.toInt()
    var height: Int
        set(value) { float.height = value.toDouble() }
        get() = float.height.toInt()

    //override fun toString(): String = "SizeInt($width, $height)"
    override fun toString(): String = "SizeInt(width=$width, height=$height)"
}

fun MSize.asInt(): MSizeInt = MSizeInt(this)
fun MSizeInt.asDouble(): MSize = this.float

fun MPoint.asSize(): MSize = MSize(this)

fun Point.toSize(): Size = Size(raw)
fun PointInt.toSize(): SizeInt = SizeInt(raw)

fun Size.toInt(): SizeInt = SizeInt(width.toInt(), height.toInt())
fun SizeInt.toFloat(): Size = Size(width.toFloat(), height.toFloat())
fun Size.toPoint(): Point = Point(width, height)
