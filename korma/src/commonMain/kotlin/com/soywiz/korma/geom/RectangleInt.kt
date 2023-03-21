package com.soywiz.korma.geom

import com.soywiz.kmem.pack.*

//@KormaValueApi
inline class RectangleInt(val data: Int4Pack) {
    val position: Vector2Int get() = Vector2Int(data.i0, data.i1)
    val size: SizeInt get() = SizeInt(data.i2, data.i3)

    val x: Int get() = data.i0
    val y: Int get() = data.i1
    val width: Int get() = data.i2
    val height: Int get() = data.i3

    val area: Int get() = width * height
    val isEmpty: Boolean get() = width == 0 && height == 0
    val isNotEmpty: Boolean get() = !isEmpty
    val mutable: MRectangleInt get() = MRectangleInt(x, y, width, height)

    val left: Int get() = x
    val top: Int get() = y
    val right: Int get() = x + width
    val bottom: Int get() = y + height

    val topLeft: Vector2Int get() = Vector2Int(left, top)
    val topRight: Vector2Int get() = Vector2Int(right, top)
    val bottomLeft: Vector2Int get() = Vector2Int(left, bottom)
    val bottomRight: Vector2Int get() = Vector2Int(right, bottom)

    val centerX: Int get() = ((right + left) * 0.5f).toInt()
    val centerY: Int get() = ((bottom + top) * 0.5f).toInt()
    val center: Vector2Int get() = Vector2Int(centerX, centerY)

    fun toFloat(): Rectangle = Rectangle(position.toFloat(), size.toFloat())

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
    operator fun contains(that: Vector2Int): Boolean = contains(that.x, that.y)
    fun contains(x: Float, y: Float): Boolean = (x >= left && x < right) && (y >= top && y < bottom)
    fun contains(x: Double, y: Double): Boolean = contains(x.toFloat(), y.toFloat())
    fun contains(x: Int, y: Int): Boolean = contains(x.toFloat(), y.toFloat())

    constructor() : this(Vector2Int(), SizeInt())
    constructor(position: Vector2Int, size: SizeInt) : this(int4PackOf(position.x, position.y, size.width, size.height))
    constructor(x: Int, y: Int, width: Int, height: Int) : this(Vector2Int(x, y), SizeInt(width, height))

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

        fun fromBounds(topLeft: Vector2Int, bottomRight: Vector2Int): RectangleInt = RectangleInt(topLeft, (bottomRight - topLeft).toSize())
        fun fromBounds(left: Int, top: Int, right: Int, bottom: Int): RectangleInt = fromBounds(Vector2Int(left, top), Vector2Int(right, bottom))
    }
}
