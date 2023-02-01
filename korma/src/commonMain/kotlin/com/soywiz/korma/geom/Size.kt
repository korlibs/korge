package com.soywiz.korma.geom

import com.soywiz.korma.internal.niceStr
import com.soywiz.korma.interpolation.Interpolable
import com.soywiz.korma.interpolation.MutableInterpolable
import com.soywiz.korma.interpolation.interpolate
import kotlin.math.max
import kotlin.math.min

typealias ISize = Size

fun Point.asSize(): Size = Size(this)

inline class Size(val p: Point) : Interpolable<Size>, Sizeable {
    companion object {
        operator fun invoke(): Size = Size(Point(0, 0))
        operator fun invoke(width: Double, height: Double): Size = Size(Point(width, height))
        operator fun invoke(width: Int, height: Int): Size = Size(Point(width, height))
        operator fun invoke(width: Float, height: Float): Size = Size(Point(width, height))
    }

    fun copy() = Size(p.copy())

    override val size: Size get() = this

    val width: Double get() = p.x
    val height: Double get() = p.y

    val area: Double get() = width * height
    val perimeter: Double get() = width * 2 + height * 2
    val min: Double get() = min(width, height)
    val max: Double get() = max(width, height)



    fun scaled(sx: Double, sy: Double): Size = Size(p * Point(sx, sy))
    fun scaled(sx: Float, sy: Float): Size = Size(p * Point(sx, sy))
    fun scaled(sx: Int, sy: Int): Size = Size(p * Point(sx, sy))

    fun clone() = Size(width, height)

    override fun interpolateWith(ratio: Double, other: Size): Size = interpolated(ratio, this, other)

    fun interpolated(ratio: Double, l: Size, r: Size): Size = Size(
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

val Size.int: SizeInt get() = SizeInt(this)
val SizeInt.double: Size get() = this.size

inline class SizeInt(val size: Size) : ISizeInt {
    companion object {
        operator fun invoke(): SizeInt = SizeInt(Size(0, 0))
        operator fun invoke(x: Int, y: Int): SizeInt = SizeInt(Size(x, y))
        operator fun invoke(that: ISizeInt): SizeInt = SizeInt(Size(that.width, that.height))
    }

    fun clone() = SizeInt(size.clone())

    override val width: Int
        get() = size.width.toInt()
    override val height: Int
        get() = size.height.toInt()

    //override fun toString(): String = "SizeInt($width, $height)"
    override fun toString(): String = "SizeInt(width=$width, height=$height)"
}

fun SizeInt.scaled(sx: Double, sy: Double): SizeInt = SizeInt(this.size.scaled(sx, sy))
fun SizeInt.scaled(sx: Int, sy: Int): SizeInt = SizeInt(this.size.scaled(sx, sy))
fun SizeInt.scaled(sx: Float, sy: Float): SizeInt = SizeInt(this.size.scaled(sx, sy))

fun SizeInt.anchoredIn(container: RectangleInt, anchor: Anchor, out: RectangleInt = RectangleInt()): RectangleInt {
    return out.setTo(
        ((container.width - this.width) * anchor.sx).toInt(),
        ((container.height - this.height) * anchor.sy).toInt(),
        width,
        height
    )
}

operator fun SizeInt.contains(v: SizeInt): Boolean = (v.width <= width) && (v.height <= height)
operator fun SizeInt.times(v: Double) = SizeInt(Size((width * v).toInt(), (height * v).toInt()))
operator fun SizeInt.times(v: Int) = this * v.toDouble()
operator fun SizeInt.times(v: Float) = this * v.toDouble()

fun SizeInt.getAnchorPosition(anchor: Anchor): PointInt =
    PointInt((width * anchor.sx).toInt(), (height * anchor.sy).toInt())

fun Size.asInt(): SizeInt = SizeInt(this)
fun SizeInt.asDouble(): Size = this.size

interface Sizeable {
    val size: Size
}
