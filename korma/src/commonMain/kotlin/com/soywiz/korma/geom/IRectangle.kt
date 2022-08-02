package com.soywiz.korma.geom

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
        ): IRectangle = Rectangle(x, y, width, height)

        inline operator fun invoke(x: Float, y: Float, width: Float, height: Float): IRectangle =
            Rectangle(x, y, width, height)

        inline operator fun invoke(x: Int, y: Int, width: Int, height: Int): IRectangle =
            Rectangle(x, y, width, height)

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
        operator fun invoke(point1: IPoint, point2: IPoint): Rectangle {
            val left = minOf(point1.x, point2.x)
            val top = minOf(point1.y, point2.y)
            val right = maxOf(point1.x, point2.x)
            val bottom = maxOf(point1.y, point2.y)
            return Rectangle(left, top, right - left, bottom - top)
        }
    }
}

val IRectangle.area: Double get() = width * height
fun IRectangle.clone(): Rectangle = Rectangle(x, y, width, height)
val IRectangle.isNotEmpty: Boolean get() = width != 0.0 || height != 0.0
val IRectangle.mutable: Rectangle get() = Rectangle(x, y, width, height)

val IRectangle.left get() = x
val IRectangle.top get() = y
val IRectangle.right get() = x + width
val IRectangle.bottom get() = y + height

val IRectangle.topLeft get() = Point(left, top)
val IRectangle.topRight get() = Point(right, top)
val IRectangle.bottomLeft get() = Point(left, bottom)
val IRectangle.bottomRight get() = Point(right, bottom)

val IRectangle.centerX get() = (right + left) * 0.5
val IRectangle.centerY get() = (bottom + top) * 0.5
val IRectangle.center get() = Point(centerX, centerY)

operator fun IRectangle.contains(that: IPoint) = contains(that.x, that.y)
operator fun IRectangle.contains(that: IPointInt) = contains(that.x, that.y)
fun IRectangle.contains(x: Double, y: Double) = (x >= left && x < right) && (y >= top && y < bottom)
fun IRectangle.contains(x: Float, y: Float) = contains(x.toDouble(), y.toDouble())
fun IRectangle.contains(x: Int, y: Int) = contains(x.toDouble(), y.toDouble())
