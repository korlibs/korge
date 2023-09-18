package korlibs.math.geom

import korlibs.math.*
import korlibs.math.annotations.*

@KormaMutableApi
@Deprecated("Use Line instead")
data class MLine(var a: Point, var b: Point) {
    fun clone(): MLine = MLine(a, b)
    fun flipped(): MLine = MLine(b, a)

    val minX: Double get() = kotlin.math.min(a.xD, b.xD)
    val maxX: Double get() = kotlin.math.max(a.xD, b.xD)
    val minY: Double get() = kotlin.math.min(a.yD, b.yD)
    val maxY: Double get() = kotlin.math.max(a.yD, b.yD)

    fun round(): MLine {
        a.round()
        b.round()
        return this
    }

    fun setTo(a: Point, b: Point): MLine = setTo(a.xD, a.yD, b.xD, b.yD)
    fun setTo(a: MPoint, b: MPoint): MLine = setTo(a.x, a.y, b.x, b.y)

    fun setTo(x0: Double, y0: Double, x1: Double, y1: Double): MLine {
        a = Point(x0, y0)
        b = Point(x1, y1)
        return this
    }

    fun setToPolar(x: Double, y: Double, angle: Angle, length: Double = 1.0): MLine {
        setTo(x, y, x + angle.cosineD * length, y + angle.sineD * length)
        return this
    }

    fun directionVector(out: MPoint = MPoint()): MPoint {
        out.setTo(dx, dy)
        return out
    }

    fun getMinimumDistance(p: Point): Double {
        val v = a
        val w = b
        val l2 = Point.distanceSquared(v, w)
        if (l2 == 0.0f) return Point.distanceSquared(p, a).toDouble()
        val t = (Point.dot(p - v, w - v) / l2).clamp(0.0f, 1.0f).toDouble()
        return Point.distance(p, v + (w - v) * t).toDouble()
    }

    @KormaExperimental
    fun scalePoints(scale: Double): MLine {
        a -= delta * scale
        b -= delta * scale
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
    constructor(p0: MPoint, p1: MPoint) : this(p0.point, p1.point)
    constructor(x0: Double, y0: Double, x1: Double, y1: Double) : this(MPoint(x0, y0), MPoint(x1, y1))
    constructor(x0: Float, y0: Float, x1: Float, y1: Float) : this(MPoint(x0, y0), MPoint(x1, y1))
    constructor(x0: Int, y0: Int, x1: Int, y1: Int) : this(MPoint(x0, y0), MPoint(x1, y1))

    val x0: Double get() = a.xD
    val y0: Double get() = a.yD
    val x1: Double get() = b.xD
    val y1: Double get() = b.yD

    val delta: Point get() = b - a
    val dx: Double get() = x1 - x0
    val dy: Double get() = y1 - y0

    fun containsX(x: Double): Boolean = (x in x0..x1) || (x in x1..x0) || (almostEquals(x, x0)) || (almostEquals(x, x1))
    fun containsY(y: Double): Boolean = (y in y0..y1) || (y in y1..y0) || (almostEquals(y, y0)) || (almostEquals(y, y1))
    fun containsBoundsXY(x: Double, y: Double): Boolean = containsX(x) && containsY(y)

    val angle: Angle get() = Angle.between(a, b)
    val length: Double get() = Point.distance(a, b).toDouble()
    val lengthSquared: Double get() = Point.distanceSquared(a, b).toDouble()

    override fun toString(): String = "Line($a, $b)"

    fun getLineIntersectionPoint(line: MLine): Point? {
        return getIntersectXY(x0, y0, x1, y1, line.x0, line.y0, line.x1, line.y1)
    }

    fun getIntersectionPoint(line: MLine): Point? = getSegmentIntersectionPoint(line)
    fun getSegmentIntersectionPoint(line: MLine): Point? {
        val out = getIntersectXY(x0, y0, x1, y1, line.x0, line.y0, line.x1, line.y1)
        if (out != null) {
            if (this.containsBoundsXY(out.xD, out.yD) && line.containsBoundsXY(out.xD, out.yD)) {
                return out
            }
        }
        return null
    }

    fun intersectsLine(line: MLine): Boolean = getLineIntersectionPoint(line) != null
    fun intersects(line: MLine): Boolean = intersectsSegment(line)
    fun intersectsSegment(line: MLine): Boolean {
        return getSegmentIntersectionPoint(line) != null
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
        fun fromPointAndDirection(point: Point, direction: Point, scale: Double = 1.0, out: MLine = MLine()): MLine =
            out.setTo(point.xD, point.yD, point.x + direction.x * scale, point.y + direction.y * scale)
        fun fromPointAngle(point: Point, angle: Angle, length: Double = 1.0, out: MLine = MLine()): MLine = out.setToPolar(point.xD, point.yD, angle, length)
        fun fromPointAndDirection(point: MPoint, direction: MPoint, scale: Double = 1.0, out: MLine = MLine()): MLine = out.setTo(point.x, point.y, point.x + direction.x * scale, point.y + direction.y * scale)
        fun fromPointAngle(point: MPoint, angle: Angle, length: Double = 1.0, out: MLine = MLine()): MLine = out.setToPolar(point.x, point.y, angle, length)

        fun length(Ax: Double, Ay: Double, Bx: Double, By: Double): Double = kotlin.math.hypot(Bx - Ax, By - Ay)

        fun getIntersectXY(Ax: Double, Ay: Double, Bx: Double, By: Double, Cx: Double, Cy: Double, Dx: Double, Dy: Double): Point? {
            val a1 = By - Ay
            val b1 = Ax - Bx
            val c1 = a1 * (Ax) + b1 * (Ay)
            val a2 = Dy - Cy
            val b2 = Cx - Dx
            val c2 = a2 * (Cx) + b2 * (Cy)
            val determinant = a1 * b2 - a2 * b1
            if (determinant.isAlmostZero()) return null
            val x = (b2 * c1 - b1 * c2) / determinant
            val y = (a1 * c2 - a2 * c1) / determinant
            //if (!x.isFinite() || !y.isFinite()) TODO()
            return Point(x, y)
        }

        fun getIntersectXY(a: Point, b: Point, c: Point, d: Point): Point? =
            getIntersectXY(a.xD, a.yD, b.xD, b.yD, c.xD, c.yD, d.xD, d.yD)
    }
}

data class LineIntersection(
    val line: MLine = MLine(),
    var intersection: Point = Point()
) {
    val normalVector: MLine = MLine()

    fun setFrom(x0: Double, y0: Double, x1: Double, y1: Double, ix: Double, iy: Double, normalLength: Double) {
        line.setTo(x0, y0, x1, y1)
        intersection = Point(ix, iy)
        normalVector.setToPolar(ix, iy, line.angle - 90.degrees, normalLength)
    }

    override fun toString(): String = "LineIntersection($line, intersection=$intersection)"
}


fun MLine.Companion.projectedPoint(
    v1x: Double,
    v1y: Double,
    v2x: Double,
    v2y: Double,
    px: Double,
    py: Double,
): Point {
    // return this.getIntersectionPoint(Line(point, Point.fromPolar(point, this.angle + 90.degrees)))!!
    // get dot product of e1, e2
    val e1x = v2x - v1x
    val e1y = v2y - v1y
    val e2x = px - v1x
    val e2y = py - v1y
    val valDp = MPoint.dot(e1x, e1y, e2x, e2y)
    // get length of vectors

    val lenLineE1 = kotlin.math.hypot(e1x, e1y)
    val lenLineE2 = kotlin.math.hypot(e2x, e2y)

    // What happens if lenLineE1 or lenLineE2 are zero?, it would be a division by zero.
    // Does that mean that the point is on the line, and we should use it?
    if (lenLineE1 == 0.0 || lenLineE2 == 0.0) {
        return Point(px, py)
    }

    val cos = valDp / (lenLineE1 * lenLineE2)

    // length of v1P'
    val projLenOfLine = cos * lenLineE2

    return Point((v1x + (projLenOfLine * e1x) / lenLineE1), (v1y + (projLenOfLine * e1y) / lenLineE1))
}

fun MLine.Companion.projectedPoint(v1: Point, v2: Point, point: Point): Point = projectedPoint(v1.xD, v1.yD, v2.xD, v2.yD, point.xD, point.yD)

fun MLine.Companion.lineIntersectionPoint(l1: MLine, l2: MLine): Point? = l1.getLineIntersectionPoint(l2)

fun MLine.Companion.segmentIntersectionPoint(
    l1: MLine,
    l2: MLine,
): Point? = l1.getSegmentIntersectionPoint(l2)

// @TODO: Should we create a common interface make projectedPoint part of it? (for ecample to project other kind of shapes)
// https://math.stackexchange.com/questions/62633/orthogonal-projection-of-a-point-onto-a-line
// http://www.sunshine2k.de/coding/java/PointOnLine/PointOnLine.html
fun MLine.projectedPoint(point: Point): Point = MLine.projectedPoint(a, b, point)
