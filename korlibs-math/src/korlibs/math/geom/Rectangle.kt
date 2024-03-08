package korlibs.math.geom

import korlibs.math.*
import korlibs.math.annotations.*
import korlibs.math.geom.shape.*
import korlibs.math.geom.vector.*
import korlibs.math.interpolation.*
import korlibs.number.*
import kotlin.math.*

typealias Rectangle = RectangleD

//@KormaValueApi
//inline class Rectangle(val data: Float4Pack) : Shape2D, Interpolable<Rectangle> {
//inline class Rectangle(val data: Float4) : Shape2D {
data class RectangleD(val x: Double, val y: Double, val width: Double, val height: Double) : Shape2D, IsAlmostEquals<RectangleD> {
    val int: RectangleInt get() = toInt()

    //operator fun component1(): Float = x
    //operator fun component2(): Float = y
    //operator fun component3(): Float = width
    //operator fun component4(): Float = height
    //val x: Float get() = data.f0
    //val y: Float get() = data.f1
    //val width: Float get() = data.f2
    //val height: Float get() = data.f3
    //fun copy(x: Float = this.x, y: Float = this.y, width: Float = this.width, height: Float = this.height): Rectangle = Rectangle(x, y, width, height)

    @Deprecated("", ReplaceWith("this")) fun clone(): Rectangle = this
    @Deprecated("", ReplaceWith("this")) val immutable: Rectangle get() = this

    val position: Point get() = Point(x, y)
    val size: Size get() = Size(width, height)

    @Deprecated("", ReplaceWith("x"))
    val xD: Double get() = x
    @Deprecated("", ReplaceWith("y"))
    val yD: Double get() = y
    @Deprecated("", ReplaceWith("width"))
    val widthD: Double get() = width
    @Deprecated("", ReplaceWith("height"))
    val heightD: Double get() = height

    val isZero: Boolean get() = this == ZERO
    val isInfinite: Boolean get() = this == INFINITE
    //val isNaN: Boolean get() = this == NaN
    val isNaN: Boolean get() = this.x.isNaN()
    val isNIL: Boolean get() = isNaN
    val isNotNIL: Boolean get() = !isNIL

    override fun isAlmostEquals(other: Rectangle, epsilon: Double): Boolean =
        this.x.isAlmostEquals(other.x, epsilon) &&
            this.y.isAlmostEquals(other.y, epsilon) &&
            this.width.isAlmostEquals(other.width, epsilon) &&
            this.height.isAlmostEquals(other.height, epsilon)

    fun toStringBounds(): String = "Rectangle([${left.niceStr},${top.niceStr}]-[${right.niceStr},${bottom.niceStr}])"
    fun toStringSize(): String = "Rectangle([${left.niceStr},${top.niceStr}],[${width.niceStr},${height.niceStr}])"
    fun toStringCompat(): String = "Rectangle(x=${left.niceStr}, y=${top.niceStr}, w=${width.niceStr}, h=${height.niceStr})"

    //override fun interpolateWith(ratio: Ratio, other: Rectangle): Rectangle = interpolated(this, other, ratio)

    override fun toString(): String = when {
        isNIL -> "null"
        else -> "Rectangle(x=${x.niceStr}, y=${y.niceStr}, width=${width.niceStr}, height=${height.niceStr})"
    }

    companion object {
        val ZERO = Rectangle(0, 0, 0, 0)
        val INFINITE = Rectangle(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        val NaN = Rectangle(Float.NaN, Float.NaN, 0f, 0f)
        val NIL get() = NaN

        operator fun invoke(): Rectangle = ZERO
        operator fun invoke(p: Point, s: Size): Rectangle = Rectangle(p.x, p.y, s.width, s.height)
        operator fun invoke(x: Int, y: Int, width: Int, height: Int): Rectangle = Rectangle(Point(x, y), Size(width, height))
        operator fun invoke(x: Float, y: Float, width: Float, height: Float): Rectangle = Rectangle(Point(x, y), Size(width, height))
        operator fun invoke(x: Double, y: Double, width: Double, height: Double): Rectangle = Rectangle(Point(x, y), Size(width, height))
        inline operator fun invoke(x: Number, y: Number, width: Number, height: Number): Rectangle = Rectangle(Point(x, y), Size(width, height))

        fun fromBounds(left: Double, top: Double, right: Double, bottom: Double): Rectangle = Rectangle(left, top, right - left, bottom - top)
        fun fromBounds(left: Int, top: Int, right: Int, bottom: Int): Rectangle = fromBounds(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())
        fun fromBounds(left: Float, top: Float, right: Float, bottom: Float): Rectangle = fromBounds(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())
        fun fromBounds(point1: Point, point2: Point): Rectangle = Rectangle(point1, (point2 - point1).toSize())
        inline fun fromBounds(left: Number, top: Number, right: Number, bottom: Number): Rectangle = fromBounds(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())

        fun isContainedIn(a: Rectangle, b: Rectangle): Boolean = a.x >= b.x && a.y >= b.y && a.x + a.width <= b.x + b.width && a.y + a.height <= b.y + b.height

        fun interpolated(a: Rectangle, b: Rectangle, ratio: Ratio): Rectangle = Rectangle.fromBounds(
            ratio.interpolate(a.left, b.left),
            ratio.interpolate(a.top, b.top),
            ratio.interpolate(a.right, b.right),
            ratio.interpolate(a.bottom, b.bottom),
        )
    }

    operator fun times(scale: Double): Rectangle = Rectangle(x * scale, y * scale, width * scale, height * scale)
    operator fun times(scale: Float): Rectangle = times(scale.toDouble())
    operator fun times(scale: Int): Rectangle = times(scale.toDouble())

    operator fun div(scale: Double): Rectangle = Rectangle(x / scale, y / scale, width / scale, height / scale)
    operator fun div(scale: Float): Rectangle = div(scale.toDouble())
    operator fun div(scale: Int): Rectangle = div(scale.toDouble())

    operator fun contains(that: Point): Boolean = contains(that.x, that.y)
    operator fun contains(that: Vector2F): Boolean = contains(that.x, that.y)
    operator fun contains(that: Vector2I): Boolean = contains(that.x, that.y)
    fun contains(x: Double, y: Double): Boolean = (x >= left && x < right) && (y >= top && y < bottom)
    fun contains(x: Float, y: Float): Boolean = contains(x.toDouble(), y.toDouble())
    fun contains(x: Int, y: Int): Boolean = contains(x.toDouble(), y.toDouble())

    override val area: Double get() = width * height
    override val perimeter: Double get() = (width + height) * 2

    override fun containsPoint(p: Point): Boolean = (p.x >= left && p.x < right) && (p.y >= top && p.y < bottom)
    override fun toVectorPath(): VectorPath = buildVectorPath { rect(this@RectangleD) }
    override fun distance(p: Point): Double {
        val p = p - center
        val b = Vector2D(width * 0.5, height * 0.5)
        val d = p.absoluteValue - b
        return max(d, Vector2D.ZERO).length + min(max(d.x, d.y), 0.0)
    }

    override fun normalVectorAt(p: Point): Vector2D {
        val pp = projectedPoint(p)
        val x = when (pp.x) {
            left -> -1.0
            right -> +1.0
            else -> 0.0
        }
        val y = when (pp.y) {
            top -> -1.0
            bottom -> +1.0
            else -> 0.0
        }
        return Point(x, y).normalized
    }

    override fun projectedPoint(p: Point): Point {
        val p0 = Line(topLeft, topRight).projectedPoint(p)
        val p1 = Line(topRight, bottomRight).projectedPoint(p)
        val p2 = Line(bottomRight, bottomLeft).projectedPoint(p)
        val p3 = Line(bottomLeft, topLeft).projectedPoint(p)
        val d0 = (p0 - p).lengthSquared
        val d1 = (p1 - p).lengthSquared
        val d2 = (p2 - p).lengthSquared
        val d3 = (p3 - p).lengthSquared
        val dmin = korlibs.math.min(d0, d1, d2, d3)
        return when (dmin) {
            d0 -> p0
            d1 -> p1
            d2 -> p2
            d3 -> p3
            else -> p0
        }

        //val px = p.x.clamp(left, right)
        //val py = p.y.clamp(top, bottom)
        //val distTop = (py - top).absoluteValue
        //val distBottom = (py - bottom).absoluteValue
        //val minDistY = min(distTop, distBottom)
        //val distLeft = (px - left).absoluteValue
        //val distRight = (px - right).absoluteValue
        //val minDistX = min(distLeft, distRight)
        //if (minDistX < minDistY) {
        //    return Point(if (distLeft < distRight) left else right, py)
        //} else {
        //    return Point(px, if (distTop < distBottom) top else bottom)
        //}
    }

    val isEmpty: Boolean get() = width == 0.0 && height == 0.0
    val isNotEmpty: Boolean get() = !isEmpty

    val left: Double get() = x
    val top: Double get() = y
    val right: Double get() = x + width
    val bottom: Double get() = y + height

    val topLeft: Point get() = Point(left, top)
    val topRight: Point get() = Point(right, top)
    val bottomLeft: Point get() = Point(left, bottom)
    val bottomRight: Point get() = Point(right, bottom)

    val centerX: Double get() = (right + left) * 0.5
    val centerY: Double get() = (bottom + top) * 0.5
    override val center: Point get() = Point(centerX, centerY)

    /**
     * Circle that touches or contains all the corners ([topLeft], [topRight], [bottomLeft], [bottomRight]) of the rectangle.
     */
    fun outerCircle(): Circle {
        val centerX = centerX
        val centerY = centerY
        return Circle(center, Point.distance(centerX, centerY, right, top))
    }

    fun without(padding: Margin): Rectangle = fromBounds(
        left + padding.left,
        top + padding.top,
        right - padding.right,
        bottom - padding.bottom
    )

    fun with(margin: Margin): Rectangle = fromBounds(
        left - margin.left,
        top - margin.top,
        right + margin.right,
        bottom + margin.bottom
    )

    infix fun intersects(that: Rectangle): Boolean = intersectsX(that) && intersectsY(that)
    infix fun intersectsX(that: Rectangle): Boolean = that.left <= this.right && that.right >= this.left
    infix fun intersectsY(that: Rectangle): Boolean = that.top <= this.bottom && that.bottom >= this.top

    infix fun intersectionOrNull(that: Rectangle): Rectangle? = if (this intersects that) Rectangle(
        max(this.left, that.left), max(this.top, that.top),
        min(this.right, that.right), min(this.bottom, that.bottom)
    ) else null

    infix fun intersection(that: Rectangle): Rectangle = if (this intersects that) Rectangle(
        max(this.left, that.left), max(this.top, that.top),
        min(this.right, that.right), min(this.bottom, that.bottom)
    ) else Rectangle.NIL

    fun toInt(): RectangleInt = RectangleInt(x.toInt(), y.toInt(), width.toInt(), height.toInt())
    fun toIntRound(): RectangleInt = RectangleInt(x.toIntRound(), y.toIntRound(), width.toIntRound(), height.toIntRound())
    fun toIntCeil(): RectangleInt = RectangleInt(x.toIntCeil(), y.toIntCeil(), width.toIntCeil(), height.toIntCeil())
    fun toIntFloor(): RectangleInt = RectangleInt(x.toIntFloor(), y.toIntFloor(), width.toIntFloor(), height.toIntFloor())

    fun getAnchoredPoint(anchor: Anchor): Point = Point(left + width * anchor.sx, top + height * anchor.sy)

    @Deprecated("")
    @KormaMutableApi fun toMRectangle(out: MRectangle = MRectangle()): MRectangle = out.setTo(x, y, width, height)

    fun expanded(border: MarginInt): Rectangle =
        fromBounds(left - border.left, top - border.top, right + border.right, bottom + border.bottom)

    fun copyBounds(left: Double = this.left, top: Double = this.top, right: Double = this.right, bottom: Double = this.bottom): Rectangle =
        Rectangle.fromBounds(left, top, right, bottom)

    fun translated(delta: Point): Rectangle = copy(x = this.x + delta.x, y = this.y + delta.y)

    fun transformed(m: Matrix): Rectangle {
        val tl = m.transform(topLeft)
        val tr = m.transform(topRight)
        val bl = m.transform(bottomLeft)
        val br = m.transform(bottomRight)
        val min = Point.minComponents(tl, tr, bl, br)
        val max = Point.maxComponents(tl, tr, bl, br)
        return Rectangle.fromBounds(min, max)
    }

    fun normalized(): Rectangle =
        Rectangle.fromBounds(Point.minComponents(topLeft, bottomRight), Point.maxComponents(topLeft, bottomRight))

    fun roundDecimalPlaces(places: Int): Rectangle = Rectangle(
        x.roundDecimalPlaces(places),
        y.roundDecimalPlaces(places),
        width.roundDecimalPlaces(places),
        height.roundDecimalPlaces(places)
    )

    fun rounded(): Rectangle = Rectangle(round(x), round(y), round(width), round(height))
    fun floored(): Rectangle = Rectangle(floor(x), floor(y), floor(width), floor(height))
    fun ceiled(): Rectangle = Rectangle(ceil(x), ceil(y), ceil(width), ceil(height))
}


fun Iterable<Rectangle>.bounds(): Rectangle {
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
    return Rectangle.fromBounds(left, top, right, bottom)
}

fun Rectangle.place(item: Size, anchor: Anchor, scale: ScaleMode): Rectangle {
    val outSize = scale(item, this.size)
    val p = (this.size - outSize) * anchor
    return Rectangle(p, outSize)
}

//fun RectangleInt.place(item: SizeInt, anchor: Anchor, scale: ScaleMode): RectangleInt {
//    val outSize = scale(item, this.size)
//    val p = (this.size - outSize) * anchor
//    return RectangleInt(p, outSize)
//}

typealias RectangleInt = RectangleI

//@KormaValueApi
data class RectangleI(
    val x: Int, val y: Int,
    val width: Int, val height: Int,
) {
    val float: Rectangle get() = Rectangle(x, y, width, height)

    val position: Vector2I get() = Vector2I(x, y)
    val size: SizeInt get() = SizeInt(width, height)

    val area: Int get() = width * height
    val isEmpty: Boolean get() = width == 0 && height == 0
    val isNotEmpty: Boolean get() = !isEmpty

    val left: Int get() = x
    val top: Int get() = y
    val right: Int get() = x + width
    val bottom: Int get() = y + height

    val topLeft: Vector2I get() = Vector2I(left, top)
    val topRight: Vector2I get() = Vector2I(right, top)
    val bottomLeft: Vector2I get() = Vector2I(left, bottom)
    val bottomRight: Vector2I get() = Vector2I(right, bottom)

    val centerX: Int get() = ((right + left) * 0.5f).toInt()
    val centerY: Int get() = ((bottom + top) * 0.5f).toInt()
    val center: Vector2I get() = Vector2I(centerX, centerY)

    fun toFloat(): Rectangle = Rectangle(position.toDouble(), size.toDouble())

    operator fun times(scale: Double): RectangleInt = RectangleInt(
        (x * scale).toInt(), (y * scale).toInt(),
        (width * scale).toInt(), (height * scale).toInt()
    )
    operator fun times(scale: Float): RectangleInt = this * scale.toDouble()
    operator fun times(scale: Int): RectangleInt = this * scale.toDouble()

    operator fun div(scale: Float): RectangleInt = RectangleInt(
        (x / scale).toInt(), (y / scale).toInt(),
        (width / scale).toInt(), (height / scale).toInt()
    )

    operator fun div(scale: Double): RectangleInt = this / scale.toFloat()
    operator fun div(scale: Int): RectangleInt = this / scale.toFloat()

    operator fun contains(that: Point): Boolean = contains(that.x, that.y)
    operator fun contains(that: Vector2I): Boolean = contains(that.x, that.y)
    fun contains(x: Float, y: Float): Boolean = (x >= left && x < right) && (y >= top && y < bottom)
    fun contains(x: Double, y: Double): Boolean = contains(x.toFloat(), y.toFloat())
    fun contains(x: Int, y: Int): Boolean = contains(x.toFloat(), y.toFloat())

    constructor() : this(Vector2I(), SizeInt())
    constructor(position: Vector2I, size: SizeInt) : this(position.x, position.y, size.width, size.height)

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
            kotlin.math.min(a.left, b.left),
            kotlin.math.min(a.top, b.top),
            kotlin.math.max(a.right, b.right),
            kotlin.math.max(a.bottom, b.bottom)
        )

        fun fromBounds(topLeft: Vector2I, bottomRight: Vector2I): RectangleInt = RectangleInt(topLeft, (bottomRight - topLeft).toSize())
        fun fromBounds(left: Int, top: Int, right: Int, bottom: Int): RectangleInt = fromBounds(Vector2I(left, top), Vector2I(right, bottom))
    }
}
