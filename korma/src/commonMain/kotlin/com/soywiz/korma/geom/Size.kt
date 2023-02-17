package com.soywiz.korma.geom

import com.soywiz.korma.annotations.*
import com.soywiz.korma.internal.niceStr
import com.soywiz.korma.interpolation.Interpolable
import com.soywiz.korma.interpolation.MutableInterpolable
import com.soywiz.korma.interpolation.interpolate
import kotlin.math.max
import kotlin.math.min

@KormaValueApi
interface Sizeable {
    val size: Size
}

//@KormaExperimental
@KormaValueApi
data class Size(val width: Double, val height: Double) {
    constructor() : this(0.0, 0.0)
    constructor(width: Float, height: Float) : this(width.toDouble(), height.toDouble())
    constructor(width: Int, height: Int) : this(width.toDouble(), height.toDouble())

    override fun toString(): String = "Size(width=${width.niceStr}, height=${height.niceStr})"
}

@KormaMutableApi
interface ISizeable {
    val size: ISize
}

//@Deprecated("Use Size")
@KormaMutableApi
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

//@Deprecated("Use Size")
@KormaMutableApi
inline class MSize(val p: MPoint) : MutableInterpolable<MSize>, Interpolable<MSize>, ISize, ISizeable {
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

@KormaMutableApi
interface ISizeInt {
    val width: Int
    val height: Int

    companion object {
        operator fun invoke(width: Int, height: Int): ISizeInt = MSizeInt(width, height)
    }
}

@KormaMutableApi
inline class MSizeInt(val size: MSize) : ISizeInt {
    companion object {
        operator fun invoke(): MSizeInt = MSizeInt(MSize(0, 0))
        operator fun invoke(x: Int, y: Int): MSizeInt = MSizeInt(MSize(x, y))
        operator fun invoke(that: ISizeInt): MSizeInt = MSizeInt(MSize(that.width, that.height))
    }

    fun clone() = MSizeInt(size.clone())

    fun setTo(width: Int, height: Int) : MSizeInt {
        this.width = width
        this.height = height

        return this
    }

    fun setTo(that: ISizeInt) = setTo(that.width, that.height)

    fun setToScaled(sx: Double, sy: Double) = setTo((this.width * sx).toInt(), (this.height * sy).toInt())
    fun setToScaled(sx: Int, sy: Int) = setToScaled(sx.toDouble(), sy.toDouble())
    fun setToScaled(sx: Float, sy: Float) = setToScaled(sx.toDouble(), sy.toDouble())

    fun anchoredIn(container: MRectangleInt, anchor: Anchor, out: MRectangleInt = MRectangleInt()): MRectangleInt {
        return out.setTo(
            ((container.width - this.width) * anchor.sx).toInt(),
            ((container.height - this.height) * anchor.sy).toInt(),
            width,
            height
        )
    }

    operator fun contains(v: MSizeInt): Boolean = (v.width <= width) && (v.height <= height)
    operator fun times(v: Double) = MSizeInt(MSize((width * v).toInt(), (height * v).toInt()))
    operator fun times(v: Int) = this * v.toDouble()
    operator fun times(v: Float) = this * v.toDouble()

    fun getAnchorPosition(anchor: Anchor, out: MPointInt = MPointInt(0, 0)): MPointInt =
        out.setTo((width * anchor.sx).toInt(), (height * anchor.sy).toInt())

    override var width: Int
        set(value) { size.width = value.toDouble() }
        get() = size.width.toInt()
    override var height: Int
        set(value) { size.height = value.toDouble() }
        get() = size.height.toInt()

    //override fun toString(): String = "SizeInt($width, $height)"
    override fun toString(): String = "SizeInt(width=$width, height=$height)"
}

fun MSize.asInt(): MSizeInt = MSizeInt(this)
fun MSizeInt.asDouble(): MSize = this.size

fun MPoint.asSize(): MSize = MSize(this)
fun IPoint.asSize(): ISize = MSize(MPoint(this))

fun Point.toSize(): Size = Size(x, y)
