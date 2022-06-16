package com.soywiz.korma.geom

import com.soywiz.korma.annotations.KormaExperimental
import com.soywiz.korma.math.almostEquals
import com.soywiz.korma.math.clamp
import com.soywiz.korma.math.isAlmostZero

interface ILine {
    val a: IPoint
    val b: IPoint
}

data class Line(override val a: Point, override val b: Point) : ILine {
    private val temp = Point()

    fun clone(): Line = Line(a.copy(), b.copy())

    val minX: Double get() = kotlin.math.min(a.x, b.x)
    val maxX: Double get() = kotlin.math.max(a.x, b.x)

    val minY: Double get() = kotlin.math.min(a.y, b.y)
    val maxY: Double get() = kotlin.math.max(a.y, b.y)

    fun round(): Line {
        a.round()
        b.round()
        return this
    }

    fun setTo(a: IPoint, b: IPoint): Line {
        return setTo(a.x, a.y, b.x, b.y)
    }

    fun setTo(x0: Double, y0: Double, x1: Double, y1: Double): Line {
        a.setTo(x0, y0)
        b.setTo(x1, y1)
        return this
    }

    fun setToPolar(x: Double, y: Double, angle: Angle, length: Double = 1.0): Line {
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

    @KormaExperimental
    fun scalePoints(scale: Double): Line {
        val dx = this.dx
        val dy = this.dy
        x0 -= dx * scale
        y0 -= dy * scale
        x1 += dx * scale
        y1 += dy * scale
        //val p1 = getIntersectXY(rect.topLeft, rect.topRight, this.a, this.b)
        //val p2 = getIntersectXY(rect.bottomLeft, rect.bottomRight, this.a, this.b)
        //val p3 = getIntersectXY(rect.topLeft, rect.bottomLeft, this.a, this.b)
        //val p4 = getIntersectXY(rect.topRight, rect.bottomRight, this.a, this.b)
        //if (p1 != null) {
        //    if (Line(this.a, p1!!.mutable).angle.isAlmostEquals(this.angle)) {
        //        this.setTo(this.a, p1!!.mutable)
        //    }
        //    if (Line(p1!!.mutable, this.b).angle.isAlmostEquals(this.angle)) {
        //        this.setTo(p1!!.mutable, this.b)
        //    }
        //}
        //println("p1=$p1, p2=$p2, p3=$p3, p4=$p4")
        return this
    }

    constructor() : this(Point(), Point())
    constructor(x0: Double, y0: Double, x1: Double, y1: Double) : this(Point(x0, y0), Point(x1, y1))
    constructor(x0: Float, y0: Float, x1: Float, y1: Float) : this(Point(x0, y0), Point(x1, y1))
    constructor(x0: Int, y0: Int, x1: Int, y1: Int) : this(Point(x0, y0), Point(x1, y1))

    var x0: Double by a::x
    var y0: Double by a::y

    var x1: Double by b::x
    var y1: Double by b::y

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
        fun fromPointAndDirection(point: IPoint, direction: IPoint, scale: Double = 1.0, out: Line = Line()): Line {
            return out.setTo(point.x, point.y, point.x + direction.x * scale, point.y + direction.y * scale)
        }
        fun fromPointAngle(point: IPoint, angle: Angle, length: Double = 1.0, out: Line = Line()): Line =
            out.setToPolar(point.x, point.y, angle, length)

        fun length(Ax: Double, Ay: Double, Bx: Double, By: Double): Double = kotlin.math.hypot(Bx - Ax, By - Ay)

        inline fun getIntersectXY(Ax: Double, Ay: Double, Bx: Double, By: Double, Cx: Double, Cy: Double, Dx: Double, Dy: Double, out: (x: Double, y: Double) -> Unit): Boolean {
            val a1 = By - Ay
            val b1 = Ax - Bx
            val c1 = a1 * (Ax) + b1 * (Ay)
            val a2 = Dy - Cy
            val b2 = Cx - Dx
            val c2 = a2 * (Cx) + b2 * (Cy)
            val determinant = a1 * b2 - a2 * b1
            if (determinant.isAlmostZero()) return false
            val x = (b2 * c1 - b1 * c2) / determinant
            val y = (a1 * c2 - a2 * c1) / determinant
            //if (!x.isFinite() || !y.isFinite()) TODO()
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

data class LineIntersection(
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

fun Line.Companion.projectedPoint(
    v1x: Double,
    v1y: Double,
    v2x: Double,
    v2y: Double,
    px: Double,
    py: Double,
    out: Point = Point()
): Point {
    // return this.getIntersectionPoint(Line(point, Point.fromPolar(point, this.angle + 90.degrees)))!!
    // get dot product of e1, e2
    val e1x = v2x - v1x
    val e1y = v2y - v1y
    val e2x = px - v1x
    val e2y = py - v1y
    val valDp = Point.dot(e1x, e1y, e2x, e2y)
    // get length of vectors

    val lenLineE1 = kotlin.math.hypot(e1x, e1y)
    val lenLineE2 = kotlin.math.hypot(e2x, e2y)

    // What happens if lenLineE1 or lenLineE2 are zero?, it would be a division by zero.
    // Does that mean that the point is on the line, and we should use it?
    if (lenLineE1 == 0.0 || lenLineE2 == 0.0) {
        return out.setTo(px, py)
    }

    val cos = valDp / (lenLineE1 * lenLineE2)

    // length of v1P'
    val projLenOfLine = cos * lenLineE2

    return out.setTo((v1x + (projLenOfLine * e1x) / lenLineE1), (v1y + (projLenOfLine * e1y) / lenLineE1))
}

fun Line.Companion.projectedPoint(
    v1: IPoint,
    v2: IPoint,
    point: IPoint,
    out: Point = Point()
): Point = projectedPoint(v1.x, v1.y, v2.x, v2.y, point.x, point.y, out)

fun Line.Companion.lineIntersectionPoint(
    l1: Line,
    l2: Line,
    out: Point = Point()
): Point? = l1.getLineIntersectionPoint(l2, out)

fun Line.Companion.segmentIntersectionPoint(
    l1: Line,
    l2: Line,
    out: Point = Point()
): Point? = l1.getSegmentIntersectionPoint(l2, out)

// @TODO: Should we create a common interface make projectedPoint part of it? (for ecample to project other kind of shapes)
// https://math.stackexchange.com/questions/62633/orthogonal-projection-of-a-point-onto-a-line
// http://www.sunshine2k.de/coding/java/PointOnLine/PointOnLine.html
fun ILine.projectedPoint(point: IPoint, out: Point = Point()): Point = Line.projectedPoint(a, b, point, out)
