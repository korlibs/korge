package com.soywiz.korma.geom.vector

import com.soywiz.korma.annotations.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.internal.*
import kotlin.math.*

@Suppress("DuplicatedCode")
@KormaExperimental
@KormaMutableApi
class MEdge {
    fun getX(n: Int) = if (n == 0) this.ax else this.bx
    fun getY(n: Int) = if (n == 0) this.ay else this.by

    companion object {
        operator fun invoke(ax: Int, ay: Int, bx: Int, by: Int, wind: Int = 0) = MEdge().setTo(ax, ay, bx, by, wind)
        operator fun invoke(a: MPointInt, b: MPointInt, wind: Int = 0) = this(a.x, a.y, b.x, b.y, wind)

        fun getIntersectY(a: MEdge, b: MEdge): Int = getIntersectXYInt(a, b)?.y ?: Int.MIN_VALUE
        fun getIntersectX(a: MEdge, b: MEdge): Int = getIntersectXYInt(a, b)?.x ?: Int.MIN_VALUE

        fun areParallel(a: MEdge, b: MEdge) = ((a.by - a.ay) * (b.ax - b.bx)) - ((b.by - b.ay) * (a.ax - a.bx)) == 0
        fun getIntersectXY(a: MEdge, b: MEdge): Point? = _getIntersectXY(a, b)?.let { Point(it.x, it.y) }
        fun getIntersectXYInt(a: MEdge, b: MEdge): PointInt? = _getIntersectXY(a, b)

        fun angleBetween(a: MEdge, b: MEdge): Angle {
            return b.angle - a.angle
        }

        // https://www.geeksforgeeks.org/program-for-point-of-intersection-of-two-lines/
        inline fun _getIntersectXY(a: MEdge, b: MEdge): PointInt? {
            val Ax: Double = a.ax.toDouble()
            val Ay: Double = a.ay.toDouble()
            val Bx: Double = a.bx.toDouble()
            val By: Double = a.by.toDouble()
            val Cx: Double = b.ax.toDouble()
            val Cy: Double = b.ay.toDouble()
            val Dx: Double = b.bx.toDouble()
            val Dy: Double = b.by.toDouble()
            return getIntersectXY(Ax, Ay, Bx, By, Cx, Cy, Dx, Dy)?.let { PointInt(floorCeil(it.xD).toInt(), floorCeil(it.yD).toInt()) }
        }

        fun getIntersectXY(Ax: Double, Ay: Double, Bx: Double, By: Double, Cx: Double, Cy: Double, Dx: Double, Dy: Double): Point? {
            return MLine.getIntersectXY(Ax, Ay, Bx, By, Cx, Cy, Dx, Dy)
        }
    }

    var ax = 0; private set
    var ay = 0; private set
    var bx = 0; private set
    var by = 0; private set
    var wind: Int = 0; private set

    var dy: Int = 0; private set
    var dx: Int = 0; private set
    var isCoplanarX: Boolean = false; private set
    var isCoplanarY: Boolean = false; private set

    var h: Int = 0; private set

    val length: Float get() = hypot(dx.toFloat(), dy.toFloat())

    fun copyFrom(other: MEdge) = setTo(other.ax, other.ay, other.bx, other.by, other.wind)

    fun setTo(ax: Int, ay: Int, bx: Int, by: Int, wind: Int) = this.apply {
        this.ax = ax
        this.ay = ay
        this.bx = bx
        this.by = by
        this.dx = bx - ax
        this.dy = by - ay
        this.isCoplanarX = ay == by
        this.isCoplanarY = ax == bx
        this.wind = wind
        this.h = if (isCoplanarY) 0 else ay - (ax * dy) / dx
    }

    fun setToHalf(a: MEdge, b: MEdge): MEdge = this.apply {
        val minY = min(a.minY, b.minY)
        val maxY = min(a.maxY, b.maxY)
        val minX = (a.intersectX(minY) + b.intersectX(minY)) / 2
        val maxX = (a.intersectX(maxY) + b.intersectX(maxY)) / 2
        setTo(minX, minY, maxX, maxY, +1)
    }

    val minX get() = min(ax, bx)
    val maxX get() = max(ax, bx)
    val minY get() = min(ay, by)
    val maxY get() = max(ay, by)

    @Suppress("ConvertTwoComparisonsToRangeCheck")
    //fun containsY(y: Int): Boolean = if (ay == by) y == ay else if (wind >= 0) y >= ay && y < by else y > ay && y <= by
    fun containsY(y: Int): Boolean {
        return y >= ay && y < by
        //val a = if (wind >= 0) y >= ay && y < by else y > ay && y <= by
        //val b = y >= ay && y < by
        //if (a != b) {
        //    println("wind=$wind, y=$y, ay=$ay, by=$by, a=$a, b=$b")
        //}
        //return a
    }

    //fun containsYNear(y: Int, offset: Int): Boolean = y >= (ay - offset) && y < (by + offset)
    //fun containsY(y: Int): Boolean = y in ay..by
    //fun containsYNear(y: Int, offset: Int): Boolean = y >= (ay - offset) && y <= (by + offset)
    //fun intersectX(y: Int): Int = if (isCoplanarY) ax else ((y - h) * dx) / dy
    //fun intersectX(y: Int): Int = if (dy == 0) ax else ((y - h) * dx) / dy
    fun intersectX(y: Int): Int = if (isCoplanarY || dy == 0) ax else ((y - h) * dx) / dy
    //fun intersectX(y: Double): Double = if (isCoplanarY) ax else ((y - h) * this.dx) / this.dy

    // Stroke extensions
    val angle: Angle get() = Angle.between(ax, ay, bx, by)
    val cos: Double get() = angle.cosineD
    val absCos: Double get() = cos.absoluteValue
    val sin: Double get() = angle.sineD
    val absSin: Double get() = sin.absoluteValue

    override fun toString(): String = "Edge([$ax,$ay]-[$bx,$by])"
    fun toString(scale: Double): String = "Edge([${(ax * scale).toInt()},${(ay * scale).toInt()}]-[${(bx * scale).toInt()},${(by * scale).toInt()}])"
}


// y = (m * x) + b
// x = (y - b) / m
/*
internal data class Line(var m: Double, var b: Double) {
    // Aliases
    val slope get() = m
    val yIntercept get() = b
    val isXCoplanar get() = m == 0.0
    val isYCoplanar get() = m.isInfinite()

    companion object {
        fun fromTwoPoints(ax: Double, ay: Double, bx: Double, by: Double) = Line(0.0, 0.0).setFromTwoPoints(ax, ay, bx, by)
        fun getHalfLine(a: Line, b: Line, out: Line = Line(0.0, 0.0)): Line {


            return out.setFromTwoPoints()
        }
    }

    fun setFromTwoPoints(ax: Double, ay: Double, bx: Double, by: Double) = this.apply {
        this.m = (by - ay) / (bx - ax)
        // y = (slope * x) + b
        // ay = (slope * ax) + b
        // b = ay - (slope * ax)
        this.b = ay - (this.m * ax)
    }

    fun getY(x: Double) = if (isYCoplanar) 0.0 else (m * x) + b
    fun getX(y: Double) = if (isXCoplanar) 0.0 else (y - b) / m

    // y = (m0 * x) + b0
    // y = (m1 * x) + b1
    // (m0 * x) + b0 = (m1 * x) + b1
    // (m0 * x) = (m1 * x) + b1 - b0
    // (m0 * x) - (m1 * x) = b1 - b0
    // (m0 - m1) * x = b1 - b0
    // x = (b1 - b0) / (m0 - m1)
    fun getIntersectionX(other: Line): Double = (other.b - this.b) / (this.m - other.m)

    fun getIntersection(other: Line, out: Point = Point()): Point {
        val x = getIntersectionX(other)
        return out.setTo(x, getY(x))
    }

    fun getSegmentFromX(x0: Double, x1: Double) = LineSegment(x0, getY(x0), x1, getY(x1))
    fun getSegmentFromY(y0: Double, y1: Double) = LineSegment(getX(y0), y0, getX(y1), y1)
}

internal class LineSegment(ax: Double, ay: Double, bx: Double, by: Double) {
    var ax = ax; private set
    var ay = ay; private set
    var bx = bx; private set
    var by = by; private set
    val line = Line.fromTwoPoints(ax, ay, bx, by)
    fun setTo(ax: Double, ay: Double, bx: Double, by: Double) = this.apply {
        this.ax = ax
        this.ay = ay
        this.bx = bx
        this.by = by
        this.line.setFromTwoPoints(ax, ay, bx, by)
    }
    val slope get() = line.slope
    val length get() = Point.distance(ax, ay, bx, by)
}

internal data class Line(val ax: Double, val ay: Double, val bx: Double, val by: Double) {
    val minX get() = min(ax, bx)
    val maxX get() = max(ax, bx)
    val minY get() = min(ay, by)
    val maxY get() = max(ay, by)

    val isCoplanarX get() = ay == by
    val isCoplanarY get() = ax == bx
    val dy get() = (by - ay)
    val dx get() = (bx - ax)
    val slope get() = dy / dx
    val islope get() = 1.0 / slope

    val h = if (isCoplanarY) 0.0 else ay - (ax * dy) / dx

    fun containsY(y: Double): Boolean = y >= ay && y < by
    fun containsYNear(y: Double, offset: Double): Boolean = y >= (ay - offset) && y < (by + offset)
    fun getX(y: Double): Double = if (isCoplanarY) ax else ((y - h) * dx) / dy
    fun getY(x: Double): Double = if (isCoplanarX) ay else TODO()
    fun intersect(line: Line, out: Point = Point()): Point? {

    }
    //fun intersectX(y: Double): Double = if (isCoplanarY) ax else ((y - h) * this.dx) / this.dy

    // Stroke extensions
    val angle = Angle.between(ax, ay, bx, by)
    val cos = angle.cosine
    val absCos = cos.absoluteValue
    val sin = angle.sine
    val absSin = sin.absoluteValue
}
*/
