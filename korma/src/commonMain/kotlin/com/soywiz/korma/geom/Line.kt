package com.soywiz.korma.geom

import com.soywiz.korma.math.*

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

    fun containsX(x: Double): Boolean = (x in x0..x1) || (x in x1..x0) || (almostEquals(x, x0)) || (almostEquals(x, x1))
    fun containsY(y: Double): Boolean = (y in y0..y1) || (y in y1..y0) || (almostEquals(y, y0)) || (almostEquals(y, y1))
    fun containsBoundsXY(x: Double, y: Double): Boolean = containsX(x) && containsY(y)

    val angle: Angle get() = Angle.between(a, b)
    val length: Double get() = Point.distance(a, b)
    val lengthSquared: Double get() = Point.distanceSquared(a, b)

    override fun toString(): String = "Line($a, $b)"

    fun getLineIntersectionPoint(line: Line, out: Point = Point()): Point? {
        return getIntersectXY(x0, y0, x1, y1, line.x0, line.y0, line.x1, line.y1, out)
    }

    fun getIntersectionPoint(line: Line, out: Point = Point()): Point? = getSegmentIntersectionPoint(line, out)
    fun getSegmentIntersectionPoint(line: Line, out: Point = Point()): Point? {
        if (getIntersectXY(x0, y0, x1, y1, line.x0, line.y0, line.x1, line.y1, out) != null) {
            if (this.containsBoundsXY(out.x, out.y) && line.containsBoundsXY(out.x, out.y)) {
                return out
            }
        }
        return null
    }

    fun intersectsLine(line: Line): Boolean = getLineIntersectionPoint(line, temp) != null
    fun intersects(line: Line): Boolean = intersectsSegment(line)
    fun intersectsSegment(line: Line): Boolean {
        return getSegmentIntersectionPoint(line, temp) != null
        //val line1 = this
        //val line2 = line
        //val o1 = Point.orientation(line1.a, line1.b, line2.a).sign
        //val o2 = Point.orientation(line1.a, line1.b, line2.b).sign
        //val o3 = Point.orientation(line2.a, line2.b, line1.a).sign
        //val o4 = Point.orientation(line2.a, line2.b, line1.b).sign
        //return when {
        //    o1 != o2 && o3 != o4 -> true
        //    o1.isAlmostZero() && onSegment(line1.a, line2.a, line1.b) -> true
        //    o2.isAlmostZero() && onSegment(line1.a, line2.b, line1.b) -> true
        //    o3.isAlmostZero() && onSegment(line2.a, line1.a, line2.b) -> true
        //    o4.isAlmostZero() && onSegment(line2.a, line1.b, line2.b) -> true
        //    else -> false
        //}
    }

    //private fun onSegment(p: Point, q: Point, r: Point): Boolean =
    //    ((q.x <= max(p.x, r.x)) && (q.x >= min(p.x, r.x)) &&
    //        (q.y <= max(p.y, r.y)) && (q.y >= min(p.y, r.y)))

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

        fun getIntersectXY(a: IPoint, b: IPoint, c: IPoint, d: IPoint, out: Point = Point()): IPoint? {
            return getIntersectXY(a.x, a.y, b.x, b.y, c.x, c.y, d.x, d.y, out)
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

