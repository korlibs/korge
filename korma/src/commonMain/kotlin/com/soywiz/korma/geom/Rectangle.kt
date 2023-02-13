package com.soywiz.korma.geom

import com.soywiz.kds.*
import com.soywiz.korma.internal.niceStr
import com.soywiz.korma.interpolation.Interpolable
import com.soywiz.korma.interpolation.MutableInterpolable
import com.soywiz.korma.interpolation.interpolate
import com.soywiz.korma.math.isAlmostEquals
import com.soywiz.korma.math.roundDecimalPlaces
import kotlin.math.max
import kotlin.math.min

// @TODO: Value Class
data class Rectangle(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
)

@Deprecated("Use Rectangle instead")
interface IRectangle {
    val x: Double
    val y: Double
    val width: Double
    val height: Double

    companion object {
        inline operator fun invoke(
            x: Double,
            y: Double,
            width: Double,
            height: Double
        ): IRectangle = MRectangle(x, y, width, height)

        inline operator fun invoke(x: Float, y: Float, width: Float, height: Float): IRectangle =
            MRectangle(x, y, width, height)

        inline operator fun invoke(x: Int, y: Int, width: Int, height: Int): IRectangle =
            MRectangle(x, y, width, height)

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

val IRectangle.area: Double get() = width * height
fun IRectangle.clone(): MRectangle = MRectangle(x, y, width, height)
val IRectangle.isNotEmpty: Boolean get() = width != 0.0 || height != 0.0
val IRectangle.mutable: MRectangle get() = MRectangle(x, y, width, height)

val IRectangle.left get() = x
val IRectangle.top get() = y
val IRectangle.right get() = x + width
val IRectangle.bottom get() = y + height

val IRectangle.topLeft get() = MPoint(left, top)
val IRectangle.topRight get() = MPoint(right, top)
val IRectangle.bottomLeft get() = MPoint(left, bottom)
val IRectangle.bottomRight get() = MPoint(right, bottom)

val IRectangle.centerX: Double get() = (right + left) * 0.5
val IRectangle.centerY: Double get() = (bottom + top) * 0.5
val IRectangle.center: MPoint get() = MPoint(centerX, centerY)

/**
 * Circle that touches or contains all the corners ([topLeft], [topRight], [bottomLeft], [bottomRight]) of the rectangle.
 */
fun IRectangle.outerCircle(): Circle {
    val centerX = centerX
    val centerY = centerY
    return Circle(center, MPoint.distance(centerX, centerY, right, top))
}

operator fun IRectangle.contains(that: IPoint) = contains(that.x, that.y)
operator fun IRectangle.contains(that: IPointInt) = contains(that.x, that.y)
fun IRectangle.contains(x: Double, y: Double) = (x >= left && x < right) && (y >= top && y < bottom)
fun IRectangle.contains(x: Float, y: Float) = contains(x.toDouble(), y.toDouble())
fun IRectangle.contains(x: Int, y: Int) = contains(x.toDouble(), y.toDouble())

@Deprecated("Use Rectangle instead")
data class MRectangle(
    override var x: Double, override var y: Double,
    override var width: Double, override var height: Double
) : MutableInterpolable<MRectangle>, Interpolable<MRectangle>, IRectangle, Sizeable {

    companion object {
        val POOL: ConcurrentPool<MRectangle> = ConcurrentPool<MRectangle>({ it.clear() }) { MRectangle() }

        operator fun invoke(): MRectangle = MRectangle(0.0, 0.0, 0.0, 0.0)
        operator fun invoke(x: Int, y: Int, width: Int, height: Int): MRectangle =
            MRectangle(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

        operator fun invoke(x: Float, y: Float, width: Float, height: Float): MRectangle =
            MRectangle(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

        operator fun invoke(topLeft: IPoint, size: ISize): MRectangle =
            MRectangle(topLeft.x, topLeft.y, size.width, size.height)

        fun fromBounds(left: Double, top: Double, right: Double, bottom: Double): MRectangle =
            MRectangle().setBounds(left, top, right, bottom)

        fun fromBounds(left: Int, top: Int, right: Int, bottom: Int): MRectangle =
            MRectangle().setBounds(left, top, right, bottom)

        fun fromBounds(left: Float, top: Float, right: Float, bottom: Float): MRectangle =
            MRectangle().setBounds(left, top, right, bottom)

        fun fromBounds(point1: IPoint, point2: IPoint): MRectangle =
            IRectangle(point1, point2)

        fun isContainedIn(a: MRectangle, b: MRectangle): Boolean =
            a.x >= b.x && a.y >= b.y && a.x + a.width <= b.x + b.width && a.y + a.height <= b.y + b.height
    }

    fun setXY(x: Double, y: Double) {
        this.x = x
        this.y = y
    }

    val isEmpty: Boolean get() = area == 0.0
    val isNotEmpty: Boolean get() = area != 0.0
    val area: Double get() = width * height
    @Deprecated("Use x or leftKeepingRight")
    var left: Double
        get() = x
        set(value) {
            x = value
        }
    @Deprecated("Use y or topKeepingBottom")
    var top: Double
        get() = y
        set(value) {
            y = value
        }

    var leftKeepingRight: Double
        get() = x
        set(value) {
            width += (x - value)
            x = value
        }
    var topKeepingBottom: Double
        get() = y
        set(value) {
            height += (y - value)
            y = value
        }

    var right: Double
        get() = x + width
        set(value) {
            width = value - x
        }
    var bottom: Double
        get() = y + height
        set(value) {
            height = value - y
        }

    val position: MPoint get() = MPoint(x, y)
    override val size: MSize get() = MSize(width, height)

    fun setToBounds(left: Double, top: Double, right: Double, bottom: Double): MRectangle =
        setTo(left, top, right - left, bottom - top)

    fun setTo(x: Double, y: Double, width: Double, height: Double): MRectangle {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
        return this
    }

    fun setTo(x: Int, y: Int, width: Int, height: Int): MRectangle =
        setTo(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

    fun setTo(x: Float, y: Float, width: Float, height: Float): MRectangle =
        setTo(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

    fun copyFrom(that: IRectangle) = setTo(that.x, that.y, that.width, that.height)

    fun setBounds(left: Double, top: Double, right: Double, bottom: Double) =
        setTo(left, top, right - left, bottom - top)

    fun setBounds(left: Int, top: Int, right: Int, bottom: Int) =
        setBounds(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())

    fun setBounds(left: Float, top: Float, right: Float, bottom: Float) =
        setBounds(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())

    operator fun times(scale: Double) =
        MRectangle(x * scale, y * scale, width * scale, height * scale)

    operator fun times(scale: Float) = this * scale.toDouble()
    operator fun times(scale: Int) = this * scale.toDouble()

    operator fun div(scale: Double) = MRectangle(x / scale, y / scale, width / scale, height / scale)
    operator fun div(scale: Float) = this / scale.toDouble()
    operator fun div(scale: Int) = this / scale.toDouble()

    operator fun contains(that: MRectangle) = isContainedIn(that, this)

    infix fun intersects(that: MRectangle): Boolean = intersectsX(that) && intersectsY(that)

    infix fun intersectsX(that: MRectangle): Boolean =
        that.left <= this.right && that.right >= this.left

    infix fun intersectsY(that: MRectangle): Boolean =
        that.top <= this.bottom && that.bottom >= this.top

    fun setToIntersection(a: MRectangle, b: MRectangle): MRectangle? {
        return if (a.intersection(b, this) != null) this else null
    }

    fun setToUnion(a: MRectangle, b: MRectangle): MRectangle =
        setToBounds(
            min(a.left, b.left),
            min(a.top, b.top),
            max(a.right, b.right),
            max(a.bottom, b.bottom)
        )

    infix fun intersection(that: MRectangle) = intersection(that, MRectangle())

    fun intersection(that: MRectangle, target: MRectangle = MRectangle()) =
        if (this intersects that) target.setBounds(
            max(this.left, that.left), max(this.top, that.top),
            min(this.right, that.right), min(this.bottom, that.bottom)
        ) else null

    fun displaced(dx: Double, dy: Double) = MRectangle(this.x + dx, this.y + dy, width, height)
    fun displaced(dx: Float, dy: Float) = displaced(dx.toDouble(), dy.toDouble())
    fun displaced(dx: Int, dy: Int) = displaced(dx.toDouble(), dy.toDouble())

    fun displace(dx: Double, dy: Double) = setTo(this.x + dx, this.y + dy, this.width, this.height)
    fun displace(dx: Float, dy: Float) = displace(dx.toDouble(), dy.toDouble())
    fun displace(dx: Int, dy: Int) = displace(dx.toDouble(), dy.toDouble())

    fun place(
        item: MSize,
        anchor: Anchor,
        scale: ScaleMode,
        out: MRectangle = MRectangle()
    ): MRectangle =
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
    ): MRectangle =
        setBounds(this.left - left, this.top - top, this.right + right, this.bottom + bottom)

    inline fun inflate(
        left: Number,
        top: Number = left,
        right: Number = left,
        bottom: Number = top
    ): MRectangle = inflate(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())

    fun clear() = setTo(0.0, 0.0, 0.0, 0.0)

    fun clone() = MRectangle(x, y, width, height)

    fun setToAnchoredRectangle(item: MRectangle, anchor: Anchor, container: MRectangle) =
        setToAnchoredRectangle(item.size, anchor, container)

    fun setToAnchoredRectangle(item: MSize, anchor: Anchor, container: MRectangle) = setTo(
        container.x + anchor.sx * (container.width - item.width),
        container.y + anchor.sy * (container.height - item.height),
        item.width,
        item.height
    )

    //override fun toString(): String = "Rectangle([${left.niceStr}, ${top.niceStr}]-[${right.niceStr}, ${bottom.niceStr}])"
    override fun toString(): String =
        "Rectangle(x=${x.niceStr}, y=${y.niceStr}, width=${width.niceStr}, height=${height.niceStr})"

    fun toStringBounds(): String =
        "Rectangle([${left.niceStr},${top.niceStr}]-[${right.niceStr},${bottom.niceStr}])"

    fun toStringSize(): String =
        "Rectangle([${left.niceStr},${top.niceStr}],[${width.niceStr},${height.niceStr}])"

    fun toStringCompat(): String =
        "Rectangle(x=${left.niceStr}, y=${top.niceStr}, w=${width.niceStr}, h=${height.niceStr})"

    override fun equals(other: Any?): Boolean = other is MRectangle
        && x.isAlmostEquals(other.x)
        && y.isAlmostEquals(other.y)
        && width.isAlmostEquals(other.width)
        && height.isAlmostEquals(other.height)

    override fun interpolateWith(ratio: Double, other: MRectangle): MRectangle =
        MRectangle().setToInterpolated(ratio, this, other)

    override fun setToInterpolated(ratio: Double, l: MRectangle, r: MRectangle): MRectangle =
        this.setTo(
            ratio.interpolate(l.x, r.x),
            ratio.interpolate(l.y, r.y),
            ratio.interpolate(l.width, r.width),
            ratio.interpolate(l.height, r.height)
        )

    fun getMiddlePoint(out: MPoint = MPoint()): MPoint = getAnchoredPosition(Anchor.CENTER, out)

    fun getAnchoredPosition(anchor: Anchor, out: MPoint = MPoint()): MPoint =
        getAnchoredPosition(anchor.sx, anchor.sy, out)

    fun getAnchoredPosition(anchorX: Double, anchorY: Double, out: MPoint = MPoint()): MPoint =
        out.setTo(left + width * anchorX, top + height * anchorY)

    fun toInt(): RectangleInt = RectangleInt(x.toInt(), y.toInt(), width.toInt(), height.toInt())
    fun floor(): MRectangle {
        setTo(
            kotlin.math.floor(x),
            kotlin.math.floor(y),
            kotlin.math.floor(width),
            kotlin.math.floor(height)
        )
        return this
    }

    fun round(): MRectangle {
        setTo(
            kotlin.math.round(x),
            kotlin.math.round(y),
            kotlin.math.round(width),
            kotlin.math.round(height)
        )
        return this
    }

    fun roundDecimalPlaces(places: Int): MRectangle {
        setTo(
            x.roundDecimalPlaces(places),
            y.roundDecimalPlaces(places),
            width.roundDecimalPlaces(places),
            height.roundDecimalPlaces(places)
        )
        return this
    }

    fun ceil(): MRectangle {
        setTo(
            kotlin.math.ceil(x),
            kotlin.math.ceil(y),
            kotlin.math.ceil(width),
            kotlin.math.ceil(height)
        )
        return this
    }

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
}

fun MRectangle.expand(left: Double, top: Double, right: Double, bottom: Double): MRectangle {
    this.left -= left
    this.top -= top
    this.width += left + right
    this.height += top + bottom
    return this
}

fun MRectangle.expand(left: Int, top: Int, right: Int, bottom: Int): MRectangle =
    expand(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())

fun MRectangle.expand(margin: IMargin): MRectangle =
    expand(margin.left, margin.top, margin.right, margin.bottom)

fun MRectangle.expand(margin: IMarginInt): MRectangle =
    expand(margin.left, margin.top, margin.right, margin.bottom)

@Deprecated("Use non-mixed Int or Double variants for now")
inline fun MRectangle.setTo(x: Number, y: Number, width: Number, height: Number) =
    this.setTo(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

//////////// INT

interface IRectangleInt {
    val x: Int
    val y: Int
    val width: Int
    val height: Int

    companion object {
        operator fun invoke(x: Int, y: Int, width: Int, height: Int): IRectangleInt =
            RectangleInt(x, y, width, height)

        operator fun invoke(x: Float, y: Float, width: Float, height: Float): IRectangleInt =
            RectangleInt(x.toInt(), y.toInt(), width.toInt(), height.toInt())

        operator fun invoke(x: Double, y: Double, width: Double, height: Double): IRectangleInt =
            RectangleInt(x.toInt(), y.toInt(), width.toInt(), height.toInt())
    }
}

fun IRectangleInt.clone(): RectangleInt = RectangleInt(x, y, width, height)

fun IRectangleInt.expanded(border: com.soywiz.korma.geom.IMarginInt): IRectangleInt {
    return clone().expand(border)
}

/** Inline expand the rectangle */
fun RectangleInt.expand(border: com.soywiz.korma.geom.IMarginInt): RectangleInt {
    return this.setBoundsTo(left - border.left, top - border.top, right + border.right, bottom + border.bottom)
}

fun IRectangleInt.sliceWithBounds(left: Int, top: Int, right: Int, bottom: Int, clamped: Boolean = true): IRectangleInt {
    val left = if (!clamped) left else left.coerceIn(0, this.width)
    val right = if (!clamped) right else right.coerceIn(0, this.width)
    val top = if (!clamped) top else top.coerceIn(0, this.height)
    val bottom = if (!clamped) bottom else bottom.coerceIn(0, this.height)
    return RectangleInt.fromBounds(this.x + left, this.y + top, this.x + right, this.y + bottom)
}

fun IRectangleInt.sliceWithSize(x: Int, y: Int, width: Int, height: Int, clamped: Boolean = true): IRectangleInt =
    sliceWithBounds(x, y, x + width, y + height, clamped)

val IRectangleInt.left: Int get() = x
val IRectangleInt.top: Int get() = y
val IRectangleInt.right: Int get() = x + width
val IRectangleInt.bottom: Int get() = y + height
val IRectangleInt.area: Int get() = width * height

val IRectangleInt.topLeft: MPointInt get() = MPointInt(left, top)
val IRectangleInt.topRight: MPointInt get() = MPointInt(right, top)
val IRectangleInt.bottomLeft: MPointInt get() = MPointInt(left, bottom)
val IRectangleInt.bottomRight: MPointInt get() = MPointInt(right, bottom)

inline class RectangleInt(val rect: MRectangle) : IRectangleInt {
    override var x: Int
        set(value) {
            rect.x = value.toDouble()
        }
        get() = rect.x.toInt()

    override var y: Int
        set(value) {
            rect.y = value.toDouble()
        }
        get() = rect.y.toInt()

    override var width: Int
        set(value) {
            rect.width = value.toDouble()
        }
        get() = rect.width.toInt()

    override var height: Int
        set(value) {
            rect.height = value.toDouble()
        }
        get() = rect.height.toInt()

    var left: Int
        set(value) {
            rect.left = value.toDouble()
        }
        get() = rect.left.toInt()

    var top: Int
        set(value) {
            rect.top = value.toDouble()
        }
        get() = rect.top.toInt()

    var right: Int
        set(value) {
            rect.right = value.toDouble()
        }
        get() = rect.right.toInt()

    var bottom: Int
        set(value) {
            rect.bottom = value.toDouble()
        }
        get() = rect.bottom.toInt()

    companion object {
        operator fun invoke() = RectangleInt(MRectangle())
        operator fun invoke(x: Int, y: Int, width: Int, height: Int) =
            RectangleInt(MRectangle(x, y, width, height))

        operator fun invoke(x: Float, y: Float, width: Float, height: Float) =
            RectangleInt(MRectangle(x, y, width, height))

        operator fun invoke(x: Double, y: Double, width: Double, height: Double) =
            RectangleInt(MRectangle(x, y, width, height))

        operator fun invoke(other: IRectangleInt) =
            RectangleInt(MRectangle(other.x, other.y, other.width, other.height))

        fun fromBounds(left: Int, top: Int, right: Int, bottom: Int): RectangleInt =
            RectangleInt(left, top, right - left, bottom - top)
    }

    override fun toString(): String = "Rectangle(x=$x, y=$y, width=$width, height=$height)"
    fun toStringBounds(): String = "Rectangle([$left,$top]-[$right,$bottom])"
    fun copyFrom(rect: IRectangleInt): RectangleInt {
        setTo(rect.x, rect.y, rect.width, rect.height)
        return this
    }

    fun setToUnion(a: IRectangleInt, b: IRectangleInt) {
        setToBounds(
            min(a.left, b.left),
            min(a.top, b.top),
            max(a.right, b.right),
            max(a.bottom, b.bottom)
        )
    }
}

fun RectangleInt.setTo(that: IRectangleInt) = setTo(that.x, that.y, that.width, that.height)

fun RectangleInt.setTo(x: Int, y: Int, width: Int, height: Int): RectangleInt {
    this.x = x
    this.y = y
    this.width = width
    this.height = height
    return this
}
fun RectangleInt.setToBounds(left: Int, top: Int, right: Int, bottom: Int): RectangleInt = setTo(left, top, right - left, bottom - top)

fun RectangleInt.setPosition(x: Int, y: Int): RectangleInt {
    this.x = x
    this.y = y
    return this
}

fun RectangleInt.setSize(width: Int, height: Int): RectangleInt {
    this.width = width
    this.height = height

    return this
}

fun RectangleInt.getPosition(out: MPointInt = MPointInt()): MPointInt = out.setTo(x, y)
fun RectangleInt.getSize(out: SizeInt = SizeInt()): SizeInt = out.setTo(width, height)

val RectangleInt.position get() = getPosition()
val RectangleInt.size get() = getSize()

fun RectangleInt.setBoundsTo(left: Int, top: Int, right: Int, bottom: Int) =
    setTo(left, top, right - left, bottom - top)

////////////////////

operator fun IRectangleInt.contains(v: SizeInt): Boolean =
    (v.width <= width) && (v.height <= height)

operator fun IRectangleInt.contains(that: IPoint) = contains(that.x, that.y)
operator fun IRectangleInt.contains(that: IPointInt) = contains(that.x, that.y)
fun IRectangleInt.contains(x: Double, y: Double) =
    (x >= left && x < right) && (y >= top && y < bottom)

fun IRectangleInt.contains(x: Float, y: Float) = contains(x.toDouble(), y.toDouble())
fun IRectangleInt.contains(x: Int, y: Int) = contains(x.toDouble(), y.toDouble())

fun IRectangleInt.anchoredIn(
    container: RectangleInt,
    anchor: Anchor,
    out: RectangleInt = RectangleInt()
): RectangleInt =
    out.setTo(
        ((container.width - this.width) * anchor.sx).toInt(),
        ((container.height - this.height) * anchor.sy).toInt(),
        width,
        height
    )

fun IRectangleInt.getAnchorPosition(anchor: Anchor, out: MPointInt = MPointInt()): MPointInt =
    out.setTo((x + width * anchor.sx).toInt(), (y + height * anchor.sy).toInt())

fun MRectangle.asInt() = RectangleInt(this)
fun RectangleInt.asDouble() = this.rect

val IRectangle.int: RectangleInt get() = RectangleInt(x, y, width, height)
val IRectangleInt.float: MRectangle get() = MRectangle(x, y, width, height)

fun IRectangleInt.anchor(ax: Double, ay: Double): MPointInt =
    MPointInt((x + width * ax).toInt(), (y + height * ay).toInt())

inline fun IRectangleInt.anchor(ax: Number, ay: Number): MPointInt =
    anchor(ax.toDouble(), ay.toDouble())

val IRectangleInt.center get() = anchor(0.5, 0.5)

///////////////////////////

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

fun MRectangle.without(padding: IMargin): MRectangle =
    MRectangle.fromBounds(
        left + padding.left,
        top + padding.top,
        right - padding.right,
        bottom - padding.bottom
    )

fun MRectangle.with(margin: IMargin): MRectangle =
    MRectangle.fromBounds(
        left - margin.left,
        top - margin.top,
        right + margin.right,
        bottom + margin.bottom
    )

fun MRectangle.applyTransform(m: MMatrix): MRectangle {
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
