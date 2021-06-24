package com.soywiz.korma.geom

import com.soywiz.korma.internal.*
import com.soywiz.korma.interpolation.*
import kotlin.math.*

interface IRectangle {
    val x: Double
    val y: Double
    val width: Double
    val height: Double

    companion object {
        inline operator fun invoke(x: Double, y: Double, width: Double, height: Double): IRectangle = Rectangle(x, y, width, height)
        inline operator fun invoke(x: Float, y: Float, width: Float, height: Float): IRectangle = Rectangle(x, y, width, height)
        inline operator fun invoke(x: Int, y: Int, width: Int, height: Int): IRectangle = Rectangle(x, y, width, height)
    }
}

@Deprecated("Properties with underscores are deprecated and will be removed soon", ReplaceWith("x"), DeprecationLevel.ERROR)
val IRectangle._x get() = x
@Deprecated("Properties with underscores are deprecated and will be removed soon", ReplaceWith("y"), DeprecationLevel.ERROR)
val IRectangle._y get() = y
@Deprecated("Properties with underscores are deprecated and will be removed soon", ReplaceWith("width"), DeprecationLevel.ERROR)
val IRectangle._width get() = width
@Deprecated("Properties with underscores are deprecated and will be removed soon", ReplaceWith("height"), DeprecationLevel.ERROR)
val IRectangle._height get() = height

val IRectangle.left get() = x
val IRectangle.top get() = y
val IRectangle.right get() = x + width
val IRectangle.bottom get() = y + height

val IRectangle.topLeft get() = Point(left, top)
val IRectangle.topRight get() = Point(right, top)
val IRectangle.bottomLeft get() = Point(left, bottom)
val IRectangle.bottomRight get() = Point(right, bottom)

operator fun IRectangle.contains(that: IPoint) = contains(that.x, that.y)
operator fun IRectangle.contains(that: IPointInt) = contains(that.x, that.y)
fun IRectangle.contains(x: Double, y: Double) = (x >= left && x < right) && (y >= top && y < bottom)
fun IRectangle.contains(x: Float, y: Float) = contains(x.toDouble(), y.toDouble())
fun IRectangle.contains(x: Int, y: Int) = contains(x.toDouble(), y.toDouble())

data class Rectangle(
    override var x: Double, override var y: Double,
    override var width: Double, override var height: Double
) : MutableInterpolable<Rectangle>, Interpolable<Rectangle>, IRectangle, Sizeable {

    companion object {
        operator fun invoke(): Rectangle = Rectangle(0.0, 0.0, 0.0, 0.0)
        operator fun invoke(x: Int, y: Int, width: Int, height: Int): Rectangle = Rectangle(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
        operator fun invoke(x: Float, y: Float, width: Float, height: Float): Rectangle = Rectangle(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
        operator fun invoke(topLeft: IPoint, size: ISize): Rectangle = Rectangle(topLeft.x, topLeft.y, size.width, size.height)

        fun fromBounds(left: Double, top: Double, right: Double, bottom: Double): Rectangle = Rectangle().setBounds(left, top, right, bottom)
        fun fromBounds(left: Int, top: Int, right: Int, bottom: Int): Rectangle = Rectangle().setBounds(left, top, right, bottom)
        fun fromBounds(left: Float, top: Float, right: Float, bottom: Float): Rectangle = Rectangle().setBounds(left, top, right, bottom)
        fun fromBounds(topLeft: Point, bottomRight: Point): Rectangle = Rectangle().setBounds(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)

        fun isContainedIn(a: Rectangle, b: Rectangle): Boolean = a.x >= b.x && a.y >= b.y && a.x + a.width <= b.x + b.width && a.y + a.height <= b.y + b.height
    }

    val isEmpty: Boolean get() = area == 0.0
    val isNotEmpty: Boolean get() = area != 0.0
    val area: Double get() = width * height
    var left: Double; get() = x; set(value) = run { x = value }
    var top: Double; get() = y; set(value) = run { y = value }
    var right: Double; get() = x + width; set(value) = run { width = value - x }
    var bottom: Double; get() = y + height; set(value) = run { height = value - y }

    val position: Point get() = Point(x, y)
    override val size: Size get() = Size(width, height)

    fun setTo(x: Double, y: Double, width: Double, height: Double): Rectangle {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
        return this
    }
    fun setTo(x: Int, y: Int, width: Int, height: Int): Rectangle = setTo(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
    fun setTo(x: Float, y: Float, width: Float, height: Float): Rectangle = setTo(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

    fun copyFrom(that: Rectangle) = setTo(that.x, that.y, that.width, that.height)

    fun setBounds(left: Double, top: Double, right: Double, bottom: Double) = setTo(left, top, right - left, bottom - top)
    fun setBounds(left: Int, top: Int, right: Int, bottom: Int) = setBounds(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())
    fun setBounds(left: Float, top: Float, right: Float, bottom: Float) = setBounds(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())

    operator fun times(scale: Double) = Rectangle(x * scale, y * scale, width * scale, height * scale)
    operator fun times(scale: Float) = this * scale.toDouble()
    operator fun times(scale: Int) = this * scale.toDouble()

    operator fun div(scale: Double) = Rectangle(x / scale, y / scale, width / scale, height / scale)
    operator fun div(scale: Float) = this / scale.toDouble()
    operator fun div(scale: Int) = this / scale.toDouble()

    operator fun contains(that: Rectangle) = isContainedIn(that, this)

    infix fun intersects(that: Rectangle): Boolean = intersectsX(that) && intersectsY(that)

    infix fun intersectsX(that: Rectangle): Boolean = that.left <= this.right && that.right >= this.left
    infix fun intersectsY(that: Rectangle): Boolean = that.top <= this.bottom && that.bottom >= this.top

    fun setToIntersection(a: Rectangle, b: Rectangle): Rectangle? {
        return if (a.intersection(b, this) != null) this else null
    }

    infix fun intersection(that: Rectangle) = intersection(that, Rectangle())

    fun intersection(that: Rectangle, target: Rectangle = Rectangle()) = if (this intersects that) target.setBounds(
        max(this.left, that.left), max(this.top, that.top),
        min(this.right, that.right), min(this.bottom, that.bottom)
    ) else null

    fun displaced(dx: Double, dy: Double) = Rectangle(this.x + dx, this.y + dy, width, height)
    fun displaced(dx: Float, dy: Float) = displaced(dx.toDouble(), dy.toDouble())
    fun displaced(dx: Int, dy: Int) = displaced(dx.toDouble(), dy.toDouble())

    fun displace(dx: Double, dy: Double) = setTo(this.x + dx, this.y + dy, this.width, this.height)
    fun displace(dx: Float, dy: Float) = displace(dx.toDouble(), dy.toDouble())
    fun displace(dx: Int, dy: Int) = displace(dx.toDouble(), dy.toDouble())

    fun place(item: Size, anchor: Anchor, scale: ScaleMode, out: Rectangle = Rectangle()): Rectangle =
        place(item.width, item.height, anchor, scale, out)

    fun place(width: Double, height: Double, anchor: Anchor, scale: ScaleMode, out: Rectangle = Rectangle()): Rectangle {
        val ow = scale.transformW(width, height, this.width, this.height)
        val oh = scale.transformH(width, height, this.width, this.height)
        val x = (this.width - ow) * anchor.sx
        val y = (this.height - oh) * anchor.sy
        return out.setTo(x, y, ow, oh)
    }
    
    fun inflate(left: Double, top: Double = left, right: Double = left, bottom: Double = top): Rectangle = setBounds(this.left - left, this.top - top, this.right + right, this.bottom + bottom)
    inline fun inflate(left: Number, top: Number = left, right: Number = left, bottom: Number = top): Rectangle = inflate(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())

    fun clear() = setTo(0.0, 0.0, 0.0, 0.0)

    fun clone() = Rectangle(x, y, width, height)

    fun setToAnchoredRectangle(item: Rectangle, anchor: Anchor, container: Rectangle) = setToAnchoredRectangle(item.size, anchor, container)

    fun setToAnchoredRectangle(item: Size, anchor: Anchor, container: Rectangle) = setTo(
        container.x + anchor.sx * (container.width - item.width),
        container.y + anchor.sy * (container.height - item.height),
        item.width,
        item.height
    )

    //override fun toString(): String = "Rectangle([${left.niceStr}, ${top.niceStr}]-[${right.niceStr}, ${bottom.niceStr}])"
    override fun toString(): String = "Rectangle(x=${x.niceStr}, y=${y.niceStr}, width=${width.niceStr}, height=${height.niceStr})"
    fun toStringBounds(): String = "Rectangle([${left.niceStr},${top.niceStr}]-[${right.niceStr},${bottom.niceStr}])"
    fun toStringSize(): String = "Rectangle([${left.niceStr},${top.niceStr}],[${width.niceStr},${height.niceStr}])"
    fun toStringCompat(): String = "Rectangle(x=${left.niceStr}, y=${top.niceStr}, w=${width.niceStr}, h=${height.niceStr})"

    override fun interpolateWith(ratio: Double, other: Rectangle): Rectangle =
        Rectangle().setToInterpolated(ratio, this, other)

    override fun setToInterpolated(ratio: Double, l: Rectangle, r: Rectangle): Rectangle = this.setTo(
        ratio.interpolate(l.x, r.x),
        ratio.interpolate(l.y, r.y),
        ratio.interpolate(l.width, r.width),
        ratio.interpolate(l.height, r.height)
    )

    fun getAnchoredPosition(anchor: Anchor, out: Point = Point()): Point = getAnchoredPosition(anchor.sx, anchor.sy, out)

    fun getAnchoredPosition(anchorX: Double, anchorY: Double, out: Point = Point()): Point =
        out.setTo(left + width * anchorX, top + height * anchorY)

    fun toInt() = RectangleInt(x, y, width, height)
    fun floor(): Rectangle {
        setTo(kotlin.math.floor(x), kotlin.math.floor(y), kotlin.math.floor(width), kotlin.math.floor(height))
        return this
    }
    fun round(): Rectangle {
        setTo(kotlin.math.round(x), kotlin.math.round(y), kotlin.math.round(width), kotlin.math.round(height))
        return this
    }
    fun ceil(): Rectangle {
        setTo(kotlin.math.ceil(x), kotlin.math.ceil(y), kotlin.math.ceil(width), kotlin.math.ceil(height))
        return this
    }
}

@Deprecated("Use non-mixed Int or Double variants for now")
inline fun Rectangle.setTo(x: Number, y: Number, width: Number, height: Number) =
    this.setTo(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

@Deprecated("Use non-mixed Int or Double variants for now")
inline fun Rectangle.setBounds(left: Number, top: Number, right: Number, bottom: Number) = setBounds(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())

//////////// INT

interface IRectangleInt {
    val x: Int
    val y: Int
    val width: Int
    val height: Int

    companion object {
        operator fun invoke(x: Int, y: Int, width: Int, height: Int): IRectangleInt = RectangleInt(x, y, width, height)
        operator fun invoke(x: Float, y: Float, width: Float, height: Float): IRectangleInt = RectangleInt(x.toInt(), y.toInt(), width.toInt(), height.toInt())
        operator fun invoke(x: Double, y: Double, width: Double, height: Double): IRectangleInt = RectangleInt(x.toInt(), y.toInt(), width.toInt(), height.toInt())
    }
}

val IRectangleInt.left get() = x
val IRectangleInt.top get() = y
val IRectangleInt.right get() = x + width
val IRectangleInt.bottom get() = y + height

val IRectangleInt.topLeft get() = PointInt(left, top)
val IRectangleInt.topRight get() = PointInt(right, top)
val IRectangleInt.bottomLeft get() = PointInt(left, bottom)
val IRectangleInt.bottomRight get() = PointInt(right, bottom)

inline class RectangleInt(val rect: Rectangle) : IRectangleInt {
    override var x: Int
        set(value) = run { rect.x = value.toDouble() }
        get() = rect.x.toInt()

    override var y: Int
        set(value) = run { rect.y = value.toDouble() }
        get() = rect.y.toInt()

    override var width: Int
        set(value) = run { rect.width = value.toDouble() }
        get() = rect.width.toInt()

    override var height: Int
        set(value) = run { rect.height = value.toDouble() }
        get() = rect.height.toInt()

    var left: Int
        set(value) = run { rect.left = value.toDouble() }
        get() = rect.left.toInt()

    var top: Int
        set(value) = run { rect.top = value.toDouble() }
        get() = rect.top.toInt()

    var right: Int
        set(value) = run { rect.right = value.toDouble() }
        get() = rect.right.toInt()

    var bottom: Int
        set(value) = run { rect.bottom = value.toDouble() }
        get() = rect.bottom.toInt()

    companion object {
        operator fun invoke() = RectangleInt(Rectangle())
        operator fun invoke(x: Int, y: Int, width: Int, height: Int) = RectangleInt(Rectangle(x, y, width, height))
        operator fun invoke(x: Float, y: Float, width: Float, height: Float) = RectangleInt(Rectangle(x, y, width, height))
        operator fun invoke(x: Double, y: Double, width: Double, height: Double) = RectangleInt(Rectangle(x, y, width, height))
        operator fun invoke(other: IRectangleInt) = RectangleInt(Rectangle(other.x, other.y, other.width, other.height))

        fun fromBounds(left: Int, top: Int, right: Int, bottom: Int): RectangleInt =
            RectangleInt(left, top, right - left, bottom - top)
    }

    override fun toString(): String = "Rectangle(x=$x, y=$y, width=$width, height=$height)"
}

fun RectangleInt.setTo(that: IRectangleInt) = setTo(that.x, that.y, that.width, that.height)

fun RectangleInt.setTo(x: Int, y: Int, width: Int, height: Int): RectangleInt {
    this.x = x
    this.y = y
    this.width = width
    this.height = height

    return this
}

fun RectangleInt.setPosition(x: Int, y: Int):RectangleInt {
    this.x = x
    this.y = y

    return this
}

fun RectangleInt.setSize(width: Int, height: Int): RectangleInt {
    this.width = width
    this.height = height

    return this
}

fun RectangleInt.getPosition(out: PointInt = PointInt()): PointInt = out.setTo(x, y)
fun RectangleInt.getSize(out: SizeInt = SizeInt()): SizeInt = out.setTo(width, height)

val RectangleInt.position get() = getPosition()
val RectangleInt.size get() = getSize()

fun RectangleInt.setBoundsTo(left: Int, top: Int, right: Int, bottom: Int) = setTo(left, top, right - left, bottom - top)

////////////////////

operator fun IRectangleInt.contains(v: SizeInt): Boolean = (v.width <= width) && (v.height <= height)
operator fun IRectangleInt.contains(that: IPoint) = contains(that.x, that.y)
operator fun IRectangleInt.contains(that: IPointInt) = contains(that.x, that.y)
fun IRectangleInt.contains(x: Double, y: Double) = (x >= left && x < right) && (y >= top && y < bottom)
fun IRectangleInt.contains(x: Float, y: Float) = contains(x.toDouble(), y.toDouble())
fun IRectangleInt.contains(x: Int, y: Int) = contains(x.toDouble(), y.toDouble())

fun IRectangleInt.anchoredIn(container: RectangleInt, anchor: Anchor, out: RectangleInt = RectangleInt()): RectangleInt =
    out.setTo(
        ((container.width - this.width) * anchor.sx).toInt(),
        ((container.height - this.height) * anchor.sy).toInt(),
        width,
        height
    )

fun IRectangleInt.getAnchorPosition(anchor: Anchor, out: PointInt = PointInt()): PointInt =
    out.setTo((x + width * anchor.sx).toInt(), (y + height * anchor.sy).toInt())

fun Rectangle.asInt() = RectangleInt(this)
fun RectangleInt.asDouble() = this.rect

val IRectangle.int get() = RectangleInt(x, y, width, height)
val IRectangleInt.float get() = Rectangle(x, y, width, height)

fun IRectangleInt.anchor(ax: Double, ay: Double): PointInt =
    PointInt((x + width * ax).toInt(), (y + height * ay).toInt())

inline fun IRectangleInt.anchor(ax: Number, ay: Number): PointInt = anchor(ax.toDouble(), ay.toDouble())

val IRectangleInt.center get() = anchor(0.5, 0.5)

///////////////////////////

fun Iterable<IRectangle>.bounds(target: Rectangle = Rectangle()): Rectangle {
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

fun Rectangle.without(padding: Margin): Rectangle =
    Rectangle.fromBounds(left + padding.left, top + padding.top, right - padding.right, bottom - padding.bottom)

fun Rectangle.with(margin: Margin): Rectangle =
    Rectangle.fromBounds(left - margin.left, top - margin.top, right + margin.right, bottom + margin.bottom)
