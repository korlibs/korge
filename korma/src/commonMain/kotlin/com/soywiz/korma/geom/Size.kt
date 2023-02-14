package com.soywiz.korma.geom

import com.soywiz.korma.annotations.*
import com.soywiz.korma.internal.niceStr
import com.soywiz.korma.interpolation.Interpolable
import com.soywiz.korma.interpolation.MutableInterpolable
import com.soywiz.korma.interpolation.interpolate
import kotlin.math.max
import kotlin.math.min

@KormaExperimental
data class Size(val width: Double, val height: Double) {
    constructor() : this(0.0, 0.0)
    constructor(width: Float, height: Float) : this(width.toDouble(), height.toDouble())
    constructor(width: Int, height: Int) : this(width.toDouble(), height.toDouble())

    override fun toString(): String = "Size(width=${width.niceStr}, height=${height.niceStr})"
}

//@Deprecated("Use Size")
interface ISize {
    val width: Double
    val height: Double

    val area: Double get() = width * height
    val perimeter: Double get() = width * 2 + height * 2
    val min: Double get() = min(width, height)
    val max: Double get() = max(width, height)

    companion object {
        operator fun invoke(width: Double, height: Double): ISize = MSize(MPoint(width, height))
        operator fun invoke(width: Int, height: Int): ISize = MSize(MPoint(width, height))
        operator fun invoke(width: Float, height: Float): ISize = MSize(MPoint(width, height))
    }
}

fun MPoint.asSize(): MSize = MSize(this)
fun IPoint.asSize(): ISize = MSize(MPoint(this))

//@Deprecated("Use Size")
inline class MSize(val p: MPoint) : MutableInterpolable<MSize>, Interpolable<MSize>, ISize, Sizeable {
    companion object {
        operator fun invoke(): MSize = MSize(MPoint(0, 0))
        operator fun invoke(width: Double, height: Double): MSize = MSize(MPoint(width, height))
        operator fun invoke(width: Int, height: Int): MSize = MSize(MPoint(width, height))
        operator fun invoke(width: Float, height: Float): MSize = MSize(MPoint(width, height))
    }

    fun copy() = MSize(p.copy())

    override val size: MSize get() = this

    override var width: Double
        set(value) { p.x = value }
        get() = p.x
    override var height: Double
        set(value) { p.y = value }
        get() = p.y

    fun setTo(width: Double, height: Double): MSize {
        this.width = width
        this.height = height
        return this
    }
    fun setTo(width: Int, height: Int) = setTo(width.toDouble(), height.toDouble())
    fun setTo(width: Float, height: Float) = setTo(width.toDouble(), height.toDouble())
    fun setTo(that: ISize) = setTo(that.width, that.height)

    fun setToScaled(sx: Double, sy: Double) = setTo((this.width * sx), (this.height * sy))
    fun setToScaled(sx: Float, sy: Float) = setToScaled(sx.toDouble(), sy.toDouble())
    fun setToScaled(sx: Int, sy: Int) = setToScaled(sx.toDouble(), sy.toDouble())

    fun clone() = MSize(width, height)

    override fun interpolateWith(ratio: Double, other: MSize): MSize = MSize(0, 0).setToInterpolated(ratio, this, other)

    override fun setToInterpolated(ratio: Double, l: MSize, r: MSize): MSize = this.setTo(
        ratio.interpolate(l.width, r.width),
        ratio.interpolate(l.height, r.height)
    )

    override fun toString(): String = "Size(width=${width.niceStr}, height=${height.niceStr})"
}

interface ISizeInt {
    val width: Int
    val height: Int

    companion object {
        operator fun invoke(width: Int, height: Int): ISizeInt = SizeInt(width, height)
    }
}

inline class SizeInt(val size: MSize) : ISizeInt {
    companion object {
        operator fun invoke(): SizeInt = SizeInt(MSize(0, 0))
        operator fun invoke(x: Int, y: Int): SizeInt = SizeInt(MSize(x, y))
        operator fun invoke(that: ISizeInt): SizeInt = SizeInt(MSize(that.width, that.height))
    }

    fun clone() = SizeInt(size.clone())

    override var width: Int
        set(value) { size.width = value.toDouble() }
        get() = size.width.toInt()
    override var height: Int
        set(value) { size.height = value.toDouble() }
        get() = size.height.toInt()

    //override fun toString(): String = "SizeInt($width, $height)"
    override fun toString(): String = "SizeInt(width=$width, height=$height)"
}

fun SizeInt.setTo(width: Int, height: Int) : SizeInt {
    this.width = width
    this.height = height

    return this
}

fun SizeInt.setTo(that: ISizeInt) = setTo(that.width, that.height)

fun SizeInt.setToScaled(sx: Double, sy: Double) = setTo((this.width * sx).toInt(), (this.height * sy).toInt())
fun SizeInt.setToScaled(sx: Int, sy: Int) = setToScaled(sx.toDouble(), sy.toDouble())
fun SizeInt.setToScaled(sx: Float, sy: Float) = setToScaled(sx.toDouble(), sy.toDouble())

fun SizeInt.anchoredIn(container: RectangleInt, anchor: Anchor, out: RectangleInt = RectangleInt()): RectangleInt {
    return out.setTo(
        ((container.width - this.width) * anchor.sx).toInt(),
        ((container.height - this.height) * anchor.sy).toInt(),
        width,
        height
    )
}

operator fun SizeInt.contains(v: SizeInt): Boolean = (v.width <= width) && (v.height <= height)
operator fun SizeInt.times(v: Double) = SizeInt(MSize((width * v).toInt(), (height * v).toInt()))
operator fun SizeInt.times(v: Int) = this * v.toDouble()
operator fun SizeInt.times(v: Float) = this * v.toDouble()

fun SizeInt.getAnchorPosition(anchor: Anchor, out: MPointInt = MPointInt(0, 0)): MPointInt =
    out.setTo((width * anchor.sx).toInt(), (height * anchor.sy).toInt())

fun MSize.asInt(): SizeInt = SizeInt(this)
fun SizeInt.asDouble(): MSize = this.size

interface Sizeable {
    val size: MSize
}
