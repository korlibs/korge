package korlibs.math.geom

import korlibs.math.annotations.*

@KormaMutableApi
inline class MRectangleInt(val rect: MRectangle) {
    companion object {
        operator fun invoke(): MRectangleInt = MRectangleInt(MRectangle())
        operator fun invoke(x: Int, y: Int, width: Int, height: Int): MRectangleInt = MRectangleInt(MRectangle(x, y, width, height))
        operator fun invoke(x: Float, y: Float, width: Float, height: Float): MRectangleInt = MRectangleInt(MRectangle(x, y, width, height))
        operator fun invoke(x: Double, y: Double, width: Double, height: Double): MRectangleInt = MRectangleInt(MRectangle(x, y, width, height))
        operator fun invoke(other: MRectangleInt): MRectangleInt = MRectangleInt(MRectangle(other.x, other.y, other.width, other.height))
        fun fromBounds(left: Int, top: Int, right: Int, bottom: Int): MRectangleInt = MRectangleInt(left, top, right - left, bottom - top)
    }

    fun clone(): MRectangleInt = MRectangleInt(x, y, width, height)
    @Deprecated("Allocates")
    fun expanded(border: MarginInt): MRectangleInt = clone().expand(border)

    val area: Int get() = width * height

    @Deprecated("Allocates")
    val topLeft: MPointInt get() = MPointInt(left, top)
    @Deprecated("Allocates")
    val topRight: MPointInt get() = MPointInt(right, top)
    @Deprecated("Allocates")
    val bottomLeft: MPointInt get() = MPointInt(left, bottom)
    @Deprecated("Allocates")
    val bottomRight: MPointInt get() = MPointInt(right, bottom)

    fun sliceWithBounds(left: Int, top: Int, right: Int, bottom: Int, clamped: Boolean = true): MRectangleInt {
        val left = if (!clamped) left else left.coerceIn(0, this.width)
        val right = if (!clamped) right else right.coerceIn(0, this.width)
        val top = if (!clamped) top else top.coerceIn(0, this.height)
        val bottom = if (!clamped) bottom else bottom.coerceIn(0, this.height)
        return MRectangleInt.fromBounds(this.x + left, this.y + top, this.x + right, this.y + bottom)
    }

    fun sliceWithSize(x: Int, y: Int, width: Int, height: Int, clamped: Boolean = true): MRectangleInt =
        sliceWithBounds(x, y, x + width, y + height, clamped)

    operator fun contains(v: SizeInt): Boolean = (v.width <= width) && (v.height <= height)
    operator fun contains(v: MSizeInt): Boolean = contains(v.immutable)
    operator fun contains(that: Point) = contains(that.x, that.y)
    operator fun contains(that: MPoint) = contains(that.x, that.y)
    operator fun contains(that: Vector2Int) = contains(that.x, that.y)
    operator fun contains(that: MPointInt) = contains(that.x, that.y)
    fun contains(x: Double, y: Double) = (x >= left && x < right) && (y >= top && y < bottom)
    fun contains(x: Float, y: Float) = contains(x.toDouble(), y.toDouble())
    fun contains(x: Int, y: Int) = contains(x.toDouble(), y.toDouble())

    var x: Int
        get() = rect.x.toInt()
        set(value) {
            rect.x = value.toDouble()
        }
    var y: Int
        get() = rect.y.toInt()
        set(value) {
            rect.y = value.toDouble()
        }
    var width: Int
        get() = rect.width.toInt()
        set(value) {
            rect.width = value.toDouble()
        }
    var height: Int
        get() = rect.height.toInt()
        set(value) {
            rect.height = value.toDouble()
        }
    var left: Int
        get() = rect.left.toInt()
        set(value) {
            rect.left = value.toDouble()
        }
    var top: Int
        get() = rect.top.toInt()
        set(value) {
            rect.top = value.toDouble()
        }
    var right: Int
        get() = rect.right.toInt()
        set(value) {
            rect.right = value.toDouble()
        }
    var bottom: Int
        get() = rect.bottom.toInt()
        set(value) {
            rect.bottom = value.toDouble()
        }

    fun anchoredIn(
        container: MRectangleInt,
        anchor: Anchor,
        out: MRectangleInt = MRectangleInt()
    ): MRectangleInt = out.setTo(
        ((container.width - this.width) * anchor.doubleX).toInt(),
        ((container.height - this.height) * anchor.doubleY).toInt(),
        width,
        height
    )

    fun getAnchorPosition(anchor: Anchor, out: MPointInt = MPointInt()): MPointInt =
        out.setTo((x + width * anchor.doubleX).toInt(), (y + height * anchor.doubleY).toInt())

    val center: MPoint get() = anchor(0.5, 0.5).double
    inline fun anchor(ax: Number, ay: Number): MPointInt = anchor(ax.toDouble(), ay.toDouble())
    fun anchor(ax: Double, ay: Double): MPointInt = MPointInt((x + width * ax).toInt(), (y + height * ay).toInt())

    fun setTo(that: MRectangleInt) = setTo(that.x, that.y, that.width, that.height)
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
    fun expand(border: MarginInt): MRectangleInt =
        this.setBoundsTo(left - border.left, top - border.top, right + border.right, bottom + border.bottom)

    override fun toString(): String = "Rectangle(x=$x, y=$y, width=$width, height=$height)"
    fun toStringBounds(): String = "Rectangle([$left,$top]-[$right,$bottom])"
    fun copyFrom(rect: MRectangleInt): MRectangleInt = setTo(rect.x, rect.y, rect.width, rect.height)
    fun copyFrom(rect: RectangleInt): MRectangleInt = setTo(rect.x, rect.y, rect.width, rect.height)

    fun setToUnion(a: MRectangleInt, b: MRectangleInt): MRectangleInt = setToBounds(
        kotlin.math.min(a.left, b.left),
        kotlin.math.min(a.top, b.top),
        kotlin.math.max(a.right, b.right),
        kotlin.math.max(a.bottom, b.bottom)
    )

    fun setToUnion(a: MRectangleInt, b: RectangleInt): MRectangleInt = setToBounds(
        kotlin.math.min(a.left, b.left),
        kotlin.math.min(a.top, b.top),
        kotlin.math.max(a.right, b.right),
        kotlin.math.max(a.bottom, b.bottom)
    )

    fun setToUnion(a: RectangleInt, b: RectangleInt): MRectangleInt = setToBounds(
        kotlin.math.min(a.left, b.left),
        kotlin.math.min(a.top, b.top),
        kotlin.math.max(a.right, b.right),
        kotlin.math.max(a.bottom, b.bottom)
    )

    @KormaMutableApi fun asDouble(): MRectangle = this.rect
    @KormaMutableApi val float: MRectangle get() = MRectangle(x, y, width, height)
    @KormaValueApi val value: Rectangle get() = Rectangle(x, y, width, height)
}
