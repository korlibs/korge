package com.soywiz.korma.geom

import com.soywiz.korma.internal.*
import com.soywiz.korma.interpolation.*

interface IRectangle {
    val _x: Double
    val _y: Double
    val _width: Double
    val _height: Double

    companion object {
        inline operator fun invoke(x: Double, y: Double, width: Double, height: Double): IRectangle = Rectangle(x, y, width, height)
        inline operator fun invoke(x: Float, y: Float, width: Float, height: Float): IRectangle = Rectangle(x, y, width, height)
        inline operator fun invoke(x: Int, y: Int, width: Int, height: Int): IRectangle = Rectangle(x, y, width, height)
    }
}

val IRectangle.x get() = _x
val IRectangle.y get() = _y
val IRectangle.width get() = _width
val IRectangle.height get() = _height

val IRectangle.left get() = _x
val IRectangle.top get() = _y
val IRectangle.right get() = _x + _width
val IRectangle.bottom get() = _y + _height

data class Rectangle(
    var x: Double, var y: Double,
    var width: Double, var height: Double
) : MutableInterpolable<Rectangle>, Interpolable<Rectangle>, IRectangle, Sizeable {
    val topLeft get() = Point(left, top)
    val topRight get() = Point(right, top)
    val bottomLeft get() = Point(left, bottom)
    val bottomRight get() = Point(right, bottom)

    override val _x: Double get() = x
    override val _y: Double get() = y
    override val _width: Double get() = width
    override val _height: Double get() = height

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
    operator fun contains(that: Point) = contains(that.x, that.y)
    operator fun contains(that: IPoint) = contains(that.x, that.y)
    fun contains(x: Double, y: Double) = (x >= left && x < right) && (y >= top && y < bottom)
    fun contains(x: Float, y: Float) = contains(x.toDouble(), y.toDouble())
    fun contains(x: Int, y: Int) = contains(x.toDouble(), y.toDouble())

    infix fun intersects(that: Rectangle): Boolean = intersectsX(that) && intersectsY(that)

    infix fun intersectsX(that: Rectangle): Boolean = that.left <= this.right && that.right >= this.left
    infix fun intersectsY(that: Rectangle): Boolean = that.top <= this.bottom && that.bottom >= this.top

    fun setToIntersection(a: Rectangle, b: Rectangle) = this.apply { a.intersection(b, this) }

    infix fun intersection(that: Rectangle) = intersection(that, Rectangle())

    fun intersection(that: Rectangle, target: Rectangle = Rectangle()) = if (this intersects that) target.setBounds(
        kotlin.math.max(this.left, that.left), kotlin.math.max(this.top, that.top),
        kotlin.math.min(this.right, that.right), kotlin.math.min(this.bottom, that.bottom)
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

    fun inflate(dx: Double, dy: Double) {
        x -= dx; width += 2 * dx
        y -= dy; height += 2 * dy
    }
    fun inflate(dx: Float, dy: Float) = inflate(dx.toDouble(), dy.toDouble())
    fun inflate(dx: Int, dy: Int) = inflate(dx.toDouble(), dy.toDouble())

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

    fun getAnchoredPosition(anchor: Anchor, out: Point = Point()): Point =
        out.setTo(left + width * anchor.sx, top + height * anchor.sy)

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

inline fun Rectangle.setTo(x: Number, y: Number, width: Number, height: Number) =
    this.setTo(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

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

        fun fromBounds(left: Int, top: Int, right: Int, bottom: Int): RectangleInt =
            RectangleInt(left, top, right - left, bottom - top)
    }

    override fun toString(): String = "Rectangle(x=$x, y=$y, width=$width, height=$height)"
}

fun RectangleInt.setTo(that: RectangleInt) = setTo(that.x, that.y, that.width, that.height)

fun RectangleInt.setTo(x: Int, y: Int, width: Int, height: Int) = this.apply {
    this.x = x
    this.y = y
    this.width = width
    this.height = height
}

fun RectangleInt.setPosition(x: Int, y: Int) = this.apply { this.x = x; this.y = y }

fun RectangleInt.setSize(width: Int, height: Int) = this.apply {
    this.width = width
    this.height = height
}

fun RectangleInt.getPosition(out: PointInt = PointInt()): PointInt = out.setTo(x, y)
fun RectangleInt.getSize(out: SizeInt = SizeInt()): SizeInt = out.setTo(width, height)

val RectangleInt.position get() = getPosition()
val RectangleInt.size get() = getSize()

fun RectangleInt.setBoundsTo(left: Int, top: Int, right: Int, bottom: Int) = setTo(left, top, right - left, bottom - top)

////////////////////

operator fun IRectangleInt.contains(v: SizeInt): Boolean = (v.width <= width) && (v.height <= height)

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

val IRectangle.int get() = RectangleInt(_x, _y, _width, _height)
val IRectangleInt.float get() = Rectangle(x, y, width, height)

fun IRectangleInt.anchor(ax: Double, ay: Double): IPointInt =
    PointInt((x + width * ax).toInt(), (y + height * ay).toInt())

inline fun IRectangleInt.anchor(ax: Number, ay: Number): IPointInt = anchor(ax.toDouble(), ay.toDouble())

val IRectangleInt.center get() = anchor(0.5, 0.5)

///////////////////////////

fun Iterable<Rectangle>.bounds(target: Rectangle = Rectangle()): Rectangle {
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
            left = kotlin.math.min(left, r.left)
            right = kotlin.math.max(right, r.right)
            top = kotlin.math.min(top, r.top)
            bottom = kotlin.math.max(bottom, r.bottom)
        }
    }
    return target.setBounds(left, top, right, bottom)
}
