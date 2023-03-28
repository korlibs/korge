package korlibs.math.geom

import korlibs.memory.*
import korlibs.memory.pack.*
import korlibs.math.annotations.*
import korlibs.math.internal.*
import korlibs.math.math.*
import kotlin.math.*

//@KormaValueApi
inline class Rectangle(val data: Float4Pack) {
    val int: RectangleInt get() = toInt()

    @Deprecated("", ReplaceWith("this")) fun clone(): Rectangle = this
    @Deprecated("", ReplaceWith("this")) val immutable: Rectangle get() = this

    val position: Point get() = Point(data.f0, data.f1)
    val size: Size get() = Size(data.f2, data.f3)

    val x: Float get() = data.f0
    val y: Float get() = data.f1
    val width: Float get() = data.f2
    val height: Float get() = data.f3

    val xD: Double get() = x.toDouble()
    val yD: Double get() = y.toDouble()
    val widthD: Double get() = width.toDouble()
    val heightD: Double get() = height.toDouble()

    val isZero: Boolean get() = this == ZERO
    val isInfinite: Boolean get() = this == INFINITE
    //val isNaN: Boolean get() = this == NaN
    val isNaN: Boolean get() = this.x.isNaN()
    val isNIL: Boolean get() = isNaN
    val isNotNIL: Boolean get() = !isNIL

    fun isAlmostEquals(other: Rectangle, epsilon: Float = 0.01f): Boolean =
        this.x.isAlmostEquals(other.x, epsilon) &&
            this.y.isAlmostEquals(other.y, epsilon) &&
            this.width.isAlmostEquals(other.width, epsilon) &&
            this.height.isAlmostEquals(other.height, epsilon)

    fun toStringBounds(): String = "Rectangle([${left.niceStr},${top.niceStr}]-[${right.niceStr},${bottom.niceStr}])"
    fun toStringSize(): String = "Rectangle([${left.niceStr},${top.niceStr}],[${width.niceStr},${height.niceStr}])"
    fun toStringCompat(): String = "Rectangle(x=${left.niceStr}, y=${top.niceStr}, w=${width.niceStr}, h=${height.niceStr})"

    override fun toString(): String = when {
        isNIL -> "null"
        else -> "Rectangle(x=${x.niceStr}, y=${y.niceStr}, width=${width.niceStr}, height=${height.niceStr})"
    }

    companion object {
        val ZERO = Rectangle(0, 0, 0, 0)
        val INFINITE = Rectangle(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        val NaN = Rectangle(Float.NaN, Float.NaN, Float.NaN, Float.NaN)
        val NIL get() = NaN

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
    operator fun contains(that: Vector2Int): Boolean = contains(that.x, that.y)
    fun contains(x: Float, y: Float): Boolean = (x >= left && x < right) && (y >= top && y < bottom)
    fun contains(x: Double, y: Double): Boolean = contains(x.toFloat(), y.toFloat())
    fun contains(x: Int, y: Int): Boolean = contains(x.toFloat(), y.toFloat())

    val area: Float get() = width * height
    val isEmpty: Boolean get() = width == 0f && height == 0f
    val isNotEmpty: Boolean get() = !isEmpty
    @Deprecated("")
    val mutable: MRectangle get() = MRectangle(x, y, width, height)
    @Deprecated("")
    fun mutable(out: MRectangle = MRectangle()): MRectangle = out.copyFrom(this)

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

    fun copy(x: Float = this.x, y: Float = this.y, width: Float = this.width, height: Float = this.height): Rectangle =
        Rectangle(x, y, width, height)

    fun copyBounds(left: Float = this.left, top: Float = this.top, right: Float = this.right, bottom: Float = this.bottom): Rectangle =
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
