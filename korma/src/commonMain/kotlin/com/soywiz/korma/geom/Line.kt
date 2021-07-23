package com.soywiz.korma.geom

import com.soywiz.korma.math.*
import kotlin.math.*

open class Line(val a: Point, val b: Point) {
    private val temp = Point()

    fun round(): Line {
        a.round()
        b.round()
        return this
    }

    fun setTo(x0: Double, y0: Double, x1: Double, y1: Double): Line {
        a.setTo(x0, y0)
        b.setTo(x1, y1)
        return this
    }

    fun setToPolar(x: Double, y: Double, angle: Angle, length: Double): Line {
        setTo(x, y, x + angle.cosine * length, y + angle.sine * length)
        return this
    }

    fun directionVector(out: Point = Point()): Point {
        out.setTo(dx, dy)
        return out
    }

    fun getMinimumDistance(p: Point): Double {
        val v = a
        val w = b
        val l2 = Point.distanceSquared(v, w)
        if (l2 == 0.0) return Point.distanceSquared(p, a)
        val t = (Point.dot(p - v, w - v) / l2).clamp(0.0, 1.0)
        return Point.distance(p, v + (w - v) * t);
    }

    constructor() : this(Point(), Point())
    constructor(x0: Double, y0: Double, x1: Double, y1: Double) : this(Point(x0, y0), Point(x1, y1))
    constructor(x0: Float, y0: Float, x1: Float, y1: Float) : this(Point(x0, y0), Point(x1, y1))
    constructor(x0: Int, y0: Int, x1: Int, y1: Int) : this(Point(x0, y0), Point(x1, y1))

    var x0: Double get() = a.x ; set(value) { a.x = value }
    var y0: Double get() = a.y ; set(value) { a.y = value }

    var x1: Double get() = b.x ; set(value) { b.x = value }
    var y1: Double get() = b.y ; set(value) { b.y = value }

    val dx: Double get() = x1 - x0
    val dy: Double get() = y1 - y0

    fun containsX(x: Double) = x in x0..x1 || x in x1..x0
    fun containsY(y: Double) = y in y0..y1 || y in y1..y0

    val angle: Angle get() = Angle.between(a, b)
    val length: Double get() = Point.distance(a, b)
    val lengthSquared: Double get() = Point.distanceSquared(a, b)

    override fun toString(): String = "Line($a, $b)"

    fun getIntersectionPoint(line: Line, out: Point = Point()): Point? {
        if (getIntersectXY(x0, y0, x1, y1, line.x0, line.y0, line.x1, line.y1, out) != null) {
            if (line.containsY(out.y) && this.containsY(out.y) && line.containsX(out.x) && this.containsX(out.x)) {
                return out
            }
        }
        return null
    }

    fun intersects(line: Line): Boolean {
        return getIntersectionPoint(line, temp) != null
    }

    companion object {
        inline fun getIntersectXY(Ax: Double, Ay: Double, Bx: Double, By: Double, Cx: Double, Cy: Double, Dx: Double, Dy: Double, out: (x: Double, y: Double) -> Unit): Boolean {
            val a1 = By - Ay
            val b1 = Ax - Bx
            val c1 = a1 * (Ax) + b1 * (Ay)
            val a2 = Dy - Cy
            val b2 = Cx - Dx
            val c2 = a2 * (Cx) + b2 * (Cy)
            val determinant = a1 * b2 - a2 * b1
            if (determinant == 0.0) return false
            val x = (b2 * c1 - b1 * c2) / determinant
            val y = (a1 * c2 - a2 * c1) / determinant
            out(x, y)
            return true
        }

        fun getIntersectXY(Ax: Double, Ay: Double, Bx: Double, By: Double, Cx: Double, Cy: Double, Dx: Double, Dy: Double, out: Point = Point()): Point? {
            return if (getIntersectXY(Ax, Ay, Bx, By, Cx, Cy, Dx, Dy) { x, y -> out.setTo(x, y) }) out else null
        }
    }
}

open class LineIntersection(
    val line: Line = Line(),
    val intersection: Point = Point()
) {
    val normalVector: Line = Line()

    fun setFrom(x0: Double, y0: Double, x1: Double, y1: Double, ix: Double, iy: Double, normalLength: Double) {
        line.setTo(x0, y0, x1, y1)
        intersection.setTo(ix, iy)
        normalVector.setToPolar(ix, iy, line.angle - 90.degrees, normalLength)
    }

    override fun toString(): String = "LineIntersection($line, intersection=$intersection)"
}

