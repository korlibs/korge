package com.soywiz.korma.geom

import com.soywiz.kds.pack.*
import com.soywiz.korma.annotations.*
import com.soywiz.korma.internal.niceStr
import com.soywiz.korma.interpolation.*
import kotlin.math.max
import kotlin.math.min

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

    //(val width: Double, val height: Double) {
    constructor() : this(0f, 0f)
    constructor(width: Float, height: Float) : this(Float2Pack(width, height))
    constructor(width: Double, height: Double) : this(Float2Pack(width.toFloat(), height.toFloat()))
    constructor(width: Int, height: Int) : this(width.toFloat(), height.toFloat())

    override fun toString(): String = "Size(width=${width.niceStr}, height=${height.niceStr})"
}

val Size.mutable: MSize get() = MSize(width, height)

inline class SizeInt internal constructor(internal val raw: Int2Pack) {
    val width: Int get() = raw.x
    val height: Int get() = raw.y

    constructor() : this(0, 0)
    constructor(width: Int, height: Int) : this(Int2Pack(width, height))
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

@KormaMutableApi
interface ISizeable {
    val size: ISize
}

@KormaMutableApi
@Deprecated("Use Size instead")
sealed interface ISize {
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

@KormaMutableApi
@Deprecated("Use Size instead")
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

    override fun interpolateWith(ratio: Ratio, other: MSize): MSize = MSize(0, 0).setToInterpolated(ratio, this, other)

    override fun setToInterpolated(ratio: Ratio, l: MSize, r: MSize): MSize = this.setTo(
        ratio.interpolate(l.width, r.width),
        ratio.interpolate(l.height, r.height)
    )

    override fun toString(): String = "Size(width=${width.niceStr}, height=${height.niceStr})"
}

@KormaMutableApi
sealed interface ISizeInt {
    val width: Int
    val height: Int

    companion object {
        operator fun invoke(width: Int, height: Int): ISizeInt = MSizeInt(width, height)
    }
}

fun ISizeInt.clone(): MSizeInt = MSizeInt(width, height)

@KormaMutableApi
inline class MSizeInt(val size: MSize) : ISizeInt {
    companion object {
        operator fun invoke(): MSizeInt = MSizeInt(MSize(0, 0))
        operator fun invoke(width: Int, height: Int): MSizeInt = MSizeInt(MSize(width, height))
        operator fun invoke(that: ISizeInt): MSizeInt = MSizeInt(MSize(that.width, that.height))
    }

    fun clone(): MSizeInt = MSizeInt(size.clone())

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
            ((container.width - this.width) * anchor.sxD).toInt(),
            ((container.height - this.height) * anchor.syD).toInt(),
            width,
            height
        )
    }

    operator fun contains(v: MSizeInt): Boolean = (v.width <= width) && (v.height <= height)
    operator fun times(v: Double) = MSizeInt(MSize((width * v).toInt(), (height * v).toInt()))
    operator fun times(v: Int) = this * v.toDouble()
    operator fun times(v: Float) = this * v.toDouble()

    fun getAnchorPosition(anchor: Anchor, out: MPointInt = MPointInt(0, 0)): MPointInt =
        out.setTo((width * anchor.sxD).toInt(), (height * anchor.syD).toInt())

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
fun PointInt.toSize(): SizeInt = SizeInt(x, y)

fun Size.toInt(): SizeInt = SizeInt(width.toInt(), height.toInt())
fun SizeInt.toFloat(): Size = Size(width.toFloat(), height.toFloat())
