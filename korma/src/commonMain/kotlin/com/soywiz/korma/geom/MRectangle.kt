package com.soywiz.korma.geom

import com.soywiz.kds.ConcurrentPool
import com.soywiz.korma.annotations.*
import com.soywiz.korma.internal.niceStr
import com.soywiz.korma.interpolation.Interpolable
import com.soywiz.korma.interpolation.MutableInterpolable
import com.soywiz.korma.interpolation.Ratio
import com.soywiz.korma.interpolation.interpolate
import com.soywiz.korma.math.isAlmostEquals
import com.soywiz.korma.math.roundDecimalPlaces

@KormaMutableApi
@Deprecated("Use Rectangle")
data class MRectangle(
    var x: Double, var y: Double,
    var width: Double, var height: Double
) : MutableInterpolable<MRectangle>, Interpolable<MRectangle>, Sizeable, MSizeable {

    operator fun contains(that: Point) = contains(that.x, that.y)
    operator fun contains(that: MPoint) = contains(that.x, that.y)
    operator fun contains(that: MPointInt) = contains(that.x, that.y)
    fun contains(x: Double, y: Double) = (x >= left && x < right) && (y >= top && y < bottom)
    fun contains(x: Float, y: Float) = contains(x.toDouble(), y.toDouble())
    fun contains(x: Int, y: Int) = contains(x.toDouble(), y.toDouble())

    val area: Double get() = width * height
    val isEmpty: Boolean get() = width == 0.0 && height == 0.0
    val isNotEmpty: Boolean get() = width != 0.0 || height != 0.0
    val mutable: MRectangle get() = MRectangle(x, y, width, height)

    val topLeft: Point get() = Point(left, top)
    val topRight: Point get() = Point(right, top)
    val bottomLeft: Point get() = Point(left, bottom)
    val bottomRight: Point get() = Point(right, bottom)

    val center: Point get() = Point((right + left) * 0.5, (bottom + top) * 0.5)

    /**
     * Circle that touches or contains all the corners ([topLeft], [topRight], [bottomLeft], [bottomRight]) of the rectangle.
     */
    fun outerCircle(): MCircle {
        return MCircle(center, Point.distance(center, topRight).toDouble())
    }


    fun without(padding: Margin): MRectangle = MRectangle.fromBounds(
        left + padding.left,
        top + padding.top,
        right - padding.right,
        bottom - padding.bottom
    )

    fun with(margin: Margin): MRectangle = MRectangle.fromBounds(
        left - margin.left,
        top - margin.top,
        right + margin.right,
        bottom + margin.bottom
    )

    infix fun intersects(that: MRectangle): Boolean = intersectsX(that) && intersectsY(that)

    infix fun intersectsX(that: MRectangle): Boolean =
        that.left <= this.right && that.right >= this.left

    infix fun intersectsY(that: MRectangle): Boolean =
        that.top <= this.bottom && that.bottom >= this.top

    fun intersection(that: MRectangle, target: MRectangle = MRectangle()) =
        if (this intersects that) target.setBounds(
                kotlin.math.max(this.left, that.left), kotlin.math.max(this.top, that.top),
                kotlin.math.min(this.right, that.right), kotlin.math.min(this.bottom, that.bottom)
        ) else null

    companion object {
        val POOL: ConcurrentPool<MRectangle> = ConcurrentPool<MRectangle>({ it.clear() }) { MRectangle() }

        // Creates a rectangle from 2 points where the (x,y) is the top left point
        // with the same width and height as the point. The 2 points provided can be
        // in any arbitrary order, the rectangle will be created from the projected
        // rectangle of the 2 points.
        //
        // Here is one example
        // Rect XY   point1
        // │        │
        // ▼        ▼
        // ┌────────┐
        // │        │
        // │        │
        // └────────┘
        // ▲
        // │
        // point2
        //
        // Here is another example
        // point1 (Rect XY)
        // │
        // ▼
        // ┌────────┐
        // │        │
        // │        │
        // └────────┘
        //          ▲
        //          │
        //        point2
        operator fun invoke(point1: MPoint, point2: MPoint): MRectangle {
            val left = minOf(point1.x, point2.x)
            val top = minOf(point1.y, point2.y)
            val right = maxOf(point1.x, point2.x)
            val bottom = maxOf(point1.y, point2.y)
            return MRectangle(left, top, right - left, bottom - top)
        }

        operator fun invoke(): MRectangle = MRectangle(0.0, 0.0, 0.0, 0.0)
        operator fun invoke(x: Int, y: Int, width: Int, height: Int): MRectangle = MRectangle(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
        operator fun invoke(x: Float, y: Float, width: Float, height: Float): MRectangle = MRectangle(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
        operator fun invoke(topLeft: MPoint, size: MSize): MRectangle = MRectangle(topLeft.x, topLeft.y, size.width, size.height)
        operator fun invoke(topLeft: Point, size: Size): MRectangle = MRectangle(topLeft.x, topLeft.y, size.width, size.height)
        fun fromBounds(left: Double, top: Double, right: Double, bottom: Double): MRectangle = MRectangle().setBounds(left, top, right, bottom)
        fun fromBounds(left: Int, top: Int, right: Int, bottom: Int): MRectangle = MRectangle().setBounds(left, top, right, bottom)
        fun fromBounds(left: Float, top: Float, right: Float, bottom: Float): MRectangle = MRectangle().setBounds(left, top, right, bottom)
        fun fromBounds(point1: MPoint, point2: MPoint): MRectangle = MRectangle(point1, point2)
        fun isContainedIn(a: MRectangle, b: MRectangle): Boolean = a.x >= b.x && a.y >= b.y && a.x + a.width <= b.x + b.width && a.y + a.height <= b.y + b.height
    }

    fun setXY(x: Double, y: Double) {
        this.x = x
        this.y = y
    }

    var left: Double ; get() = x; set(value) { width += (x - value); x = value }
    var top: Double ; get() = y; set(value) { height += (y - value); y = value }

    var right: Double ; get() = x + width ; set(value) { width = value - x }
    var bottom: Double ; get() = y + height ; set(value) { height = value - y }

    val position: MPoint get() = MPoint(x, y)
    override val size: Size get() = Size(width, height)
    override val mSize: MSize get() = MSize(width, height)

    fun setToBounds(left: Double, top: Double, right: Double, bottom: Double): MRectangle = setTo(left, top, right - left, bottom - top)

    fun setTo(x: Double, y: Double, width: Double, height: Double): MRectangle {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
        return this
    }

    fun setTo(x: Int, y: Int, width: Int, height: Int): MRectangle = setTo(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
    fun setTo(x: Float, y: Float, width: Float, height: Float): MRectangle = setTo(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

    fun copyFrom(that: MRectangle): MRectangle = setTo(that.x, that.y, that.width, that.height)
    fun setBounds(left: Double, top: Double, right: Double, bottom: Double): MRectangle = setTo(left, top, right - left, bottom - top)
    fun setBounds(left: Int, top: Int, right: Int, bottom: Int): MRectangle = setBounds(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())
    fun setBounds(left: Float, top: Float, right: Float, bottom: Float): MRectangle = setBounds(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())

    operator fun times(scale: Double): MRectangle = MRectangle(x * scale, y * scale, width * scale, height * scale)
    operator fun times(scale: Float): MRectangle = this * scale.toDouble()
    operator fun times(scale: Int): MRectangle = this * scale.toDouble()

    operator fun div(scale: Double): MRectangle = MRectangle(x / scale, y / scale, width / scale, height / scale)
    operator fun div(scale: Float): MRectangle = this / scale.toDouble()
    operator fun div(scale: Int): MRectangle = this / scale.toDouble()

    operator fun contains(that: MRectangle) = isContainedIn(that, this)

    fun setToIntersection(a: MRectangle, b: MRectangle): MRectangle? =
        if (a.intersection(b, this) != null) this else null

    fun setToUnion(a: MRectangle, b: MRectangle): MRectangle = setToBounds(
            kotlin.math.min(a.left, b.left),
            kotlin.math.min(a.top, b.top),
            kotlin.math.max(a.right, b.right),
            kotlin.math.max(a.bottom, b.bottom)
    )

    infix fun intersection(that: MRectangle) = intersection(that, MRectangle())

    fun displaced(dx: Double, dy: Double) = MRectangle(this.x + dx, this.y + dy, width, height)
    fun displaced(dx: Float, dy: Float) = displaced(dx.toDouble(), dy.toDouble())
    fun displaced(dx: Int, dy: Int) = displaced(dx.toDouble(), dy.toDouble())

    fun displace(dx: Double, dy: Double) = setTo(this.x + dx, this.y + dy, this.width, this.height)
    fun displace(dx: Float, dy: Float) = displace(dx.toDouble(), dy.toDouble())
    fun displace(dx: Int, dy: Int) = displace(dx.toDouble(), dy.toDouble())

    fun place(item: MSize, anchor: Anchor, scale: ScaleMode, out: MRectangle = MRectangle()): MRectangle =
        place(item.width, item.height, anchor, scale, out)

    fun place(
            width: Double,
            height: Double,
            anchor: Anchor,
            scale: ScaleMode,
            out: MRectangle = MRectangle()
    ): MRectangle {
        val (ow, oh) = scale.transform(Size(width, height), Size(this.width, this.height))
        val x = (this.width - ow) * anchor.doubleX
        val y = (this.height - oh) * anchor.doubleY
        return out.setTo(x, y, ow.toDouble(), oh.toDouble())
    }

    fun inflate(
        left: Double,
        top: Double = left,
        right: Double = left,
        bottom: Double = top
    ): MRectangle = setBounds(this.left - left, this.top - top, this.right + right, this.bottom + bottom)

    fun clear(): MRectangle = setTo(0.0, 0.0, 0.0, 0.0)

    fun clone(): MRectangle = MRectangle(x, y, width, height)

    fun setToAnchoredRectangle(item: MRectangle, anchor: Anchor, container: MRectangle): MRectangle =
        setToAnchoredRectangle(item.mSize, anchor, container)

    fun setToAnchoredRectangle(item: MSize, anchor: Anchor, container: MRectangle): MRectangle =
        setToAnchoredRectangle(item.immutable, anchor, container)

    fun setToAnchoredRectangle(item: Size, anchor: Anchor, container: MRectangle): MRectangle = setTo(
        (container.x + anchor.doubleX * (container.width - item.width)).toFloat(),
        (container.y + anchor.doubleY * (container.height - item.height)).toFloat(),
        item.width,
        item.height
    )

    fun applyTransform(m: Matrix): MRectangle {
        val tl = m.transform(Point(left, top))
        val tr = m.transform(Point(right, top))
        val bl = m.transform(Point(left, bottom))
        val br = m.transform(Point(right, bottom))

        val minX = com.soywiz.korma.math.min(tl.x, tr.x, bl.x, br.x)
        val minY = com.soywiz.korma.math.min(tl.y, tr.y, bl.y, br.y)
        val maxX = com.soywiz.korma.math.max(tl.x, tr.x, bl.x, br.x)
        val maxY = com.soywiz.korma.math.max(tl.y, tr.y, bl.y, br.y)

        //val l = m.transformX(left, top)
        //val t = m.transformY(left, top)
        //val r = m.transformX(right, bottom)
        //val b = m.transformY(right, bottom)
        return setBounds(minX, minY, maxX, maxY)
    }

    //override fun toString(): String = "Rectangle([${left.niceStr}, ${top.niceStr}]-[${right.niceStr}, ${bottom.niceStr}])"
    override fun toString(): String = "Rectangle(x=${x.niceStr}, y=${y.niceStr}, width=${width.niceStr}, height=${height.niceStr})"
    fun toStringBounds(): String = "Rectangle([${left.niceStr},${top.niceStr}]-[${right.niceStr},${bottom.niceStr}])"
    fun toStringSize(): String = "Rectangle([${left.niceStr},${top.niceStr}],[${width.niceStr},${height.niceStr}])"
    fun toStringCompat(): String = "Rectangle(x=${left.niceStr}, y=${top.niceStr}, w=${width.niceStr}, h=${height.niceStr})"

    override fun equals(other: Any?): Boolean = other is MRectangle
        && x.isAlmostEquals(other.x)
        && y.isAlmostEquals(other.y)
        && width.isAlmostEquals(other.width)
        && height.isAlmostEquals(other.height)

    override fun interpolateWith(ratio: Ratio, other: MRectangle): MRectangle =
        MRectangle().setToInterpolated(ratio, this, other)

    override fun setToInterpolated(ratio: Ratio, l: MRectangle, r: MRectangle): MRectangle =
        this.setTo(
            ratio.interpolate(l.x, r.x),
            ratio.interpolate(l.y, r.y),
            ratio.interpolate(l.width, r.width),
            ratio.interpolate(l.height, r.height)
        )

    val immutable: Rectangle get() = Rectangle(x, y, width, height)

    fun toInt(): MRectangleInt = MRectangleInt(x.toInt(), y.toInt(), width.toInt(), height.toInt())
    fun floor(): MRectangle = setTo(
            kotlin.math.floor(x),
            kotlin.math.floor(y),
            kotlin.math.floor(width),
            kotlin.math.floor(height)
    )

    fun round(): MRectangle = setTo(
            kotlin.math.round(x),
            kotlin.math.round(y),
            kotlin.math.round(width),
            kotlin.math.round(height)
    )

    fun roundDecimalPlaces(places: Int): MRectangle = setTo(
        x.roundDecimalPlaces(places),
        y.roundDecimalPlaces(places),
        width.roundDecimalPlaces(places),
        height.roundDecimalPlaces(places)
    )

    fun ceil(): MRectangle = setTo(
            kotlin.math.ceil(x),
            kotlin.math.ceil(y),
            kotlin.math.ceil(width),
            kotlin.math.ceil(height)
    )

    fun normalize() {
        if (width < 0.0) {
            x += width
            width = -width
        }
        if (height < 0.0) {
            y += height
            height = -height
        }
    }

    fun expand(left: Float, top: Float, right: Float, bottom: Float): MRectangle =
        this.setToBounds(this.left - left, this.top - top, this.right + right, this.bottom + bottom)

    fun expand(left: Int, top: Int, right: Int, bottom: Int): MRectangle =
        expand(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())

    fun expand(margin: Margin): MRectangle =
        expand(margin.left, margin.top, margin.right, margin.bottom)

    fun expand(margin: MarginInt): MRectangle =
        expand(margin.left, margin.top, margin.right, margin.bottom)

    fun toRectangle(): Rectangle = Rectangle(x, y, width, height)
    @KormaMutableApi fun asInt(): MRectangleInt = MRectangleInt(this)
    @KormaMutableApi val int: MRectangleInt get() = MRectangleInt(x, y, width, height)
    @KormaValueApi val value: Rectangle get() = Rectangle(x, y, width, height)

}
