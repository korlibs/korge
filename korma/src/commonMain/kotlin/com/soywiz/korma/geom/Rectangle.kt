package com.soywiz.korma.geom

import com.soywiz.kmem.*
import com.soywiz.kmem.pack.*
import com.soywiz.korma.annotations.*
import kotlin.math.*

//@KormaValueApi
inline class Rectangle(
    val data: Float4Pack
) {
    val position: Point get() = Point(data.x, data.y)
    val size: Size get() = Size(data.w, data.z)

    val x: Float get() = position.x
    val y: Float get() = position.y
    val width: Float get() = size.width
    val height: Float get() = size.height

    companion object {
        operator fun invoke(): Rectangle = Rectangle(Point(), Size())
        operator fun invoke(p: Point, s: Size): Rectangle = Rectangle(float4PackOf(p.x, p.y, s.width, s.height))
        operator fun invoke(x: Int, y: Int, width: Int, height: Int): Rectangle = Rectangle(Point(x, y), Size(width, height))
        operator fun invoke(x: Float, y: Float, width: Float, height: Float): Rectangle = Rectangle(Point(x, y), Size(width, height))
        operator fun invoke(x: Double, y: Double, width: Double, height: Double): Rectangle = Rectangle(Point(x, y), Size(width, height))

        fun fromBounds(left: Double, top: Double, right: Double, bottom: Double): Rectangle = Rectangle(left, top, right - left, bottom - top)
        fun fromBounds(left: Int, top: Int, right: Int, bottom: Int): Rectangle = fromBounds(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())
        fun fromBounds(left: Float, top: Float, right: Float, bottom: Float): Rectangle = fromBounds(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())
        fun fromBounds(point1: Point, point2: Point): Rectangle = Rectangle(point1, (point2 - point1).toSize())
        fun isContainedIn(a: Rectangle, b: Rectangle): Boolean = a.x >= b.x && a.y >= b.y && a.x + a.width <= b.x + b.width && a.y + a.height <= b.y + b.height
    }

    operator fun times(scale: Float): Rectangle = Rectangle(x * scale, y * scale, width * scale, height * scale)
    operator fun times(scale: Double): Rectangle = this * scale.toFloat()
    operator fun times(scale: Int): Rectangle = this * scale.toFloat()

    operator fun div(scale: Float): Rectangle = Rectangle(x / scale, y / scale, width / scale, height / scale)
    operator fun div(scale: Double): Rectangle = this / scale.toFloat()
    operator fun div(scale: Int): Rectangle = this / scale.toFloat()

    operator fun contains(that: Point): Boolean = contains(that.x, that.y)
    operator fun contains(that: PointInt): Boolean = contains(that.x, that.y)
    fun contains(x: Float, y: Float): Boolean = (x >= left && x < right) && (y >= top && y < bottom)
    fun contains(x: Double, y: Double): Boolean = contains(x.toFloat(), y.toFloat())
    fun contains(x: Int, y: Int): Boolean = contains(x.toFloat(), y.toFloat())

    val area: Float get() = width * height
    val isEmpty: Boolean get() = width == 0f && height == 0f
    val isNotEmpty: Boolean get() = !isEmpty
    val mutable: MRectangle get() = MRectangle(x, y, width, height)

    val left: Float get() = x
    val top: Float get() = y
    val right: Float get() = x + width
    val bottom: Float get() = y + height

    val topLeft: Point get() = Point(left, top)
    val topRight: Point get() = Point(right, top)
    val bottomLeft: Point get() = Point(left, bottom)
    val bottomRight: Point get() = Point(right, bottom)

    val centerX: Float get() = (right + left) * 0.5f
    val centerY: Float get() = (bottom + top) * 0.5f
    val center: Point get() = Point(centerX, centerY)

    /**
     * Circle that touches or contains all the corners ([topLeft], [topRight], [bottomLeft], [bottomRight]) of the rectangle.
     */
    fun outerCircle(): Circle {
        val centerX = centerX
        val centerY = centerY
        return Circle(center, Point.distance(centerX, centerY, right, top).toDouble())
    }

    fun without(padding: Margin): Rectangle = Rectangle.fromBounds(
        left + padding.left,
        top + padding.top,
        right - padding.right,
        bottom - padding.bottom
    )

    fun with(margin: Margin): Rectangle = Rectangle.fromBounds(
        left - margin.left,
        top - margin.top,
        right + margin.right,
        bottom + margin.bottom
    )

    infix fun intersects(that: Rectangle): Boolean = intersectsX(that) && intersectsY(that)
    infix fun intersectsX(that: Rectangle): Boolean = that.left <= this.right && that.right >= this.left
    infix fun intersectsY(that: Rectangle): Boolean = that.top <= this.bottom && that.bottom >= this.top

    fun intersection(that: Rectangle): Rectangle? = if (this intersects that) Rectangle(
        max(this.left, that.left), max(this.top, that.top),
        min(this.right, that.right), min(this.bottom, that.bottom)
    ) else null

    fun toInt(): RectangleInt = RectangleInt(x.toInt(), y.toInt(), width.toInt(), height.toInt())
    fun toIntRound(): RectangleInt = RectangleInt(x.toIntRound(), y.toIntRound(), width.toIntRound(), height.toIntRound())
    fun toIntCeil(): RectangleInt = RectangleInt(x.toIntCeil(), y.toIntCeil(), width.toIntCeil(), height.toIntCeil())
    fun toIntFloor(): RectangleInt = RectangleInt(x.toIntFloor(), y.toIntFloor(), width.toIntFloor(), height.toIntFloor())

    @KormaMutableApi fun toMRectangle(out: MRectangle = MRectangle()): MRectangle = out.setTo(x, y, width, height)
}

@KormaMutableApi
fun Iterable<MRectangle>.bounds(target: MRectangle = MRectangle()): MRectangle {
    var first = true
    var left = 0.0
    var right = 0.0
    var top = 0.0
    var bottom = 0.0
    for (r in this) {
        if (first) {
            left = r.left
            right = r.right
            top = r.top
            bottom = r.bottom
            first = false
        } else {
            left = min(left, r.left)
            right = max(right, r.right)
            top = min(top, r.top)
            bottom = max(bottom, r.bottom)
        }
    }
    return target.setBounds(left, top, right, bottom)
}
