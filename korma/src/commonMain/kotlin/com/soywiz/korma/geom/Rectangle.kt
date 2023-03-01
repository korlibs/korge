package com.soywiz.korma.geom

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korma.annotations.*
import com.soywiz.korma.internal.niceStr
import com.soywiz.korma.interpolation.*
import com.soywiz.korma.math.*
import kotlin.math.max
import kotlin.math.min

@KormaValueApi
data class Rectangle(
    val position: Point,
    val size: Size,
) {
    val x: Float get() = position.x
    val y: Float get() = position.y
    val width: Float get() = size.width
    val height: Float get() = size.height

    companion object {
        operator fun invoke(): Rectangle = Rectangle(Point(), Size())
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
}

@KormaValueApi
data class RectangleInt(
    val position: PointInt,
    val size: SizeInt
) {
    val x: Int get() = position.x
    val y: Int get() = position.y
    val width: Int get() = size.width
    val height: Int get() = size.height

    val area: Int get() = width * height
    val isEmpty: Boolean get() = width == 0 && height == 0
    val isNotEmpty: Boolean get() = !isEmpty
    val mutable: MRectangleInt get() = MRectangleInt(x, y, width, height)

    val left: Int get() = x
    val top: Int get() = y
    val right: Int get() = x + width
    val bottom: Int get() = y + height

    val topLeft: PointInt get() = PointInt(left, top)
    val topRight: PointInt get() = PointInt(right, top)
    val bottomLeft: PointInt get() = PointInt(left, bottom)
    val bottomRight: PointInt get() = PointInt(right, bottom)

    val centerX: Int get() = ((right + left) * 0.5f).toInt()
    val centerY: Int get() = ((bottom + top) * 0.5f).toInt()
    val center: PointInt get() = PointInt(centerX, centerY)

    operator fun times(scale: Float): RectangleInt = RectangleInt(
        (x * scale).toInt(), (y * scale).toInt(),
        (width * scale).toInt(), (height * scale).toInt()
    )
    operator fun times(scale: Double): RectangleInt = this * scale.toFloat()
    operator fun times(scale: Int): RectangleInt = this * scale.toFloat()

    operator fun div(scale: Float): RectangleInt = RectangleInt(
        (x / scale).toInt(), (y / scale).toInt(),
        (width / scale).toInt(), (height / scale).toInt()
    )
    operator fun div(scale: Double): RectangleInt = this / scale.toFloat()
    operator fun div(scale: Int): RectangleInt = this / scale.toFloat()

    operator fun contains(that: Point): Boolean = contains(that.x, that.y)
    operator fun contains(that: PointInt): Boolean = contains(that.x, that.y)
    fun contains(x: Float, y: Float): Boolean = (x >= left && x < right) && (y >= top && y < bottom)
    fun contains(x: Double, y: Double): Boolean = contains(x.toFloat(), y.toFloat())
    fun contains(x: Int, y: Int): Boolean = contains(x.toFloat(), y.toFloat())

    constructor() : this(PointInt(), SizeInt())
    constructor(x: Int, y: Int, width: Int, height: Int) : this(PointInt(x, y), SizeInt(width, height))

    fun sliceWithBounds(left: Int, top: Int, right: Int, bottom: Int, clamped: Boolean = true): RectangleInt {
        val left = if (!clamped) left else left.coerceIn(0, this.width)
        val right = if (!clamped) right else right.coerceIn(0, this.width)
        val top = if (!clamped) top else top.coerceIn(0, this.height)
        val bottom = if (!clamped) bottom else bottom.coerceIn(0, this.height)
        return fromBounds(this.x + left, this.y + top, this.x + right, this.y + bottom)
    }

    fun sliceWithSize(x: Int, y: Int, width: Int, height: Int, clamped: Boolean = true): RectangleInt =
        sliceWithBounds(x, y, x + width, y + height, clamped)

    fun expanded(border: MarginInt): RectangleInt =
        fromBounds(left - border.left, top - border.top, right + border.right, bottom + border.bottom)

    override fun toString(): String = "Rectangle(x=${x}, y=${y}, width=${width}, height=${height})"

    companion object {
        fun union(a: RectangleInt, b: RectangleInt): RectangleInt = fromBounds(
            min(a.left, b.left),
            min(a.top, b.top),
            max(a.right, b.right),
            max(a.bottom, b.bottom)
        )

        fun fromBounds(topLeft: PointInt, bottomRight: PointInt): RectangleInt = RectangleInt(topLeft, (bottomRight - topLeft).toSize())
        fun fromBounds(left: Int, top: Int, right: Int, bottom: Int): RectangleInt = fromBounds(PointInt(left, top), PointInt(right, bottom))
    }
}

fun Rectangle.toInt(): RectangleInt = RectangleInt(x.toInt(), y.toInt(), width.toInt(), height.toInt())
fun Rectangle.toIntRound(): RectangleInt = RectangleInt(x.toIntRound(), y.toIntRound(), width.toIntRound(), height.toIntRound())
fun Rectangle.toIntCeil(): RectangleInt = RectangleInt(x.toIntCeil(), y.toIntCeil(), width.toIntCeil(), height.toIntCeil())
fun Rectangle.toIntFloor(): RectangleInt = RectangleInt(x.toIntFloor(), y.toIntFloor(), width.toIntFloor(), height.toIntFloor())

fun RectangleInt.toFloat(): Rectangle = Rectangle(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())

@KormaMutableApi
interface IRectangle {
    val x: Double
    val y: Double
    val width: Double
    val height: Double

    val position: IPoint get() = MPoint(x, y)
    val size: ISize get() = MSize(width, height)

    operator fun contains(that: IPoint) = contains(that.x, that.y)
    operator fun contains(that: IPointInt) = contains(that.x, that.y)
    fun contains(x: Double, y: Double) = (x >= left && x < right) && (y >= top && y < bottom)
    fun contains(x: Float, y: Float) = contains(x.toDouble(), y.toDouble())
    fun contains(x: Int, y: Int) = contains(x.toDouble(), y.toDouble())

    fun clone(): MRectangle = MRectangle(x, y, width, height)

    val area: Double get() = width * height
    val isEmpty: Boolean get() = width == 0.0 && height == 0.0
    val isNotEmpty: Boolean get() = width != 0.0 || height != 0.0
    val mutable: MRectangle get() = MRectangle(x, y, width, height)

    val left get() = x
    val top get() = y
    val right get() = x + width
    val bottom get() = y + height

    val topLeft: Point get() = Point(left, top)
    val topRight: Point get() = Point(right, top)
    val bottomLeft: Point get() = Point(left, bottom)
    val bottomRight: Point get() = Point(right, bottom)

    val centerX: Double get() = (right + left) * 0.5
    val centerY: Double get() = (bottom + top) * 0.5
    val center: Point get() = Point(centerX, centerY)

    /**
     * Circle that touches or contains all the corners ([topLeft], [topRight], [bottomLeft], [bottomRight]) of the rectangle.
     */
    fun outerCircle(): MCircle {
        val centerX = centerX
        val centerY = centerY
        return MCircle(center, Point.distance(centerX, centerY, right, top))
    }


    fun without(padding: IMargin): MRectangle = MRectangle.fromBounds(
        left + padding.left,
        top + padding.top,
        right - padding.right,
        bottom - padding.bottom
    )

    fun with(margin: IMargin): MRectangle = MRectangle.fromBounds(
        left - margin.left,
        top - margin.top,
        right + margin.right,
        bottom + margin.bottom
    )

    infix fun intersects(that: IRectangle): Boolean = intersectsX(that) && intersectsY(that)

    infix fun intersectsX(that: IRectangle): Boolean =
        that.left <= this.right && that.right >= this.left

    infix fun intersectsY(that: IRectangle): Boolean =
        that.top <= this.bottom && that.bottom >= this.top

    fun intersection(that: IRectangle, target: MRectangle = MRectangle()) =
        if (this intersects that) target.setBounds(
            max(this.left, that.left), max(this.top, that.top),
            min(this.right, that.right), min(this.bottom, that.bottom)
        ) else null

    companion object {
        inline operator fun invoke(
            x: Double,
            y: Double,
            width: Double,
            height: Double
        ): IRectangle = MRectangle(x, y, width, height)

        inline operator fun invoke(x: Float, y: Float, width: Float, height: Float): IRectangle = MRectangle(x, y, width, height)
        inline operator fun invoke(x: Int, y: Int, width: Int, height: Int): IRectangle = MRectangle(x, y, width, height)

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
        operator fun invoke(point1: IPoint, point2: IPoint): MRectangle {
            val left = minOf(point1.x, point2.x)
            val top = minOf(point1.y, point2.y)
            val right = maxOf(point1.x, point2.x)
            val bottom = maxOf(point1.y, point2.y)
            return MRectangle(left, top, right - left, bottom - top)
        }
    }
}

@KormaMutableApi
data class MRectangle(
    override var x: Double, override var y: Double,
    override var width: Double, override var height: Double
) : MutableInterpolable<IRectangle>, Interpolable<IRectangle>, IRectangle, ISizeable {

    companion object {
        val POOL: ConcurrentPool<MRectangle> = ConcurrentPool<MRectangle>({ it.clear() }) { MRectangle() }

        operator fun invoke(): MRectangle = MRectangle(0.0, 0.0, 0.0, 0.0)
        operator fun invoke(x: Int, y: Int, width: Int, height: Int): MRectangle = MRectangle(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
        operator fun invoke(x: Float, y: Float, width: Float, height: Float): MRectangle = MRectangle(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
        operator fun invoke(topLeft: IPoint, size: ISize): MRectangle = MRectangle(topLeft.x, topLeft.y, size.width, size.height)
        fun fromBounds(left: Double, top: Double, right: Double, bottom: Double): MRectangle = MRectangle().setBounds(left, top, right, bottom)
        fun fromBounds(left: Int, top: Int, right: Int, bottom: Int): MRectangle = MRectangle().setBounds(left, top, right, bottom)
        fun fromBounds(left: Float, top: Float, right: Float, bottom: Float): MRectangle = MRectangle().setBounds(left, top, right, bottom)
        fun fromBounds(point1: IPoint, point2: IPoint): MRectangle = IRectangle(point1, point2)
        fun isContainedIn(a: IRectangle, b: IRectangle): Boolean = a.x >= b.x && a.y >= b.y && a.x + a.width <= b.x + b.width && a.y + a.height <= b.y + b.height
    }

    fun setXY(x: Double, y: Double) {
        this.x = x
        this.y = y
    }

    override var left: Double ; get() = x; set(value) { width += (x - value); x = value }
    override var top: Double ; get() = y; set(value) { height += (y - value); y = value }

    override var right: Double ; get() = x + width ; set(value) { width = value - x }
    override var bottom: Double ; get() = y + height ; set(value) { height = value - y }

    override val position: MPoint get() = MPoint(x, y)
    override val size: MSize get() = MSize(width, height)

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

    fun copyFrom(that: IRectangle): MRectangle = setTo(that.x, that.y, that.width, that.height)
    fun setBounds(left: Double, top: Double, right: Double, bottom: Double): MRectangle = setTo(left, top, right - left, bottom - top)
    fun setBounds(left: Int, top: Int, right: Int, bottom: Int): MRectangle = setBounds(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())
    fun setBounds(left: Float, top: Float, right: Float, bottom: Float): MRectangle = setBounds(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())

    operator fun times(scale: Double): MRectangle = MRectangle(x * scale, y * scale, width * scale, height * scale)
    operator fun times(scale: Float): MRectangle = this * scale.toDouble()
    operator fun times(scale: Int): MRectangle = this * scale.toDouble()

    operator fun div(scale: Double): MRectangle = MRectangle(x / scale, y / scale, width / scale, height / scale)
    operator fun div(scale: Float): MRectangle = this / scale.toDouble()
    operator fun div(scale: Int): MRectangle = this / scale.toDouble()

    operator fun contains(that: IRectangle) = isContainedIn(that, this)

    fun setToIntersection(a: IRectangle, b: IRectangle): MRectangle? =
        if (a.intersection(b, this) != null) this else null

    fun setToUnion(a: IRectangle, b: IRectangle): MRectangle = setToBounds(
        min(a.left, b.left),
        min(a.top, b.top),
        max(a.right, b.right),
        max(a.bottom, b.bottom)
    )

    infix fun intersection(that: IRectangle) = intersection(that, MRectangle())

    fun displaced(dx: Double, dy: Double) = MRectangle(this.x + dx, this.y + dy, width, height)
    fun displaced(dx: Float, dy: Float) = displaced(dx.toDouble(), dy.toDouble())
    fun displaced(dx: Int, dy: Int) = displaced(dx.toDouble(), dy.toDouble())

    fun displace(dx: Double, dy: Double) = setTo(this.x + dx, this.y + dy, this.width, this.height)
    fun displace(dx: Float, dy: Float) = displace(dx.toDouble(), dy.toDouble())
    fun displace(dx: Int, dy: Int) = displace(dx.toDouble(), dy.toDouble())

    fun place(item: ISize, anchor: Anchor, scale: ScaleMode, out: MRectangle = MRectangle()): MRectangle =
        place(item.width, item.height, anchor, scale, out)

    fun place(
        width: Double,
        height: Double,
        anchor: Anchor,
        scale: ScaleMode,
        out: MRectangle = MRectangle()
    ): MRectangle {
        val ow = scale.transformW(width, height, this.width, this.height)
        val oh = scale.transformH(width, height, this.width, this.height)
        val x = (this.width - ow) * anchor.sx
        val y = (this.height - oh) * anchor.sy
        return out.setTo(x, y, ow, oh)
    }

    fun inflate(
        left: Double,
        top: Double = left,
        right: Double = left,
        bottom: Double = top
    ): MRectangle = setBounds(this.left - left, this.top - top, this.right + right, this.bottom + bottom)

    fun clear(): MRectangle = setTo(0.0, 0.0, 0.0, 0.0)

    override fun clone(): MRectangle = MRectangle(x, y, width, height)

    fun setToAnchoredRectangle(item: IRectangle, anchor: Anchor, container: IRectangle): MRectangle =
        setToAnchoredRectangle(item.size, anchor, container)

    fun setToAnchoredRectangle(item: ISize, anchor: Anchor, container: IRectangle): MRectangle = setTo(
        container.x + anchor.sx * (container.width - item.width),
        container.y + anchor.sy * (container.height - item.height),
        item.width,
        item.height
    )

    fun applyTransform(m: IMatrix): MRectangle {
        val tl = m.transform(left, top)
        val tr = m.transform(right, top)
        val bl = m.transform(left, bottom)
        val br = m.transform(right, bottom)

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

    override fun equals(other: Any?): Boolean = other is IRectangle
        && x.isAlmostEquals(other.x)
        && y.isAlmostEquals(other.y)
        && width.isAlmostEquals(other.width)
        && height.isAlmostEquals(other.height)

    override fun interpolateWith(ratio: Ratio, other: IRectangle): MRectangle =
        MRectangle().setToInterpolated(ratio, this, other)

    override fun setToInterpolated(ratio: Ratio, l: IRectangle, r: IRectangle): MRectangle =
        this.setTo(
            ratio.interpolate(l.x, r.x),
            ratio.interpolate(l.y, r.y),
            ratio.interpolate(l.width, r.width),
            ratio.interpolate(l.height, r.height)
        )

    fun getMiddlePoint(): Point = getAnchoredPosition(Anchor.CENTER)

    fun getAnchoredPosition(anchor: Anchor): Point =
        getAnchoredPosition(anchor.sxD, anchor.syD)

    fun getAnchoredPosition(anchorX: Double, anchorY: Double): Point =
        Point(left + width * anchorX, top + height * anchorY)

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

    fun expand(left: Double, top: Double, right: Double, bottom: Double): MRectangle =
        this.setToBounds(this.left - left, this.top - top, this.right + right, this.bottom + bottom)

    fun expand(left: Int, top: Int, right: Int, bottom: Int): MRectangle =
        expand(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())

    fun expand(margin: IMargin): MRectangle =
        expand(margin.left, margin.top, margin.right, margin.bottom)

    fun expand(margin: IMarginInt): MRectangle =
        expand(margin.left, margin.top, margin.right, margin.bottom)

    fun expand(margin: MarginInt): MRectangle =
        expand(margin.left, margin.top, margin.right, margin.bottom)
}

//////////// INT

@KormaMutableApi
sealed interface IRectangleInt {
    companion object {
        operator fun invoke(x: Int, y: Int, width: Int, height: Int): IRectangleInt = MRectangleInt(x, y, width, height)
        operator fun invoke(x: Float, y: Float, width: Float, height: Float): IRectangleInt = MRectangleInt(x.toInt(), y.toInt(), width.toInt(), height.toInt())
        operator fun invoke(x: Double, y: Double, width: Double, height: Double): IRectangleInt = MRectangleInt(x.toInt(), y.toInt(), width.toInt(), height.toInt())
    }

    val x: Int
    val y: Int
    val width: Int
    val height: Int

    fun clone(): MRectangleInt = MRectangleInt(x, y, width, height)

    fun expanded(border: IMarginInt): IRectangleInt = clone().expand(border)
    fun expanded(border: MarginInt): IRectangleInt = clone().expand(border)

    val left: Int get() = x
    val top: Int get() = y
    val right: Int get() = x + width
    val bottom: Int get() = y + height
    val area: Int get() = width * height

    val topLeft: MPointInt get() = MPointInt(left, top)
    val topRight: MPointInt get() = MPointInt(right, top)
    val bottomLeft: MPointInt get() = MPointInt(left, bottom)
    val bottomRight: MPointInt get() = MPointInt(right, bottom)

    fun sliceWithBounds(left: Int, top: Int, right: Int, bottom: Int, clamped: Boolean = true): IRectangleInt {
        val left = if (!clamped) left else left.coerceIn(0, this.width)
        val right = if (!clamped) right else right.coerceIn(0, this.width)
        val top = if (!clamped) top else top.coerceIn(0, this.height)
        val bottom = if (!clamped) bottom else bottom.coerceIn(0, this.height)
        return MRectangleInt.fromBounds(this.x + left, this.y + top, this.x + right, this.y + bottom)
    }

    fun sliceWithSize(x: Int, y: Int, width: Int, height: Int, clamped: Boolean = true): IRectangleInt =
        sliceWithBounds(x, y, x + width, y + height, clamped)

    operator fun contains(v: ISizeInt): Boolean = (v.width <= width) && (v.height <= height)
    operator fun contains(that: IPoint) = contains(that.x, that.y)
    operator fun contains(that: IPointInt) = contains(that.x, that.y)
    fun contains(x: Double, y: Double) = (x >= left && x < right) && (y >= top && y < bottom)
    fun contains(x: Float, y: Float) = contains(x.toDouble(), y.toDouble())
    fun contains(x: Int, y: Int) = contains(x.toDouble(), y.toDouble())
}

@KormaMutableApi
inline class MRectangleInt(val rect: MRectangle) : IRectangleInt {
    override var x: Int ; get() = rect.x.toInt() ; set(value) { rect.x = value.toDouble() }
    override var y: Int ; get() = rect.y.toInt() ; set(value) { rect.y = value.toDouble() }
    override var width: Int ; get() = rect.width.toInt() ; set(value) { rect.width = value.toDouble() }
    override var height: Int ; get() = rect.height.toInt() ; set(value) { rect.height = value.toDouble() }
    override var left: Int ; get() = rect.left.toInt() ; set(value) { rect.left = value.toDouble() }
    override var top: Int ; get() = rect.top.toInt() ; set(value) { rect.top = value.toDouble() }
    override var right: Int ; get() = rect.right.toInt() ; set(value) { rect.right = value.toDouble() }
    override var bottom: Int ; get() = rect.bottom.toInt() ; set(value) { rect.bottom = value.toDouble() }

    fun anchoredIn(
        container: MRectangleInt,
        anchor: Anchor,
        out: MRectangleInt = MRectangleInt()
    ): MRectangleInt = out.setTo(
        ((container.width - this.width) * anchor.sx).toInt(),
        ((container.height - this.height) * anchor.sy).toInt(),
        width,
        height
    )

    fun getAnchorPosition(anchor: Anchor, out: MPointInt = MPointInt()): MPointInt =
        out.setTo((x + width * anchor.sx).toInt(), (y + height * anchor.sy).toInt())

    val center: IPoint get() = anchor(0.5, 0.5).double
    inline fun anchor(ax: Number, ay: Number): MPointInt = anchor(ax.toDouble(), ay.toDouble())
    fun anchor(ax: Double, ay: Double): MPointInt = MPointInt((x + width * ax).toInt(), (y + height * ay).toInt())

    fun setTo(that: IRectangleInt) = setTo(that.x, that.y, that.width, that.height)
    fun setTo(x: Int, y: Int, width: Int, height: Int): MRectangleInt {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
        return this
    }
    fun setToBounds(left: Int, top: Int, right: Int, bottom: Int): MRectangleInt = setTo(left, top, right - left, bottom - top)

    fun setPosition(x: Int, y: Int): MRectangleInt {
        this.x = x
        this.y = y
        return this
    }

    fun setSize(width: Int, height: Int): MRectangleInt {
        this.width = width
        this.height = height
        return this
    }

    fun getPosition(out: MPointInt = MPointInt()): MPointInt = out.setTo(x, y)
    fun getSize(out: MSizeInt = MSizeInt()): MSizeInt = out.setTo(width, height)

    val position get() = getPosition()
    val size get() = getSize()

    fun setBoundsTo(left: Int, top: Int, right: Int, bottom: Int) =
        setTo(left, top, right - left, bottom - top)

    /** Inline expand the rectangle */
    fun expand(border: IMarginInt): MRectangleInt =
        this.setBoundsTo(left - border.left, top - border.top, right + border.right, bottom + border.bottom)
    fun expand(border: MarginInt): MRectangleInt =
        this.setBoundsTo(left - border.left, top - border.top, right + border.right, bottom + border.bottom)

    companion object {
        operator fun invoke(): MRectangleInt = MRectangleInt(MRectangle())
        operator fun invoke(x: Int, y: Int, width: Int, height: Int): MRectangleInt = MRectangleInt(MRectangle(x, y, width, height))
        operator fun invoke(x: Float, y: Float, width: Float, height: Float): MRectangleInt = MRectangleInt(MRectangle(x, y, width, height))
        operator fun invoke(x: Double, y: Double, width: Double, height: Double): MRectangleInt = MRectangleInt(MRectangle(x, y, width, height))
        operator fun invoke(other: IRectangleInt): MRectangleInt = MRectangleInt(MRectangle(other.x, other.y, other.width, other.height))
        fun fromBounds(left: Int, top: Int, right: Int, bottom: Int): MRectangleInt = MRectangleInt(left, top, right - left, bottom - top)
    }

    override fun toString(): String = "Rectangle(x=$x, y=$y, width=$width, height=$height)"
    fun toStringBounds(): String = "Rectangle([$left,$top]-[$right,$bottom])"
    fun copyFrom(rect: IRectangleInt): MRectangleInt = setTo(rect.x, rect.y, rect.width, rect.height)
    fun copyFrom(rect: RectangleInt): MRectangleInt = setTo(rect.x, rect.y, rect.width, rect.height)

    fun setToUnion(a: IRectangleInt, b: IRectangleInt): MRectangleInt = setToBounds(
        min(a.left, b.left),
        min(a.top, b.top),
        max(a.right, b.right),
        max(a.bottom, b.bottom)
    )

    fun setToUnion(a: IRectangleInt, b: RectangleInt): MRectangleInt = setToBounds(
        min(a.left, b.left),
        min(a.top, b.top),
        max(a.right, b.right),
        max(a.bottom, b.bottom)
    )

    fun setToUnion(a: RectangleInt, b: RectangleInt): MRectangleInt = setToBounds(
        min(a.left, b.left),
        min(a.top, b.top),
        max(a.right, b.right),
        max(a.bottom, b.bottom)
    )
}

////////////////////

@KormaMutableApi fun MRectangle.asInt() = MRectangleInt(this)
@KormaMutableApi fun MRectangleInt.asDouble() = this.rect

@KormaMutableApi val IRectangle.int: MRectangleInt get() = MRectangleInt(x, y, width, height)
@KormaMutableApi val IRectangleInt.float: MRectangle get() = MRectangle(x, y, width, height)

@KormaValueApi val IRectangle.value: Rectangle get() = Rectangle(x, y, width, height)
@KormaValueApi val IRectangleInt.value: Rectangle get() = Rectangle(x, y, width, height)

@KormaValueApi fun IRectangle.toRectangle(): Rectangle = Rectangle(x, y, width, height)
@KormaMutableApi fun Rectangle.toIRectangle(): IRectangle = MRectangle(x, y, width, height)
@KormaMutableApi fun Rectangle.toMRectangle(): MRectangle = MRectangle(x, y, width, height)

///////////////////////////

@KormaMutableApi
fun Iterable<IRectangle>.bounds(target: MRectangle = MRectangle()): MRectangle {
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
