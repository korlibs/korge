@file:Suppress("DEPRECATION")

package korlibs.math.geom

import korlibs.datastructure.*
import korlibs.math.*
import korlibs.math.annotations.*
import korlibs.math.interpolation.*
import korlibs.number.*
import kotlin.math.*

@Deprecated("Use immutable BoundsBuilder instead")
class MBoundsBuilder {
    val tempRect = MRectangle()

    companion object {
        val POOL: ConcurrentPool<MBoundsBuilder> = ConcurrentPool<MBoundsBuilder>({ it.reset() }) { MBoundsBuilder() }

        private val MIN = Double.NEGATIVE_INFINITY
        private val MAX = Double.POSITIVE_INFINITY

        fun getBounds(p1: Point): Rectangle = BoundsBuilder(p1).bounds
        fun getBounds(p1: Point, p2: Point): Rectangle = BoundsBuilder(p1, p2).bounds
        fun getBounds(p1: Point, p2: Point, p3: Point): Rectangle = BoundsBuilder(p1, p2, p3).bounds
        fun getBounds(p1: Point, p2: Point, p3: Point, p4: Point): Rectangle = BoundsBuilder(p1, p2, p3, p4).bounds
    }

    var npoints = 0; private set

    /**
     * True if some points were added to the [MBoundsBuilder],
     * and thus [xmin], [xmax], [ymin], [ymax] have valid values
     **/
    val hasPoints: Boolean get() = npoints > 0

    /** Minimum value found for X. Infinity if ![hasPoints] */
    var xmin = MAX; private set
    /** Maximum value found for X. -Infinity if ![hasPoints] */
    var xmax = MIN; private set
    /** Minimum value found for Y. Infinity if ![hasPoints] */
    var ymin = MAX; private set
    /** Maximum value found for Y. -Infinity if ![hasPoints] */
    var ymax = MIN; private set

    /** Minimum value found for X. null if ![hasPoints] */
    val xminOrNull: Double? get() = if (hasPoints) xmin else null
    /** Maximum value found for X. null if ![hasPoints] */
    val xmaxOrNull: Double? get() = if (hasPoints) xmax else null
    /** Minimum value found for Y. null if ![hasPoints] */
    val yminOrNull: Double? get() = if (hasPoints) ymin else null
    /** Maximum value found for Y. null if ![hasPoints] */
    val ymaxOrNull: Double? get() = if (hasPoints) ymax else null

    /** Minimum value found for X. [default] if ![hasPoints] */
    fun xminOr(default: Double = 0.0): Double = if (hasPoints) xmin else default
    /** Maximum value found for X. [default] if ![hasPoints] */
    fun xmaxOr(default: Double = 0.0): Double = if (hasPoints) xmax else default
    /** Minimum value found for Y. [default] if ![hasPoints] */
    fun yminOr(default: Double = 0.0): Double = if (hasPoints) ymin else default
    /** Maximum value found for Y. [default] if ![hasPoints] */
    fun ymaxOr(default: Double = 0.0): Double = if (hasPoints) ymax else default

    fun isEmpty() = npoints == 0
    fun isNotEmpty() = npoints > 0

    fun reset() {
        xmin = MAX
        xmax = MIN
        ymin = MAX
        ymax = MIN
        npoints = 0
    }

    fun add(x: Double, y: Double): MBoundsBuilder {
        if (x < xmin) xmin = x
        if (x > xmax) xmax = x
        if (y < ymin) ymin = y
        if (y > ymax) ymax = y
        npoints++
        //println("add($x, $y) -> ($xmin,$ymin)-($xmax,$ymax)")
        return this
    }

    fun add(x: Int, y: Int): MBoundsBuilder = add(x.toDouble(), y.toDouble())
    fun add(x: Float, y: Float): MBoundsBuilder = add(x.toDouble(), y.toDouble())
    fun add(x: Double, y: Double, transform: MMatrix?): MBoundsBuilder = if (transform != null) add(transform.transformX(x, y), transform.transformY(x, y)) else add(x, y)
    fun add(x: Int, y: Int, transform: MMatrix?): MBoundsBuilder = add(x.toDouble(), y.toDouble(), transform)
    fun add(x: Float, y: Float, transform: MMatrix?): MBoundsBuilder = add(x.toDouble(), y.toDouble(), transform)
    fun add(p: Point, transform: MMatrix?): MBoundsBuilder = add(p.x, p.y, transform)

    fun add(point: Point): MBoundsBuilder = add(point.x, point.y)
    fun add(point: MPoint, transform: MMatrix): MBoundsBuilder = add(point.x, point.y, transform)

    fun addRect(x: Int, y: Int, width: Int, height: Int): MBoundsBuilder = addRect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
    fun addRect(x: Double, y: Double, width: Double, height: Double): MBoundsBuilder = add(x, y).add(x + width, y + height)

    fun add(ps: Iterable<MPoint>): MBoundsBuilder {
        for (p in ps) add(p.immutable)
        return this
    }
    fun add(ps: PointList): MBoundsBuilder {
        for (n in 0 until ps.size) add(ps[n])
        return this
    }

    inline fun add(rect: MRectangle?): MBoundsBuilder {
        rect?.let { addNonEmpty(rect) }
        return this
    }

    fun addNonEmpty(rect: MRectangle): MBoundsBuilder {
        if (rect.isNotEmpty) {
            addEvenEmpty(rect)
        }
        return this
    }
    fun addEvenEmpty(rect: MRectangle?): MBoundsBuilder {
        if (rect == null) return this
        add(rect.left, rect.top)
        add(rect.right, rect.bottom)
        return this
    }

    fun add(ps: Iterable<MPoint>, transform: MMatrix): MBoundsBuilder {
        for (p in ps) add(p, transform)
        return this
    }
    fun add(ps: PointList, transform: MMatrix): MBoundsBuilder {
        for (n in 0 until ps.size) add(ps.getX (n), ps.getY(n), transform)
        return this
    }
    fun add(rect: MRectangle, transform: MMatrix?): MBoundsBuilder {
        if (rect.isNotEmpty) {
            add(rect.left, rect.top, transform)
            add(rect.right, rect.top, transform)
            add(rect.right, rect.bottom, transform)
            add(rect.left, rect.bottom, transform)
        }
        return this
    }

    fun getBoundsOrNull(out: MRectangle = MRectangle()): MRectangle? = if (npoints == 0) null else out.setBounds(xmin, ymin, xmax, ymax)

    fun getBounds(out: MRectangle = MRectangle()): MRectangle {
        if (getBoundsOrNull(out) == null) {
            out.setBounds(0, 0, 0, 0)
        }
        return out
    }
}

@KormaMutableApi
@Deprecated("Use Line instead")
data class MLine(var a: Point, var b: Point) {
    fun clone(): MLine = MLine(a, b)
    fun flipped(): MLine = MLine(b, a)

    val minX: Double get() = kotlin.math.min(a.x, b.x)
    val maxX: Double get() = kotlin.math.max(a.x, b.x)
    val minY: Double get() = kotlin.math.min(a.y, b.y)
    val maxY: Double get() = kotlin.math.max(a.y, b.y)

    fun round(): MLine {
        a.round()
        b.round()
        return this
    }

    fun setTo(a: Point, b: Point): MLine = setTo(a.x, a.y, b.x, b.y)
    fun setTo(a: MPoint, b: MPoint): MLine = setTo(a.x, a.y, b.x, b.y)

    fun setTo(x0: Double, y0: Double, x1: Double, y1: Double): MLine {
        a = Point(x0, y0)
        b = Point(x1, y1)
        return this
    }

    fun setToPolar(x: Double, y: Double, angle: Angle, length: Double = 1.0): MLine {
        setTo(x, y, x + angle.cosine * length, y + angle.sine * length)
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
        if (l2 == 0.0) return Point.distanceSquared(p, a)
        val t = (Point.dot(p - v, w - v) / l2).clamp(0.0, 1.0)
        return Point.distance(p, v + (w - v) * t)
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

    val x0: Double get() = a.x
    val y0: Double get() = a.y
    val x1: Double get() = b.x
    val y1: Double get() = b.y

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
            if (this.containsBoundsXY(out.x, out.y) && line.containsBoundsXY(out.x, out.y)) {
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
            out.setTo(point.x, point.y, point.x + direction.x * scale, point.y + direction.y * scale)
        fun fromPointAngle(point: Point, angle: Angle, length: Double = 1.0, out: MLine = MLine()): MLine = out.setToPolar(point.x, point.y, angle, length)
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
            getIntersectXY(a.x, a.y, b.x, b.y, c.x, c.y, d.x, d.y)
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

fun MLine.Companion.projectedPoint(v1: Point, v2: Point, point: Point): Point = projectedPoint(v1.x, v1.y, v2.x, v2.y, point.x, point.y)

fun MLine.Companion.lineIntersectionPoint(l1: MLine, l2: MLine): Point? = l1.getLineIntersectionPoint(l2)

fun MLine.Companion.segmentIntersectionPoint(
    l1: MLine,
    l2: MLine,
): Point? = l1.getSegmentIntersectionPoint(l2)

// @TODO: Should we create a common interface make projectedPoint part of it? (for ecample to project other kind of shapes)
// https://math.stackexchange.com/questions/62633/orthogonal-projection-of-a-point-onto-a-line
// http://www.sunshine2k.de/coding/java/PointOnLine/PointOnLine.html
fun MLine.projectedPoint(point: Point): Point = MLine.projectedPoint(a, b, point)

val MMatrix?.immutable: Matrix get() = if (this == null) Matrix.NIL else Matrix(a, b, c, d, tx, ty)

@Deprecated("", ReplaceWith("this")) val Matrix.immutable: Matrix get() = this
val Matrix.mutable: MMatrix get() = MMatrix(a, b, c, d, tx, ty)
@Deprecated("")
val Matrix.mutableOrNull: MMatrix? get() = if (isNIL) null else MMatrix(a, b, c, d, tx, ty)


@KormaMutableApi
@Deprecated("Use Matrix")
data class MMatrix(
    var a: Double = 1.0,
    var b: Double = 0.0,
    var c: Double = 0.0,
    var d: Double = 1.0,
    var tx: Double = 0.0,
    var ty: Double = 0.0
) : MutableInterpolable<MMatrix>, Interpolable<MMatrix> {
    val immutable: Matrix get() = Matrix(a, b, c, d, tx, ty)

    companion object {
        val POOL: ConcurrentPool<MMatrix> = ConcurrentPool<MMatrix>({ it.identity() }) { MMatrix() }

        inline operator fun invoke(a: Float, b: Float = 0f, c: Float = 0f, d: Float = 1f, tx: Float = 0f, ty: Float = 0f) =
            MMatrix(a.toDouble(), b.toDouble(), c.toDouble(), d.toDouble(), tx.toDouble(), ty.toDouble())

        inline operator fun invoke(a: Int, b: Int = 0, c: Int = 0, d: Int = 1, tx: Int = 0, ty: Int = 0) =
            MMatrix(a.toDouble(), b.toDouble(), c.toDouble(), d.toDouble(), tx.toDouble(), ty.toDouble())

        operator fun invoke(m: MMatrix, out: MMatrix = MMatrix()): MMatrix = out.copyFrom(m)

        @Deprecated("Use transform instead")
        fun transformXf(a: Float, b: Float, c: Float, d: Float, tx: Float, ty: Float, px: Float, py: Float): Float = a * px + c * py + tx
        @Deprecated("Use transform instead")
        fun transformYf(a: Float, b: Float, c: Float, d: Float, tx: Float, ty: Float, px: Float, py: Float): Float = d * py + b * px + ty

        fun isAlmostEquals(a: MMatrix, b: MMatrix, epsilon: Double = 0.000001): Boolean =
            a.tx.isAlmostEquals(b.tx, epsilon)
                && a.ty.isAlmostEquals(b.ty, epsilon)
                && a.a.isAlmostEquals(b.a, epsilon)
                && a.b.isAlmostEquals(b.b, epsilon)
                && a.c.isAlmostEquals(b.c, epsilon)
                && a.d.isAlmostEquals(b.d, epsilon)
    }

    fun isAlmostEquals(other: MMatrix, epsilon: Double = 0.000001): Boolean = isAlmostEquals(this, other, epsilon)

    var af: Float
        get() = a.toFloat()
        set(value) { a = value.toDouble() }

    var bf: Float
        get() = b.toFloat()
        set(value) { b = value.toDouble() }

    var cf: Float
        get() = c.toFloat()
        set(value) { c = value.toDouble() }

    var df: Float
        get() = d.toFloat()
        set(value) { d = value.toDouble() }

    var txf: Float
        get() = tx.toFloat()
        set(value) { tx = value.toDouble() }

    var tyf: Float
        get() = ty.toFloat()
        set(value) { ty = value.toDouble() }

    fun getType(): MatrixType {
        val hasRotation = b != 0.0 || c != 0.0
        val hasScale = a != 1.0 || d != 1.0
        val hasTranslation = tx != 0.0 || ty != 0.0

        return when {
            hasRotation -> MatrixType.COMPLEX
            hasScale && hasTranslation -> MatrixType.SCALE_TRANSLATE
            hasScale -> MatrixType.SCALE
            hasTranslation -> MatrixType.TRANSLATE
            else -> MatrixType.IDENTITY
        }
    }

    fun setTo(a: Double, b: Double, c: Double, d: Double, tx: Double, ty: Double): MMatrix {
        this.a = a
        this.b = b
        this.c = c
        this.d = d
        this.tx = tx
        this.ty = ty
        return this

    }
    fun setTo(a: Float, b: Float, c: Float, d: Float, tx: Float, ty: Float): MMatrix = setTo(a.toDouble(), b.toDouble(), c.toDouble(), d.toDouble(), tx.toDouble(), ty.toDouble())
    fun setTo(a: Int, b: Int, c: Int, d: Int, tx: Int, ty: Int): MMatrix = setTo(a.toDouble(), b.toDouble(), c.toDouble(), d.toDouble(), tx.toDouble(), ty.toDouble())

    fun copyTo(that: MMatrix = MMatrix()): MMatrix {
        that.copyFrom(this)
        return that
    }

    fun copyFromInverted(that: MMatrix): MMatrix {
        return invert(that)
    }

    fun copyFrom(that: Matrix): MMatrix = setTo(that.a, that.b, that.c, that.d, that.tx, that.ty)

    fun copyFrom(that: MMatrix?): MMatrix {
        if (that != null) {
            setTo(that.a, that.b, that.c, that.d, that.tx, that.ty)
        } else {
            identity()
        }
        return this
    }

    fun rotate(angle: Angle) = this.apply {
        val theta = angle.radians
        val cos = cos(theta)
        val sin = sin(theta)

        val a1 = a * cos - b * sin
        b = (a * sin + b * cos)
        a = a1

        val c1 = c * cos - d * sin
        d = (c * sin + d * cos)
        c = c1

        val tx1 = tx * cos - ty * sin
        ty = (tx * sin + ty * cos)
        tx = tx1
    }

    fun skew(skewX: Angle, skewY: Angle): MMatrix {
        val sinX = sin(skewX)
        val cosX = cos(skewX)
        val sinY = sin(skewY)
        val cosY = cos(skewY)

        return this.setTo(
            a * cosY - b * sinX,
            a * sinY + b * cosX,
            c * cosY - d * sinX,
            c * sinY + d * cosX,
            tx * cosY - ty * sinX,
            tx * sinY + ty * cosX
        )
    }

    fun setToMultiply(l: MMatrix?, r: MMatrix?): MMatrix {
        when {
            l != null && r != null -> multiply(l, r)
            l != null -> copyFrom(l)
            r != null -> copyFrom(r)
            else -> identity()
        }
        return this
    }

    fun scale(sx: Double, sy: Double = sx) = setTo(a * sx, b * sx, c * sy, d * sy, tx * sx, ty * sy)
    fun scale(sx: Float, sy: Float = sx) = scale(sx.toDouble(), sy.toDouble())
    fun scale(sx: Int, sy: Int = sx) = scale(sx.toDouble(), sy.toDouble())

    fun prescale(sx: Double, sy: Double = sx) = setTo(a * sx, b * sx, c * sy, d * sy, tx, ty)
    fun prescale(sx: Float, sy: Float = sx) = prescale(sx.toDouble(), sy.toDouble())
    fun prescale(sx: Int, sy: Int = sx) = prescale(sx.toDouble(), sy.toDouble())

    fun translate(dx: Double, dy: Double) = this.apply { this.tx += dx; this.ty += dy }
    fun translate(dx: Float, dy: Float) = translate(dx.toDouble(), dy.toDouble())
    fun translate(dx: Int, dy: Int) = translate(dx.toDouble(), dy.toDouble())

    fun pretranslate(dx: Double, dy: Double) = this.apply { tx += a * dx + c * dy; ty += b * dx + d * dy }
    fun pretranslate(dx: Float, dy: Float) = pretranslate(dx.toDouble(), dy.toDouble())
    fun pretranslate(dx: Int, dy: Int) = pretranslate(dx.toDouble(), dy.toDouble())

    fun prerotate(angle: Angle) = this.apply {
        val m = MMatrix()
        m.rotate(angle)
        this.premultiply(m)
    }

    fun preskew(skewX: Angle, skewY: Angle) = this.apply {
        val m = MMatrix()
        m.skew(skewX, skewY)
        this.premultiply(m)
    }

    fun premultiply(m: Matrix) = this.premultiply(m.a, m.b, m.c, m.d, m.tx, m.ty)
    fun premultiply(m: MMatrix) = this.premultiply(m.a, m.b, m.c, m.d, m.tx, m.ty)
    fun postmultiply(m: MMatrix) = multiply(this, m)

    fun premultiply(la: Double, lb: Double, lc: Double, ld: Double, ltx: Double, lty: Double): MMatrix = setTo(
        la * a + lb * c,
        la * b + lb * d,
        lc * a + ld * c,
        lc * b + ld * d,
        ltx * a + lty * c + tx,
        ltx * b + lty * d + ty
    )
    fun premultiply(la: Float, lb: Float, lc: Float, ld: Float, ltx: Float, lty: Float): MMatrix = premultiply(la.toDouble(), lb.toDouble(), lc.toDouble(), ld.toDouble(), ltx.toDouble(), lty.toDouble())
    fun premultiply(la: Int, lb: Int, lc: Int, ld: Int, ltx: Int, lty: Int): MMatrix = premultiply(la.toDouble(), lb.toDouble(), lc.toDouble(), ld.toDouble(), ltx.toDouble(), lty.toDouble())

    fun multiply(l: MMatrix, r: MMatrix): MMatrix = setTo(
        l.a * r.a + l.b * r.c,
        l.a * r.b + l.b * r.d,
        l.c * r.a + l.d * r.c,
        l.c * r.b + l.d * r.d,
        l.tx * r.a + l.ty * r.c + r.tx,
        l.tx * r.b + l.ty * r.d + r.ty
    )

    /** Transform point without translation */
    fun deltaTransformPoint(point: MPoint, out: MPoint = MPoint()) = deltaTransformPoint(point.x, point.y, out)
    fun deltaTransformPoint(x: Float, y: Float, out: MPoint = MPoint()): MPoint = deltaTransformPoint(x.toDouble(), y.toDouble(), out)
    fun deltaTransformPoint(x: Double, y: Double, out: MPoint = MPoint()): MPoint {
        out.x = deltaTransformX(x, y)
        out.y = deltaTransformY(x, y)
        return out
    }

    fun deltaTransformX(x: Double, y: Double): Double = (x * a) + (y * c)
    fun deltaTransformY(x: Double, y: Double): Double = (x * b) + (y * d)

    fun identity() = setTo(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
    fun setToNan() = setTo(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN)

    fun isIdentity() = getType() == MatrixType.IDENTITY

    fun invert(matrixToInvert: MMatrix = this): MMatrix {
        val src = matrixToInvert
        val dst = this
        val norm = src.a * src.d - src.b * src.c

        if (norm == 0.0) {
            dst.setTo(0.0, 0.0, 0.0, 0.0, -src.tx, -src.ty)
        } else {
            val inorm = 1.0 / norm
            val d = src.a * inorm
            val a = src.d * inorm
            val b = src.b * -inorm
            val c = src.c * -inorm
            dst.setTo(a, b, c, d, -a * src.tx - c * src.ty, -b * src.tx - d * src.ty)
        }

        return this
    }

    fun concat(value: MMatrix): MMatrix = this.multiply(this, value)
    fun preconcat(value: MMatrix): MMatrix = this.multiply(this, value)
    fun postconcat(value: MMatrix): MMatrix = this.multiply(value, this)

    fun inverted(out: MMatrix = MMatrix()) = out.invert(this)

    fun setTransform(
        transform: Transform,
        pivotX: Double = 0.0,
        pivotY: Double = 0.0,
    ): MMatrix {
        return setTransform(
            transform.x, transform.y,
            transform.scaleX, transform.scaleY,
            transform.rotation, transform.skewX, transform.skewY,
            pivotX, pivotY
        )
    }

    fun setTransform(
        x: Double = 0.0,
        y: Double = 0.0,
        scaleX: Double = 1.0,
        scaleY: Double = 1.0,
        rotation: Angle = Angle.ZERO,
        skewX: Angle = Angle.ZERO,
        skewY: Angle = Angle.ZERO,
        pivotX: Double = 0.0,
        pivotY: Double = 0.0,
    ): MMatrix {
        // +0.0 drops the negative -0.0
        this.a = cos(rotation + skewY) * scaleX + 0.0
        this.b = sin(rotation + skewY) * scaleX + 0.0
        this.c = -sin(rotation - skewX) * scaleY + 0.0
        this.d = cos(rotation - skewX) * scaleY + 0.0

        if (pivotX == 0.0 && pivotY == 0.0) {
            this.tx = x
            this.ty = y
        } else {
            this.tx = x - ((pivotX * this.a) + (pivotY * this.c))
            this.ty = y - ((pivotX * this.b) + (pivotY * this.d))
        }
        return this
    }
    fun setTransform(x: Float = 0f, y: Float = 0f, scaleX: Float = 1f, scaleY: Float = 1f, rotation: Angle = Angle.ZERO, skewX: Angle = Angle.ZERO, skewY: Angle = Angle.ZERO): MMatrix =
        setTransform(x.toDouble(), y.toDouble(), scaleX.toDouble(), scaleY.toDouble(), rotation, skewX, skewY)

    fun clone(): MMatrix = MMatrix(a, b, c, d, tx, ty)

    operator fun times(that: MMatrix): MMatrix = MMatrix().multiply(this, that)
    operator fun times(scale: Double): MMatrix = MMatrix().copyFrom(this).scale(scale)

    fun toTransform(out: Transform = Transform()): Transform {
        out.setMatrixNoReturn(this)
        return out
    }

    @Suppress("DuplicatedCode")
    fun transformRectangle(rectangle: MRectangle, delta: Boolean = false) {
        val a = this.af
        val b = this.bf
        val c = this.cf
        val d = this.df
        val tx = if (delta) 0f else this.txf
        val ty = if (delta) 0f else this.tyf

        val x = rectangle.x
        val y = rectangle.y
        val xMax = x + rectangle.width
        val yMax = y + rectangle.height

        var x0 = a * x + c * y + tx
        var y0 = b * x + d * y + ty
        var x1 = a * xMax + c * y + tx
        var y1 = b * xMax + d * y + ty
        var x2 = a * xMax + c * yMax + tx
        var y2 = b * xMax + d * yMax + ty
        var x3 = a * x + c * yMax + tx
        var y3 = b * x + d * yMax + ty

        var tmp = 0.0

        if (x0 > x1) {
            tmp = x0
            x0 = x1
            x1 = tmp
        }
        if (x2 > x3) {
            tmp = x2
            x2 = x3
            x3 = tmp
        }

        rectangle.x = floor(if (x0 < x2) x0 else x2)
        rectangle.width = ceil((if (x1 > x3) x1 else x3) - rectangle.x)

        if (y0 > y1) {
            tmp = y0
            y0 = y1
            y1 = tmp
        }
        if (y2 > y3) {
            tmp = y2
            y2 = y3
            y3 = tmp
        }

        rectangle.y = floor(if (y0 < y2) y0 else y2)
        rectangle.height = ceil((if (y1 > y3) y1 else y3) - rectangle.y)
    }

    fun copyFromArray(value: FloatArray, offset: Int = 0): MMatrix = setTo(
        value[offset + 0], value[offset + 1], value[offset + 2],
        value[offset + 3], value[offset + 4], value[offset + 5]
    )

    fun copyFromArray(value: DoubleArray, offset: Int = 0): MMatrix = setTo(
        value[offset + 0].toFloat(), value[offset + 1].toFloat(), value[offset + 2].toFloat(),
        value[offset + 3].toFloat(), value[offset + 4].toFloat(), value[offset + 5].toFloat()
    )

    fun decompose(out: Transform = Transform()): Transform {
        return out.setMatrix(this)
    }


    // Transform points
    fun transform(p: Point): Point = Point(transformX(p.x, p.y), transformY(p.x, p.y))
    @Deprecated("")
    fun transform(p: MPoint, out: MPoint = MPoint()): MPoint = transform(p.x, p.y, out)
    @Deprecated("")
    fun transform(px: Double, py: Double, out: MPoint = MPoint()): MPoint = out.setTo(transformX(px, py), transformY(px, py))
    @Deprecated("")
    fun transform(px: Float, py: Float, out: MPoint = MPoint()): MPoint = out.setTo(transformX(px, py), transformY(px, py))
    @Deprecated("")
    fun transform(px: Int, py: Int, out: MPoint = MPoint()): MPoint = out.setTo(transformX(px, py), transformY(px, py))

    @Deprecated("")
    fun transformX(p: MPoint): Double = transformX(p.x, p.y)
    @Deprecated("")
    fun transformX(px: Double, py: Double): Double = this.a * px + this.c * py + this.tx
    @Deprecated("")
    fun transformX(px: Float, py: Float): Double = this.a * px + this.c * py + this.tx
    @Deprecated("")
    fun transformX(px: Int, py: Int): Double = this.a * px + this.c * py + this.tx

    @Deprecated("")
    fun transformY(p: MPoint): Double = transformY(p.x, p.y)
    @Deprecated("")
    fun transformY(px: Double, py: Double): Double = this.d * py + this.b * px + this.ty
    @Deprecated("")
    fun transformY(px: Float, py: Float): Double = this.d * py + this.b * px + this.ty
    @Deprecated("")
    fun transformY(px: Int, py: Int): Double = this.d * py + this.b * px + this.ty

    @Deprecated("")
    fun transformXf(p: MPoint): Float = transformX(p.x, p.y).toFloat()
    @Deprecated("")
    fun transformXf(px: Double, py: Double): Float = transformX(px, py).toFloat()
    @Deprecated("")
    fun transformXf(px: Float, py: Float): Float = transformX(px.toDouble(), py.toDouble()).toFloat()
    @Deprecated("")
    fun transformXf(px: Int, py: Int): Float = transformX(px.toDouble(), py.toDouble()).toFloat()

    @Deprecated("")
    fun transformYf(p: MPoint): Float = transformY(p.x, p.y).toFloat()
    @Deprecated("")
    fun transformYf(px: Double, py: Double): Float = transformY(px, py).toFloat()
    @Deprecated("")
    fun transformYf(px: Float, py: Float): Float = transformY(px.toDouble(), py.toDouble()).toFloat()
    @Deprecated("")
    fun transformYf(px: Int, py: Int): Float = transformY(px.toDouble(), py.toDouble()).toFloat()

    @Deprecated("Use MatrixTransform")
    data class Transform(
        var x: Double = 0.0, var y: Double = 0.0,
        var scaleX: Double = 1.0, var scaleY: Double = 1.0,
        var skewX: Angle = 0.radians, var skewY: Angle = 0.radians,
        var rotation: Angle = 0.radians
    ) : MutableInterpolable<Transform>, Interpolable<Transform> {
        val immutable: MatrixTransform get() = MatrixTransform(x, y, scaleX, scaleY, skewX, skewY, rotation)

        val scale: Scale get() = Scale(scaleX, scaleY)

        var scaleAvg: Double
            get() = (scaleX + scaleY) * 0.5
            set(value) {
                scaleX = value
                scaleY = value
            }

        override fun interpolateWith(ratio: Ratio, other: Transform): Transform = Transform().setToInterpolated(ratio, this, other)

        override fun setToInterpolated(ratio: Ratio, l: Transform, r: Transform): Transform = this.setTo(
            ratio.interpolate(l.x, r.x),
            ratio.interpolate(l.y, r.y),
            ratio.interpolate(l.scaleX, r.scaleX),
            ratio.interpolate(l.scaleY, r.scaleY),
            ratio.interpolateAngleDenormalized(l.rotation, r.rotation),
            ratio.interpolateAngleDenormalized(l.skewX, r.skewX),
            ratio.interpolateAngleDenormalized(l.skewY, r.skewY)
        )

        fun identity() {
            x = 0.0
            y = 0.0
            scaleX = 1.0
            scaleY = 1.0
            skewX = 0.0.radians
            skewY = 0.0.radians
            rotation = 0.0.radians
        }

        fun setMatrixNoReturn(matrix: MMatrix, pivotX: Double = 0.0, pivotY: Double = 0.0) {
            val a = matrix.a
            val b = matrix.b
            val c = matrix.c
            val d = matrix.d

            val skewX = -atan2(-c, d)
            val skewY = atan2(b, a)

            val delta = abs(skewX + skewY)

            if (delta < 0.00001 || abs((PI * 2) - delta) < 0.00001) {
                this.rotation = skewY.radians
                this.skewX = 0.0.radians
                this.skewY = 0.0.radians
            } else {
                this.rotation = 0.radians
                this.skewX = skewX.radians
                this.skewY = skewY.radians
            }

            this.scaleX = hypot(a, b)
            this.scaleY = hypot(c, d)

            if (pivotX == 0.0 && pivotY == 0.0) {
                this.x = matrix.tx
                this.y = matrix.ty
            } else {
                this.x = matrix.tx + ((pivotX * a) + (pivotY * c));
                this.y = matrix.ty + ((pivotX * b) + (pivotY * d));
            }
        }

        fun setMatrix(matrix: MMatrix, pivotX: Double = 0.0, pivotY: Double = 0.0): Transform {
            setMatrixNoReturn(matrix, pivotX, pivotY)
            return this
        }

        fun toMatrix(out: MMatrix = MMatrix()): MMatrix = out.setTransform(x, y, scaleX, scaleY, rotation, skewX, skewY)
        fun copyFrom(that: Transform) = setTo(that.x, that.y, that.scaleX, that.scaleY, that.rotation, that.skewX, that.skewY)

        fun setTo(x: Double, y: Double, scaleX: Double, scaleY: Double, rotation: Angle, skewX: Angle, skewY: Angle): Transform {
            this.x = x
            this.y = y
            this.scaleX = scaleX
            this.scaleY = scaleY
            this.rotation = rotation
            this.skewX = skewX
            this.skewY = skewY
            return this
        }
        fun setTo(x: Float, y: Float, scaleX: Float, scaleY: Float, rotation: Angle, skewX: Angle, skewY: Angle): Transform =
            setTo(x.toDouble(), y.toDouble(), scaleX.toDouble(), scaleY.toDouble(), rotation, skewX, skewY)

        fun add(value: Transform): Transform = setTo(
            x + value.x,
            y + value.y,
            scaleX * value.scaleX,
            scaleY * value.scaleY,
            skewX + value.skewX,
            skewY + value.skewY,
            rotation + value.rotation,
        )

        fun minus(value: Transform): Transform = setTo(
            x - value.x,
            y - value.y,
            scaleX / value.scaleX,
            scaleY / value.scaleY,
            skewX - value.skewX,
            skewY - value.skewY,
            rotation - value.rotation,
        )

        fun clone() = Transform().copyFrom(this)

        fun isAlmostEquals(other: Transform, epsilon: Double = 0.000001): Boolean = isAlmostEquals(this, other, epsilon)

        companion object {
            fun isAlmostEquals(a: Transform, b: Transform, epsilon: Double = 0.000001): Boolean =
                a.x.isAlmostEquals(b.x, epsilon)
                    && a.y.isAlmostEquals(b.y, epsilon)
                    && a.scaleX.isAlmostEquals(b.scaleX, epsilon)
                    && a.scaleY.isAlmostEquals(b.scaleY, epsilon)
                    && a.skewX.isAlmostEquals(b.skewX, epsilon)
                    && a.skewY.isAlmostEquals(b.skewY, epsilon)
                    && a.rotation.isAlmostEquals(b.rotation, epsilon)

        }
    }

    class Computed(val matrix: MMatrix, val transform: Transform) {
        companion object;
        constructor(matrix: MMatrix) : this(matrix, Transform().also { it.setMatrixNoReturn(matrix) })
        constructor(transform: Transform) : this(transform.toMatrix(), transform)
    }

    override fun setToInterpolated(ratio: Ratio, l: MMatrix, r: MMatrix) = this.setTo(
        a = ratio.interpolate(l.a, r.a),
        b = ratio.interpolate(l.b, r.b),
        c = ratio.interpolate(l.c, r.c),
        d = ratio.interpolate(l.d, r.d),
        tx = ratio.interpolate(l.tx, r.tx),
        ty = ratio.interpolate(l.ty, r.ty)
    )

    override fun interpolateWith(ratio: Ratio, other: MMatrix): MMatrix =
        MMatrix().setToInterpolated(ratio, this, other)

    inline fun <T> keepMatrix(callback: (MMatrix) -> T): T {
        val a = this.a
        val b = this.b
        val c = this.c
        val d = this.d
        val tx = this.tx
        val ty = this.ty
        try {
            return callback(this)
        } finally {
            this.a = a
            this.b = b
            this.c = c
            this.d = d
            this.tx = tx
            this.ty = ty
        }
    }

    override fun toString(): String = "Matrix(a=$a, b=$b, c=$c, d=$d, tx=$tx, ty=$ty)"
}

@Deprecated("Use Matrix4 instead")
typealias MMatrix3D = MMatrix4

// Stored as four consecutive column vectors (effectively stored in column-major order) see https://en.wikipedia.org/wiki/Row-_and_column-major_order
@KormaMutableApi
@Deprecated("Use Matrix4 instead")
class MMatrix4 {
    val data: FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f, // column-0
        0f, 1f, 0f, 0f, // column-1
        0f, 0f, 1f, 0f, // column-2
        0f, 0f, 0f, 1f  // column-3
    )

    operator fun set(row: Int, column: Int, value: Float) = setIndex(MMatrix4.columnMajorIndex(row, column), value)
    operator fun set(row: Int, column: Int, value: Double) = this.set(row, column, value.toFloat())
    operator fun set(row: Int, column: Int, value: Int) = this.set(row, column, value.toFloat())

    operator fun get(row: Int, column: Int): Float = getIndex(MMatrix4.columnMajorIndex(row, column))

    fun copyToFloatWxH(out: FloatArray, rows: Int, columns: Int, order: MatrixMajorOrder) {
        copyToFloatWxH(out, rows, columns, order, 0)
    }

    fun copyToFloatWxH(out: FloatArray, rows: Int, columns: Int, order: MatrixMajorOrder, offset: Int) {
        var n = offset
        if (order == MatrixMajorOrder.ROW) {
            for (column in 0 until columns) for (row in 0 until rows) out[n++] = getIndex(MMatrix4.rowMajorIndex(row, column))
        } else {
            for (column in 0 until columns) for (row in 0 until rows) out[n++] = getIndex(MMatrix4.columnMajorIndex(row, column))
        }
    }

    fun copyToFloat2x2(out: FloatArray, order: MatrixMajorOrder) = copyToFloatWxH(out, 2, 2, order, 0)
    fun copyToFloat3x3(out: FloatArray, order: MatrixMajorOrder) = copyToFloatWxH(out, 3, 3, order, 0)
    fun copyToFloat4x4(out: FloatArray, order: MatrixMajorOrder) = copyToFloatWxH(out, 4, 4, order, 0)

    fun copyToFloat2x2(out: FloatArray, order: MatrixMajorOrder, offset: Int) = copyToFloatWxH(out, 2, 2, order, offset)
    fun copyToFloat3x3(out: FloatArray, order: MatrixMajorOrder, offset: Int) = copyToFloatWxH(out, 3, 3, order, offset)
    fun copyToFloat4x4(out: FloatArray, order: MatrixMajorOrder, offset: Int) = copyToFloatWxH(out, 4, 4, order, offset)

    companion object {
        const val M00 = 0
        const val M10 = 1
        const val M20 = 2
        const val M30 = 3

        const val M01 = 4
        const val M11 = 5
        const val M21 = 6
        const val M31 = 7

        const val M02 = 8
        const val M12 = 9
        const val M22 = 10
        const val M32 = 11

        const val M03 = 12
        const val M13 = 13
        const val M23 = 14
        const val M33 = 15

        val INDICES_BY_COLUMNS_4x4 = intArrayOf(
            M00, M10, M20, M30,
            M01, M11, M21, M31,
            M02, M12, M22, M32,
            M03, M13, M23, M33,
        )
        val INDICES_BY_ROWS_4x4 = intArrayOf(
            M00, M01, M02, M03,
            M10, M11, M12, M13,
            M20, M21, M22, M23,
            M30, M31, M32, M33,
        )
        val INDICES_BY_COLUMNS_3x3 = intArrayOf(
            M00, M10, M20,
            M01, M11, M21,
            M02, M12, M22,
        )
        val INDICES_BY_ROWS_3x3 = intArrayOf(
            M00, M01, M02,
            M10, M11, M12,
            M20, M21, M22,
        )
        operator fun invoke(m: MMatrix4) = MMatrix4().copyFrom(m)

        fun fromRows(
            a00: Double, a01: Double, a02: Double, a03: Double,
            a10: Double, a11: Double, a12: Double, a13: Double,
            a20: Double, a21: Double, a22: Double, a23: Double,
            a30: Double, a31: Double, a32: Double, a33: Double
        ): MMatrix4 = MMatrix4().setRows(
            a00.toFloat(), a01.toFloat(), a02.toFloat(), a03.toFloat(),
            a10.toFloat(), a11.toFloat(), a12.toFloat(), a13.toFloat(),
            a20.toFloat(), a21.toFloat(), a22.toFloat(), a23.toFloat(),
            a30.toFloat(), a31.toFloat(), a32.toFloat(), a33.toFloat()
        )
        fun fromRows(
            a00: Float, a01: Float, a02: Float, a03: Float,
            a10: Float, a11: Float, a12: Float, a13: Float,
            a20: Float, a21: Float, a22: Float, a23: Float,
            a30: Float, a31: Float, a32: Float, a33: Float
        ): MMatrix4 = MMatrix4().setRows(
            a00, a01, a02, a03,
            a10, a11, a12, a13,
            a20, a21, a22, a23,
            a30, a31, a32, a33
        )

        fun fromColumns(
            a00: Double, a10: Double, a20: Double, a30: Double,
            a01: Double, a11: Double, a21: Double, a31: Double,
            a02: Double, a12: Double, a22: Double, a32: Double,
            a03: Double, a13: Double, a23: Double, a33: Double
        ): MMatrix4 = MMatrix4().setColumns(
            a00.toFloat(), a10.toFloat(), a20.toFloat(), a30.toFloat(),
            a01.toFloat(), a11.toFloat(), a21.toFloat(), a31.toFloat(),
            a02.toFloat(), a12.toFloat(), a22.toFloat(), a32.toFloat(),
            a03.toFloat(), a13.toFloat(), a23.toFloat(), a33.toFloat()
        )
        fun fromColumns(
            a00: Float, a10: Float, a20: Float, a30: Float,
            a01: Float, a11: Float, a21: Float, a31: Float,
            a02: Float, a12: Float, a22: Float, a32: Float,
            a03: Float, a13: Float, a23: Float, a33: Float
        ): MMatrix4 = MMatrix4().setColumns(
            a00, a10, a20, a30,
            a01, a11, a21, a31,
            a02, a12, a22, a32,
            a03, a13, a23, a33
        )

        fun fromRows3x3(
            a00: Double, a01: Double, a02: Double,
            a10: Double, a11: Double, a12: Double,
            a20: Double, a21: Double, a22: Double
        ): MMatrix4 = MMatrix4().setRows3x3(
            a00.toFloat(), a01.toFloat(), a02.toFloat(),
            a10.toFloat(), a11.toFloat(), a12.toFloat(),
            a20.toFloat(), a21.toFloat(), a22.toFloat()
        )
        fun fromRows3x3(
            a00: Float, a01: Float, a02: Float,
            a10: Float, a11: Float, a12: Float,
            a20: Float, a21: Float, a22: Float
        ): MMatrix4 = MMatrix4().setRows3x3(
            a00, a01, a02,
            a10, a11, a12,
            a20, a21, a22
        )

        fun fromColumns3x3(
            a00: Double, a10: Double, a20: Double,
            a01: Double, a11: Double, a21: Double,
            a02: Double, a12: Double, a22: Double
        ): MMatrix4 = MMatrix4().setColumns3x3(
            a00.toFloat(), a10.toFloat(), a20.toFloat(),
            a01.toFloat(), a11.toFloat(), a21.toFloat(),
            a02.toFloat(), a12.toFloat(), a22.toFloat()
        )
        fun fromColumns3x3(
            a00: Float, a10: Float, a20: Float,
            a01: Float, a11: Float, a21: Float,
            a02: Float, a12: Float, a22: Float
        ): MMatrix4 = MMatrix4().setColumns3x3(
            a00, a10, a20,
            a01, a11, a21,
            a02, a12, a22
        )

        fun fromRows2x2(
            a00: Double, a01: Double,
            a10: Double, a11: Double
        ): MMatrix4 = MMatrix4().setRows2x2(
            a00.toFloat(), a01.toFloat(),
            a10.toFloat(), a11.toFloat()
        )
        fun fromRows2x2(
            a00: Float, a01: Float,
            a10: Float, a11: Float
        ): MMatrix4 = MMatrix4().setRows2x2(
            a00, a01,
            a10, a11
        )

        fun fromColumns2x2(
            a00: Double, a10: Double,
            a01: Double, a11: Double
        ): MMatrix4 = MMatrix4().setColumns2x2(
            a00.toFloat(), a10.toFloat(),
            a01.toFloat(), a11.toFloat()
        )
        fun fromColumns2x2(
            a00: Float, a10: Float,
            a01: Float, a11: Float
        ): MMatrix4 = MMatrix4().setColumns2x2(
            a00, a10,
            a01, a11
        )

        fun rowMajorIndex(row: Int, column: Int) = row * 4 + column
        fun columnMajorIndex(row: Int, column: Int) = column * 4 + row
        fun index(row: Int, column: Int, order: MatrixMajorOrder) = if (order == MatrixMajorOrder.ROW) rowMajorIndex(row, column) else columnMajorIndex(row, column)

        fun multiply(left: FloatArray, right: FloatArray, out: FloatArray = FloatArray(16)): FloatArray {
            for (row in 0 until 4) {
                for (column in 0 until 4) {
                    var value = 0f
                    for (n in 0 until 4) {
                        value += left[columnMajorIndex(row, n)] * right[columnMajorIndex(n, column)]
                    }
                    out[columnMajorIndex(row, column)] = value
                }
            }
            return out
        }
    }

    fun setIndex(index: Int, value: Float) { data[index] = value }
    fun getIndex(index: Int): Float = data[index]

    var v00: Float get() = data[M00]; set(v) { data[M00] = v }
    var v01: Float get() = data[M01]; set(v) { data[M01] = v }
    var v02: Float get() = data[M02]; set(v) { data[M02] = v }
    var v03: Float get() = data[M03]; set(v) { data[M03] = v }

    var v10: Float get() = data[M10]; set(v) { data[M10] = v }
    var v11: Float get() = data[M11]; set(v) { data[M11] = v }
    var v12: Float get() = data[M12]; set(v) { data[M12] = v }
    var v13: Float get() = data[M13]; set(v) { data[M13] = v }

    var v20: Float get() = data[M20]; set(v) { data[M20] = v }
    var v21: Float get() = data[M21]; set(v) { data[M21] = v }
    var v22: Float get() = data[M22]; set(v) { data[M22] = v }
    var v23: Float get() = data[M23]; set(v) { data[M23] = v }

    var v30: Float get() = data[M30]; set(v) { data[M30] = v }
    var v31: Float get() = data[M31]; set(v) { data[M31] = v }
    var v32: Float get() = data[M32]; set(v) { data[M32] = v }
    var v33: Float get() = data[M33]; set(v) { data[M33] = v }

    val transposed: MMatrix4 get() = this.clone().transpose()

    fun transpose(): MMatrix4 = setColumns(
        v00, v01, v02, v03,
        v10, v11, v12, v13,
        v20, v21, v22, v23,
        v30, v31, v32, v33
    )

    fun setRows(
        a00: Float, a01: Float, a02: Float, a03: Float,
        a10: Float, a11: Float, a12: Float, a13: Float,
        a20: Float, a21: Float, a22: Float, a23: Float,
        a30: Float, a31: Float, a32: Float, a33: Float
    ): MMatrix4 = this.apply {
        v00 = a00; v01 = a01; v02 = a02; v03 = a03
        v10 = a10; v11 = a11; v12 = a12; v13 = a13
        v20 = a20; v21 = a21; v22 = a22; v23 = a23
        v30 = a30; v31 = a31; v32 = a32; v33 = a33
    }

    fun setColumns(
        a00: Float, a10: Float, a20: Float, a30: Float,
        a01: Float, a11: Float, a21: Float, a31: Float,
        a02: Float, a12: Float, a22: Float, a32: Float,
        a03: Float, a13: Float, a23: Float, a33: Float
    ): MMatrix4 {
        v00 = a00; v01 = a01; v02 = a02; v03 = a03
        v10 = a10; v11 = a11; v12 = a12; v13 = a13
        v20 = a20; v21 = a21; v22 = a22; v23 = a23
        v30 = a30; v31 = a31; v32 = a32; v33 = a33
        return this
    }

    fun setColumns2x2(
        a00: Double, a10: Double,
        a01: Double, a11: Double
    ): MMatrix4 = setColumns(
        a00.toFloat(), a10.toFloat(), 0f, 0f,
        a01.toFloat(), a11.toFloat(), 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )
    fun setColumns2x2(
        a00: Float, a10: Float,
        a01: Float, a11: Float
    ): MMatrix4 = setColumns(
        a00, a10, 0f, 0f,
        a01, a11, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    fun setColumns4x4(f: FloatArray, offset: Int = 0): MMatrix4 = setColumns(
        f[offset + 0], f[offset + 1], f[offset + 2], f[offset + 3],
        f[offset + 4], f[offset + 5], f[offset + 6], f[offset + 7],
        f[offset + 8], f[offset + 9], f[offset + 10], f[offset + 11],
        f[offset + 12], f[offset + 13], f[offset + 14], f[offset + 15]
    )

    fun setRows4x4(f: FloatArray, offset: Int = 0): MMatrix4 = setRows(
        f[offset + 0], f[offset + 1], f[offset + 2], f[offset + 3],
        f[offset + 4], f[offset + 5], f[offset + 6], f[offset + 7],
        f[offset + 8], f[offset + 9], f[offset + 10], f[offset + 11],
        f[offset + 12], f[offset + 13], f[offset + 14], f[offset + 15]
    )

    fun setColumns3x3(f: FloatArray, offset: Int = 0): MMatrix4 = setColumns(
        f[offset + 0], f[offset + 1], f[offset + 2], 0f,
        f[offset + 3], f[offset + 4], f[offset + 5], 0f,
        f[offset + 6], f[offset + 7], f[offset + 8], 0f,
        0f, 0f, 0f, 1f
    )

    fun setRows3x3(f: FloatArray, offset: Int = 0) = setRows(
        f[offset + 0], f[offset + 1], f[offset + 2], 0f,
        f[offset + 3], f[offset + 4], f[offset + 5], 0f,
        f[offset + 6], f[offset + 7], f[offset + 8], 0f,
        0f, 0f, 0f, 1f
    )

    fun setColumns2x2(f: FloatArray, offset: Int = 0) = setColumns(
        f[offset + 0], f[offset + 1], 0f, 0f,
        f[offset + 1], f[offset + 2], 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    fun setRows2x2(f: FloatArray, offset: Int = 0) = setRows(
        f[offset + 0], f[offset + 1], 0f, 0f,
        f[offset + 1], f[offset + 2], 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    fun setRow(row: Int, a: Float, b: Float, c: Float, d: Float): MMatrix4 {
        data[columnMajorIndex(row, 0)] = a
        data[columnMajorIndex(row, 1)] = b
        data[columnMajorIndex(row, 2)] = c
        data[columnMajorIndex(row, 3)] = d
        return this
    }
    fun setRow(row: Int, a: Double, b: Double, c: Double, d: Double): MMatrix4 = setRow(row, a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat())
    fun setRow(row: Int, a: Int, b: Int, c: Int, d: Int): MMatrix4 = setRow(row, a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat())
    fun setRow(row: Int, data: FloatArray): MMatrix4 = setRow(row, data[0], data[1], data[2], data[3])
    fun setRow(row: Int, data: MVector4): MMatrix4 = setRow(row, data.x, data.y, data.w, data.z)

    fun setColumn(column: Int, a: Float, b: Float, c: Float, d: Float): MMatrix4 {
        data[columnMajorIndex(0, column)] = a
        data[columnMajorIndex(1, column)] = b
        data[columnMajorIndex(2, column)] = c
        data[columnMajorIndex(3, column)] = d
        return this
    }
    fun setColumn(column: Int, a: Double, b: Double, c: Double, d: Double): MMatrix4 = setColumn(column, a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat())
    fun setColumn(column: Int, a: Int, b: Int, c: Int, d: Int): MMatrix4 = setColumn(column, a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat())
    fun setColumn(column: Int, data: FloatArray): MMatrix4 = setColumn(column, data[0], data[1], data[2], data[3])
    fun setColumn(column: Int, data: MVector4): MMatrix4 = setColumn(column, data.x, data.y, data.w, data.z)

    fun getRow(n: Int, target: FloatArray = FloatArray(4)): FloatArray {
        val m = n * 4
        target[0] = data[m + 0]
        target[1] = data[m + 1]
        target[2] = data[m + 2]
        target[3] = data[m + 3]
        return target
    }

    fun getColumn(n: Int, target: FloatArray = FloatArray(4)): FloatArray {
        target[0] = data[n + 0]
        target[1] = data[n + 4]
        target[2] = data[n + 8]
        target[3] = data[n + 12]
        return target
    }

    fun getRowVector(n: Int, target: MVector4 = MVector4()): MVector4 {
        val m = n * 4
        target.x = data[m + 0]
        target.y = data[m + 1]
        target.z = data[m + 2]
        target.w = data[m + 3]
        return target
    }

    fun getColumnVector(n: Int, target: MVector4 = MVector4()): MVector4 {
        target.x = data[n + 0]
        target.y = data[n + 4]
        target.z = data[n + 8]
        target.w = data[n + 12]
        return target
    }

    val determinant: Float get() = 0f +
        (v30 * v21 * v12 * v03) -
        (v20 * v31 * v12 * v03) -
        (v30 * v11 * v22 * v03) +
        (v10 * v31 * v22 * v03) +
        (v20 * v11 * v32 * v03) -
        (v10 * v21 * v32 * v03) -
        (v30 * v21 * v02 * v13) +
        (v20 * v31 * v02 * v13) +
        (v30 * v01 * v22 * v13) -
        (v00 * v31 * v22 * v13) -
        (v20 * v01 * v32 * v13) +
        (v00 * v21 * v32 * v13) +
        (v30 * v11 * v02 * v23) -
        (v10 * v31 * v02 * v23) -
        (v30 * v01 * v12 * v23) +
        (v00 * v31 * v12 * v23) +
        (v10 * v01 * v32 * v23) -
        (v00 * v11 * v32 * v23) -
        (v20 * v11 * v02 * v33) +
        (v10 * v21 * v02 * v33) +
        (v20 * v01 * v12 * v33) -
        (v00 * v21 * v12 * v33) -
        (v10 * v01 * v22 * v33) +
        (v00 * v11 * v22 * v33)

    val determinant3x3: Float get() = 0f +
        (v00 * v11 * v22) +
        (v01 * v12 * v20) +
        (v02 * v10 * v21) -
        (v00 * v12 * v21) -
        (v01 * v10 * v22) -
        (v02 * v11 * v20)

    fun identity(): MMatrix4 = this.setColumns(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    fun setToTranslation(x: Float, y: Float, z: Float, w: Float = 1f): MMatrix4 = this.setRows(
        1f, 0f, 0f, x,
        0f, 1f, 0f, y,
        0f, 0f, 1f, z,
        0f, 0f, 0f, w
    )
    fun setToTranslation(x: Double, y: Double, z: Double, w: Double = 1.0) = setToTranslation(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
    fun setToTranslation(x: Int, y: Int, z: Int, w: Int = 1) = setToTranslation(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

    fun setToScale(x: Float, y: Float, z: Float, w: Float = 1f): MMatrix4 = this.setRows(
        x, 0f, 0f, 0f,
        0f, y, 0f, 0f,
        0f, 0f, z, 0f,
        0f, 0f, 0f, w
    )
    fun setToScale(x: Double, y: Double, z: Double, w: Double = 1.0) = setToScale(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
    fun setToScale(x: Int, y: Int, z: Int, w: Int = 1) = setToScale(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

    fun setToShear(x: Float, y: Float, z: Float): MMatrix4 = this.setRows(
        1f, y, z, 0f,
        x, 1f, z, 0f,
        x, y, 1f, 0f,
        0f, 0f, 0f, 1f
    )
    fun setToShear(x: Double, y: Double, z: Double) = setToShear(x.toFloat(), y.toFloat(), z.toFloat())
    fun setToShear(x: Int, y: Int, z: Int) = setToShear(x.toFloat(), y.toFloat(), z.toFloat())

    fun setToRotationX(angle: Angle): MMatrix4 {
        val c = cos(angle).toFloat()
        val s = sin(angle).toFloat()
        return this.setRows(
            1f, 0f, 0f, 0f,
            0f, c, - s, 0f,
            0f, s, c, 0f,
            0f, 0f, 0f, 1f
        )
    }

    fun setToRotationY(angle: Angle): MMatrix4 {
        val c = cos(angle).toFloat()
        val s = sin(angle).toFloat()
        return this.setRows(
            c, 0f, s, 0f,
            0f, 1f, 0f, 0f,
            - s, 0f, c, 0f,
            0f, 0f, 0f, 1f
        )
    }

    fun setToRotationZ(angle: Angle): MMatrix4 {
        val c = cos(angle).toFloat()
        val s = sin(angle).toFloat()
        return this.setRows(
            c, - s, 0f, 0f,
            s, c, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )
    }

    fun setToRotation(angle: Angle, x: Float, y: Float, z: Float): MMatrix4 {
        val mag = sqrt(x * x + y * y + z * z)
        val norm = 1.0 / mag

        val nx = x * norm
        val ny = y * norm
        val nz = z * norm
        val c = cos(angle)
        val s = sin(angle)
        val t = 1 - c
        val tx = t * nx
        val ty = t * ny

        return this.setRows(
            tx * nx + c, tx * ny - s * nz, tx * nz + s * ny, 0.0,
            tx * ny + s * nz, ty * ny + c, ty * nz - s * nx, 0.0,
            tx * nz - s * ny, ty * nz + s * nx, t * nz * nz + c, 0.0,
            0.0, 0.0, 0.0, 1.0
        )
    }
    fun setToRotation(angle: Angle, direction: MVector4): MMatrix4 = setToRotation(angle, direction.x, direction.y, direction.z)
    fun setToRotation(angle: Angle, x: Double, y: Double, z: Double): MMatrix4 = setToRotation(angle, x.toFloat(), y.toFloat(), z.toFloat())
    fun setToRotation(angle: Angle, x: Int, y: Int, z: Int): MMatrix4 = setToRotation(angle, x.toFloat(), y.toFloat(), z.toFloat())

    fun multiply(l: MMatrix4, r: MMatrix4) = this.setRows(
        (l.v00 * r.v00) + (l.v01 * r.v10) + (l.v02 * r.v20) + (l.v03 * r.v30),
        (l.v00 * r.v01) + (l.v01 * r.v11) + (l.v02 * r.v21) + (l.v03 * r.v31),
        (l.v00 * r.v02) + (l.v01 * r.v12) + (l.v02 * r.v22) + (l.v03 * r.v32),
        (l.v00 * r.v03) + (l.v01 * r.v13) + (l.v02 * r.v23) + (l.v03 * r.v33),

        (l.v10 * r.v00) + (l.v11 * r.v10) + (l.v12 * r.v20) + (l.v13 * r.v30),
        (l.v10 * r.v01) + (l.v11 * r.v11) + (l.v12 * r.v21) + (l.v13 * r.v31),
        (l.v10 * r.v02) + (l.v11 * r.v12) + (l.v12 * r.v22) + (l.v13 * r.v32),
        (l.v10 * r.v03) + (l.v11 * r.v13) + (l.v12 * r.v23) + (l.v13 * r.v33),

        (l.v20 * r.v00) + (l.v21 * r.v10) + (l.v22 * r.v20) + (l.v23 * r.v30),
        (l.v20 * r.v01) + (l.v21 * r.v11) + (l.v22 * r.v21) + (l.v23 * r.v31),
        (l.v20 * r.v02) + (l.v21 * r.v12) + (l.v22 * r.v22) + (l.v23 * r.v32),
        (l.v20 * r.v03) + (l.v21 * r.v13) + (l.v22 * r.v23) + (l.v23 * r.v33),

        (l.v30 * r.v00) + (l.v31 * r.v10) + (l.v32 * r.v20) + (l.v33 * r.v30),
        (l.v30 * r.v01) + (l.v31 * r.v11) + (l.v32 * r.v21) + (l.v33 * r.v31),
        (l.v30 * r.v02) + (l.v31 * r.v12) + (l.v32 * r.v22) + (l.v33 * r.v32),
        (l.v30 * r.v03) + (l.v31 * r.v13) + (l.v32 * r.v23) + (l.v33 * r.v33)
    )

    fun multiply(
        lv00: Float, lv01: Float, lv02: Float, lv03: Float,
        lv10: Float, lv11: Float, lv12: Float, lv13: Float,
        lv20: Float, lv21: Float, lv22: Float, lv23: Float,
        lv30: Float, lv31: Float, lv32: Float, lv33: Float,

        rv00: Float, rv01: Float, rv02: Float, rv03: Float,
        rv10: Float, rv11: Float, rv12: Float, rv13: Float,
        rv20: Float, rv21: Float, rv22: Float, rv23: Float,
        rv30: Float, rv31: Float, rv32: Float, rv33: Float,
    ) = this.setRows(
        (lv00 * rv00) + (lv01 * rv10) + (lv02 * rv20) + (lv03 * rv30),
        (lv00 * rv01) + (lv01 * rv11) + (lv02 * rv21) + (lv03 * rv31),
        (lv00 * rv02) + (lv01 * rv12) + (lv02 * rv22) + (lv03 * rv32),
        (lv00 * rv03) + (lv01 * rv13) + (lv02 * rv23) + (lv03 * rv33),

        (lv10 * rv00) + (lv11 * rv10) + (lv12 * rv20) + (lv13 * rv30),
        (lv10 * rv01) + (lv11 * rv11) + (lv12 * rv21) + (lv13 * rv31),
        (lv10 * rv02) + (lv11 * rv12) + (lv12 * rv22) + (lv13 * rv32),
        (lv10 * rv03) + (lv11 * rv13) + (lv12 * rv23) + (lv13 * rv33),

        (lv20 * rv00) + (lv21 * rv10) + (lv22 * rv20) + (lv23 * rv30),
        (lv20 * rv01) + (lv21 * rv11) + (lv22 * rv21) + (lv23 * rv31),
        (lv20 * rv02) + (lv21 * rv12) + (lv22 * rv22) + (lv23 * rv32),
        (lv20 * rv03) + (lv21 * rv13) + (lv22 * rv23) + (lv23 * rv33),

        (lv30 * rv00) + (lv31 * rv10) + (lv32 * rv20) + (lv33 * rv30),
        (lv30 * rv01) + (lv31 * rv11) + (lv32 * rv21) + (lv33 * rv31),
        (lv30 * rv02) + (lv31 * rv12) + (lv32 * rv22) + (lv33 * rv32),
        (lv30 * rv03) + (lv31 * rv13) + (lv32 * rv23) + (lv33 * rv33)
    )

    fun multiply(
        lv00: Double, lv01: Double, lv02: Double, lv03: Double,
        lv10: Double, lv11: Double, lv12: Double, lv13: Double,
        lv20: Double, lv21: Double, lv22: Double, lv23: Double,
        lv30: Double, lv31: Double, lv32: Double, lv33: Double,

        rv00: Double, rv01: Double, rv02: Double, rv03: Double,
        rv10: Double, rv11: Double, rv12: Double, rv13: Double,
        rv20: Double, rv21: Double, rv22: Double, rv23: Double,
        rv30: Double, rv31: Double, rv32: Double, rv33: Double,
    ) = multiply(
        lv00.toFloat(), lv01.toFloat(), lv02.toFloat(), lv03.toFloat(),
        lv10.toFloat(), lv11.toFloat(), lv12.toFloat(), lv13.toFloat(),
        lv20.toFloat(), lv21.toFloat(), lv22.toFloat(), lv23.toFloat(),
        lv30.toFloat(), lv31.toFloat(), lv32.toFloat(), lv33.toFloat(),
        rv00.toFloat(), rv01.toFloat(), rv02.toFloat(), rv03.toFloat(),
        rv10.toFloat(), rv11.toFloat(), rv12.toFloat(), rv13.toFloat(),
        rv20.toFloat(), rv21.toFloat(), rv22.toFloat(), rv23.toFloat(),
        rv30.toFloat(), rv31.toFloat(), rv32.toFloat(), rv33.toFloat(),
    )

    fun multiply(scale: Float, l: MMatrix4 = this): MMatrix4 {
        for (n in 0 until 16) this.data[n] = l.data[n] * scale
        return this
    }

    fun copyFrom(that: MMatrix4): MMatrix4 {
        for (n in 0 until 16) this.data[n] = that.data[n]
        return this
    }

    fun transform0(x: Float, y: Float, z: Float, w: Float = 1f): Float = (v00 * x) + (v01 * y) + (v02 * z) + (v03 * w)
    fun transform1(x: Float, y: Float, z: Float, w: Float = 1f): Float = (v10 * x) + (v11 * y) + (v12 * z) + (v13 * w)
    fun transform2(x: Float, y: Float, z: Float, w: Float = 1f): Float = (v20 * x) + (v21 * y) + (v22 * z) + (v23 * w)
    fun transform3(x: Float, y: Float, z: Float, w: Float = 1f): Float = (v30 * x) + (v31 * y) + (v32 * z) + (v33 * w)

    /** [[THIS MATRIX]] * VECTOR */
    fun transform(x: Float, y: Float, z: Float, w: Float = 1f, out: MVector4 = MVector4(0, 0, 0, 0)): MVector4 = out.setTo(
        transform0(x, y, z, w),
        transform1(x, y, z, w),
        transform2(x, y, z, w),
        transform3(x, y, z, w)
    )

    fun transform(x: Float, y: Float, z: Float, out: MVector3 = MVector3(0, 0, 0)): MVector3 = out.setTo(
        transform0(x, y, z, 0f),
        transform1(x, y, z, 0f),
        transform2(x, y, z, 0f),
    )

    fun transform(v: MVector4, out: MVector4 = MVector4()): MVector4 = transform(v.x, v.y, v.z, v.w, out)
    fun transform(v: MVector3, out: MVector3 = MVector3()): MVector3 = transform(v.x, v.y, v.z, out)

    fun setToOrtho(left: Float, right: Float, bottom: Float, top: Float, near: Float = 0f, far: Float = 1f): MMatrix4 {
        val sx = 2f / (right - left)
        val sy = 2f / (top - bottom)
        val sz = -2f / (far - near)

        val tx = -(right + left) / (right - left)
        val ty = -(top + bottom) / (top - bottom)
        val tz = -(far + near) / (far - near)

        return setRows(
            sx, 0f, 0f, tx,
            0f, sy, 0f, ty,
            0f, 0f, sz, tz,
            0f, 0f, 0f, 1f
        )
    }

    fun setToOrtho(rect: MRectangle, near: Double = 0.0, far: Double = 1.0): MMatrix4 = setToOrtho(rect.left, rect.right, rect.bottom, rect.top, near, far)
    fun setToOrtho(rect: MRectangle, near: Float = 0f, far: Float = 1f): MMatrix4 = setToOrtho(rect.left, rect.right, rect.bottom, rect.top, near.toDouble(), far.toDouble())
    fun setToOrtho(rect: MRectangle, near: Int = 0, far: Int = 1): MMatrix4 = setToOrtho(rect.left, rect.right, rect.bottom, rect.top, near.toDouble(), far.toDouble())
    fun setToOrtho(left: Double, right: Double, bottom: Double, top: Double, near: Double, far: Double): MMatrix4 =
        setToOrtho(left.toFloat(), right.toFloat(), bottom.toFloat(), top.toFloat(), near.toFloat(), far.toFloat())
    fun setToOrtho(left: Int, right: Int, bottom: Int, top: Int, near: Int, far: Int): MMatrix4 =
        setToOrtho(left.toFloat(), right.toFloat(), bottom.toFloat(), top.toFloat(), near.toFloat(), far.toFloat())

    fun setToFrustum(left: Float, right: Float, bottom: Float, top: Float, zNear: Float = 0f, zFar: Float = 1f): MMatrix4 {
        if (zNear <= 0.0f || zFar <= zNear) {
            throw Exception("Error: Required zNear > 0 and zFar > zNear, but zNear $zNear, zFar $zFar")
        }
        if (left == right || top == bottom) {
            throw Exception("Error: top,bottom and left,right must not be equal")
        }

        val zNear2 = 2.0f * zNear
        val dx = right - left
        val dy = top - bottom
        val dz = zFar - zNear
        val A = (right + left) / dx
        val B = (top + bottom) / dy
        val C = -1.0f * (zFar + zNear) / dz
        val D = -2.0f * (zFar * zNear) / dz

        return setRows(
            zNear2 / dx, 0f, A, 0f,
            0f, zNear2 / dy, B, 0f,
            0f, 0f, C, D,
            0f, 0f, -1f, 0f
        )
    }
    fun setToFrustum(rect: MRectangle, zNear: Double = 0.0, zFar: Double = 1.0): MMatrix4 = setToFrustum(rect.left, rect.right, rect.bottom, rect.top, zNear.toDouble(), zFar.toDouble())
    fun setToFrustum(rect: MRectangle, zNear: Float = 0f, zFar: Float = 1f): MMatrix4 = setToFrustum(rect.left, rect.right, rect.bottom, rect.top, zNear.toDouble(), zFar.toDouble())
    fun setToFrustum(rect: MRectangle, zNear: Int = 0, zFar: Int = 1): MMatrix4 = setToFrustum(rect.left, rect.right, rect.bottom, rect.top, zNear.toDouble(), zFar.toDouble())

    fun setToFrustum(left: Double, right: Double, bottom: Double, top: Double, zNear: Double = 0.0, zFar: Double = 1.0): MMatrix4
        = setToFrustum(left.toFloat(), right.toFloat(), bottom.toFloat(), top.toFloat(), zNear.toFloat(), zFar.toFloat())
    fun setToFrustum(left: Int, right: Int, bottom: Int, top: Int, zNear: Int = 0, zFar: Int = 1): MMatrix4
        = setToFrustum(left.toFloat(), right.toFloat(), bottom.toFloat(), top.toFloat(), zNear.toFloat(), zFar.toFloat())


    fun setToPerspective(fovy: Angle, aspect: Float, zNear: Float, zFar: Float): MMatrix4 {
        val top = (kotlin.math.tan(fovy.radians / 2f) * zNear).toFloat()
        val bottom = -1.0f * top
        val left = aspect * bottom
        val right = aspect * top
        return setToFrustum(left, right, bottom, top, zNear, zFar)
    }
    fun setToPerspective(fovy: Angle, aspect: Double, zNear: Double, zFar: Double): MMatrix4
        = setToPerspective(fovy, aspect.toFloat(), zNear.toFloat(), zFar.toFloat())

    fun extractTranslation(out: MVector4 = MVector4()): MVector4 = getRowVector(3, out).also { it.w = 1f }

    fun extractScale(out: MVector4 = MVector4()): MVector4 {
        val x = getRowVector(0).length3
        val y = getRowVector(1).length3
        val z = getRowVector(2).length3
        return out.setTo(x, y, z, 1f)
    }

    fun extractRotation(row_normalise: Boolean = true): Quaternion {
        return this.immutable.decomposeRotation(row_normalise)
    }

    fun extractProjection(out: MVector4 = MVector4()) = this.getColumnVector(3, out)

    fun setRows(
        a00: Double, a01: Double, a02: Double, a03: Double,
        a10: Double, a11: Double, a12: Double, a13: Double,
        a20: Double, a21: Double, a22: Double, a23: Double,
        a30: Double, a31: Double, a32: Double, a33: Double
    ): MMatrix4 = setRows(
        a00.toFloat(), a01.toFloat(), a02.toFloat(), a03.toFloat(),
        a10.toFloat(), a11.toFloat(), a12.toFloat(), a13.toFloat(),
        a20.toFloat(), a21.toFloat(), a22.toFloat(), a23.toFloat(),
        a30.toFloat(), a31.toFloat(), a32.toFloat(), a33.toFloat()
    )

    fun setColumns(
        a00: Double, a10: Double, a20: Double, a30: Double,
        a01: Double, a11: Double, a21: Double, a31: Double,
        a02: Double, a12: Double, a22: Double, a32: Double,
        a03: Double, a13: Double, a23: Double, a33: Double
    ): MMatrix4 = setColumns(
        a00.toFloat(), a10.toFloat(), a20.toFloat(), a30.toFloat(),
        a01.toFloat(), a11.toFloat(), a21.toFloat(), a31.toFloat(),
        a02.toFloat(), a12.toFloat(), a22.toFloat(), a32.toFloat(),
        a03.toFloat(), a13.toFloat(), a23.toFloat(), a33.toFloat()
    )

    fun setRows3x3(
        a00: Double, a01: Double, a02: Double,
        a10: Double, a11: Double, a12: Double,
        a20: Double, a21: Double, a22: Double
    ): MMatrix4 = setRows(
        a00.toFloat(), a01.toFloat(), a02.toFloat(), 0f,
        a10.toFloat(), a11.toFloat(), a12.toFloat(), 0f,
        a20.toFloat(), a21.toFloat(), a22.toFloat(), 0f,
        0f, 0f, 0f, 1f
    )
    fun setRows3x3(
        a00: Float, a01: Float, a02: Float,
        a10: Float, a11: Float, a12: Float,
        a20: Float, a21: Float, a22: Float
    ): MMatrix4 = setRows(
        a00, a01, a02, 0f,
        a10, a11, a12, 0f,
        a20, a21, a22, 0f,
        0f, 0f, 0f, 1f
    )

    fun setColumns3x3(
        a00: Double, a10: Double, a20: Double,
        a01: Double, a11: Double, a21: Double,
        a02: Double, a12: Double, a22: Double
    ): MMatrix4 = setColumns(
        a00.toFloat(), a10.toFloat(), a20.toFloat(), 0f,
        a01.toFloat(), a11.toFloat(), a21.toFloat(), 0f,
        a02.toFloat(), a12.toFloat(), a22.toFloat(), 0f,
        0f, 0f, 0f, 1f
    )
    fun setColumns3x3(
        a00: Float, a10: Float, a20: Float,
        a01: Float, a11: Float, a21: Float,
        a02: Float, a12: Float, a22: Float
    ): MMatrix4 = setColumns(
        a00, a10, a20, 0f,
        a01, a11, a21, 0f,
        a02, a12, a22, 0f,
        0f, 0f, 0f, 1f
    )

    fun setRows2x2(
        a00: Double, a01: Double,
        a10: Double, a11: Double
    ): MMatrix4 = setRows(
        a00.toFloat(), a01.toFloat(), 0f, 0f,
        a10.toFloat(), a11.toFloat(), 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )
    fun setRows2x2(
        a00: Float, a01: Float,
        a10: Float, a11: Float
    ): MMatrix4 = setRows(
        a00, a01, 0f, 0f,
        a10, a11, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    operator fun times(that: MMatrix4): MMatrix4 = MMatrix4().multiply(this, that)
    operator fun times(value: Float): MMatrix4 = MMatrix4(this).multiply(value)
    operator fun times(value: Double): MMatrix4 = this * value.toFloat()
    operator fun times(value: Int): MMatrix4 = this * value.toFloat()

    operator fun div(value: Float): MMatrix4 = this * (1f / value)
    operator fun div(value: Double): MMatrix4 = this / value.toFloat()
    operator fun div(value: Int): MMatrix4 = this / value.toFloat()

    fun multiply(scale: Double, l: MMatrix4 = this) = multiply(scale.toFloat(), l)
    fun multiply(scale: Int, l: MMatrix4 = this) = multiply(scale.toFloat(), l)

    override fun equals(other: Any?): Boolean = (other is MMatrix4) && this.data.contentEquals(other.data)
    override fun hashCode(): Int = data.contentHashCode()

    override fun toString(): String = buildString {
        append("Matrix3D(\n")
        for (row in 0 until 4) {
            append("  [ ")
            for (col in 0 until 4) {
                if (col != 0) append(", ")
                val v = get(row, col)
                if (floor(v) == v) append(v.toInt()) else append(v)
            }
            append(" ],\n")
        }
        append(")")
    }

    fun clone(): MMatrix4 = MMatrix4().copyFrom(this)


    fun translate(x: Float, y: Float, z: Float, w: Float = 1f, temp: MMatrix4 = MMatrix4()) = this.apply {
        temp.setToTranslation(x, y, z, w)
        this.multiply(this, temp)
    }
    fun translate(x: Double, y: Double, z: Double, w: Double = 1.0, temp: MMatrix4 = MMatrix4()) = this.translate(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat(), temp)
    fun translate(x: Int, y: Int, z: Int, w: Int = 1, temp: MMatrix4 = MMatrix4()) = this.translate(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat(), temp)

    fun rotate(angle: Angle, x: Float, y: Float, z: Float, temp: MMatrix4 = MMatrix4()) = this.apply {
        temp.setToRotation(angle, x, y, z)
        this.multiply(this, temp)
    }
    fun rotate(angle: Angle, x: Double, y: Double, z: Double, temp: MMatrix4 = MMatrix4()) = this.rotate(angle, x.toFloat(), y.toFloat(), z.toFloat(), temp)
    fun rotate(angle: Angle, x: Int, y: Int, z: Int, temp: MMatrix4 = MMatrix4()) = this.rotate(angle, x.toFloat(), y.toFloat(), z.toFloat(), temp)

    fun scale(x: Float, y: Float, z: Float, w: Float = 1f, temp: MMatrix4 = MMatrix4()) = this.apply {
        temp.setToScale(x, y, z, w)
        this.multiply(this, temp)
    }

    fun scale(x: Double, y: Double, z: Double, w: Double = 1.0, temp: MMatrix4 = MMatrix4()) = this.scale(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat(), temp)
    fun scale(x: Int, y: Int, z: Int, w: Int = 1, temp: MMatrix4 = MMatrix4()) = this.scale(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat(), temp)

    fun setToRotation(quat: Quaternion) = this.apply {

        this.multiply(this, quat.toMatrix().mutable)
    }
    fun setToRotation(euler: EulerRotation) = this.apply {
        this.multiply(this, euler.toMatrix().mutable)
    }
    fun rotate(x: Angle, y: Angle, z: Angle, temp: MMatrix4 = MMatrix4()) = this.apply {
        rotate(x, 1f, 0f, 0f, temp)
        rotate(y, 0f, 1f, 0f, temp)
        rotate(z, 0f, 0f, 1f, temp)
    }
    fun rotate(euler: EulerRotation) = this.apply {
        this.multiply(this, euler.toMatrix().mutable)
    }
    fun rotate(quat: Quaternion) = this.apply {
        this.multiply(this, quat.toMatrix().mutable)
    }

    fun setToLookAt(
        eye: MVector4,
        target: MVector4,
        up: MVector4
    ): MMatrix4 {
        val tempVec1 = MVector3D()
        val tempVec2 = MVector3D()
        val tempVec3 = MVector3D()
        val z = tempVec1.sub(eye, target)
        if (z.length3Squared == 0f) z.z = 1f
        z.normalize()
        val x = tempVec2.cross(up, z)
        if (x.length3Squared == 0f) {
            when {
                kotlin.math.abs(up.z) == 1f -> z.x += 0.0001f
                else -> z.z += 0.0001f
            }
            z.normalize()
            x.cross(up, z)
        }
        x.normalize()
        val y = tempVec3.cross(z, x)
        return this.setRows(
            x.x, y.x, z.x, 0f,
            x.y, y.y, z.y, 0f,
            x.z, y.z, z.z, 0f,
            //-x.dot(eye), -y.dot(eye), -z.dot(eye), 1f // @TODO: Check why is this making other tests to fail
            0f, 0f, 0f, 1f
        )
    }

    inline fun translate(v: MVector4, temp: MMatrix4 = MMatrix4()) = translate(v.x, v.y, v.z, v.w, temp)
    inline fun rotate(angle: Angle, v: MVector4, temp: MMatrix4 = MMatrix4()) = rotate(angle, v.x, v.y, v.z, temp)
    inline fun scale(v: MVector4, temp: MMatrix4 = MMatrix4()) = scale(v.x, v.y, v.z, v.w, temp)

    fun setTRS(translation: MPosition3D, rotation: Quaternion, scale: MScale3D): MMatrix4 {
        val rx = rotation.x.toFloat()
        val ry = rotation.y.toFloat()
        val rz = rotation.z.toFloat()
        val rw = rotation.w.toFloat()

        val xt = rx + rx
        val yt = ry + ry
        val zt = rz + rz

        val xx = rx * xt
        val xy = rx * yt
        val xz = rx * zt

        val yy = ry * yt
        val yz = ry * zt
        val zz = rz * zt

        val wx = rw * xt
        val wy = rw * yt
        val wz = rw * zt

        return setRows(
            ((1 - (yy + zz)) * scale.x), ((xy - wz) * scale.y), ((xz + wy) * scale.z), translation.x,
            ((xy + wz) * scale.x), ((1 - (xx + zz)) * scale.y), ((yz - wx) * scale.z), translation.y,
            ((xz - wy) * scale.x), ((yz + wx) * scale.y), ((1 - (xx + yy)) * scale.z), translation.z,
            0f, 0f, 0f, 1f
        )
    }

    fun getTRS(position: MPosition3D, rotation: Ref<Quaternion>, scale: MScale3D): MMatrix4 = this.apply {
        val tempMat1 = MMatrix4()
        val det = determinant
        position.setTo(v03, v13, v23, 1f)
        scale.setTo(
            MVector4.length(v00, v10, v20) * det.sign,
            MVector4.length(v01, v11, v21),
            MVector4.length(v02, v12, v22), 1f)
        val invSX = 1f / scale.x
        val invSY = 1f / scale.y
        val invSZ = 1f / scale.z
        rotation.value = Quaternion.fromRotationMatrix(tempMat1.setRows(
            v00 * invSX, v01 * invSY, v02 * invSZ, v03,
            v10 * invSX, v11 * invSY, v12 * invSZ, v13,
            v20 * invSX, v21 * invSY, v22 * invSZ, v23,
            v30, v31, v32, v33
        ).immutable)
    }

    fun invert(m: MMatrix4 = this): MMatrix4 {
        val target = this
        m.apply {
            val t11 = v12 * v23 * v31 - v13 * v22 * v31 + v13 * v21 * v32 - v11 * v23 * v32 - v12 * v21 * v33 + v11 * v22 * v33
            val t12 = v03 * v22 * v31 - v02 * v23 * v31 - v03 * v21 * v32 + v01 * v23 * v32 + v02 * v21 * v33 - v01 * v22 * v33
            val t13 = v02 * v13 * v31 - v03 * v12 * v31 + v03 * v11 * v32 - v01 * v13 * v32 - v02 * v11 * v33 + v01 * v12 * v33
            val t14 = v03 * v12 * v21 - v02 * v13 * v21 - v03 * v11 * v22 + v01 * v13 * v22 + v02 * v11 * v23 - v01 * v12 * v23

            val det = v00 * t11 + v10 * t12 + v20 * t13 + v30 * t14

            if (det == 0f) {
                println("Matrix doesn't have inverse")
                return this.identity()
            }

            val detInv = 1 / det

            return target.setRows(
                t11 * detInv,
                t12 * detInv,
                t13 * detInv,
                t14 * detInv,

                (v13 * v22 * v30 - v12 * v23 * v30 - v13 * v20 * v32 + v10 * v23 * v32 + v12 * v20 * v33 - v10 * v22 * v33) * detInv,
                (v02 * v23 * v30 - v03 * v22 * v30 + v03 * v20 * v32 - v00 * v23 * v32 - v02 * v20 * v33 + v00 * v22 * v33) * detInv,
                (v03 * v12 * v30 - v02 * v13 * v30 - v03 * v10 * v32 + v00 * v13 * v32 + v02 * v10 * v33 - v00 * v12 * v33) * detInv,
                (v02 * v13 * v20 - v03 * v12 * v20 + v03 * v10 * v22 - v00 * v13 * v22 - v02 * v10 * v23 + v00 * v12 * v23) * detInv,

                (v11 * v23 * v30 - v13 * v21 * v30 + v13 * v20 * v31 - v10 * v23 * v31 - v11 * v20 * v33 + v10 * v21 * v33) * detInv,
                (v03 * v21 * v30 - v01 * v23 * v30 - v03 * v20 * v31 + v00 * v23 * v31 + v01 * v20 * v33 - v00 * v21 * v33) * detInv,
                (v01 * v13 * v30 - v03 * v11 * v30 + v03 * v10 * v31 - v00 * v13 * v31 - v01 * v10 * v33 + v00 * v11 * v33) * detInv,
                (v03 * v11 * v20 - v01 * v13 * v20 - v03 * v10 * v21 + v00 * v13 * v21 + v01 * v10 * v23 - v00 * v11 * v23) * detInv,

                (v12 * v21 * v30 - v11 * v22 * v30 - v12 * v20 * v31 + v10 * v22 * v31 + v11 * v20 * v32 - v10 * v21 * v32) * detInv,
                (v01 * v22 * v30 - v02 * v21 * v30 + v02 * v20 * v31 - v00 * v22 * v31 - v01 * v20 * v32 + v00 * v21 * v32) * detInv,
                (v02 * v11 * v30 - v01 * v12 * v30 - v02 * v10 * v31 + v00 * v12 * v31 + v01 * v10 * v32 - v00 * v11 * v32) * detInv,
                (v01 * v12 * v20 - v02 * v11 * v20 + v02 * v10 * v21 - v00 * v12 * v21 - v01 * v10 * v22 + v00 * v11 * v22) * detInv
            )
        }
    }

    inline fun setToMap(filter: (Float) -> Float) = setRows(
        filter(v00), filter(v01), filter(v02), filter(v03),
        filter(v10), filter(v11), filter(v12), filter(v13),
        filter(v20), filter(v21), filter(v22), filter(v23),
        filter(v30), filter(v31), filter(v32), filter(v33)
    )

    fun setToInterpolated(a: MMatrix4, b: MMatrix4, ratio: Double) = setColumns(
        ratio.toRatio().interpolate(a.v00, b.v00), ratio.toRatio().interpolate(a.v10, b.v10), ratio.toRatio().interpolate(a.v20, b.v20), ratio.toRatio().interpolate(a.v30, b.v30),
        ratio.toRatio().interpolate(a.v01, b.v01), ratio.toRatio().interpolate(a.v11, b.v11), ratio.toRatio().interpolate(a.v21, b.v21), ratio.toRatio().interpolate(a.v31, b.v31),
        ratio.toRatio().interpolate(a.v02, b.v02), ratio.toRatio().interpolate(a.v12, b.v12), ratio.toRatio().interpolate(a.v22, b.v22), ratio.toRatio().interpolate(a.v32, b.v32),
        ratio.toRatio().interpolate(a.v03, b.v03), ratio.toRatio().interpolate(a.v13, b.v13), ratio.toRatio().interpolate(a.v23, b.v23), ratio.toRatio().interpolate(a.v33, b.v33)
    )

    fun copyFrom(that: MMatrix): MMatrix4 = that.toMatrix4(this)
    //fun copyFrom(that: Matrix): MMatrix4 = that.toMMatrix4(this)

}

@Deprecated("")
fun MMatrix.toMatrix4(out: MMatrix4 = MMatrix3D()): MMatrix4 = out.setRows(
    a, c, 0.0, tx,
    b, d, 0.0, ty,
    0.0, 0.0, 1.0, 0.0,
    0.0, 0.0, 0.0, 1.0
)

fun Matrix.toMatrix4(): Matrix4 {
    if (this.isNIL) return Matrix4.IDENTITY
    return Matrix4.fromRows(
        a.toFloat(), c.toFloat(), 0f, tx.toFloat(),
        b.toFloat(), d.toFloat(), 0f, ty.toFloat(),
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )
}

val MMatrix4.immutable: Matrix4 get() = Matrix4.fromColumns(data)
val Matrix4.mutable: MMatrix4 get() = MMatrix4().setColumns4x4(copyToColumns(), 0)

typealias MVector2D = MPoint

@Deprecated("Allocates") val MPoint.int: MPointInt get() = MPointInt(this.x.toInt(), this.y.toInt())
@Deprecated("Allocates") val MPointInt.double: MPoint get() = MPoint(x.toDouble(), y.toDouble())

@Deprecated("")
fun Point.toMPoint(out: MPoint = MPoint()): MPoint = out.setTo(x, y)
@Deprecated("")
fun Point.mutable(out: MPoint = MPoint()): MPoint = out.setTo(x, y)
@Deprecated("")
val Point.mutable: MPoint get() = mutable()

//@Deprecated("")
//fun Point(p: MPoint): Point = Point(p.x.toFloat(), p.y.toFloat())

@KormaMutableApi
@Deprecated("Use Point instead")
data class MPoint(
    var x: Double,
    var y: Double
    //override var xf: Float,
    //override var yf: Float
) : MutableInterpolable<MPoint>, Interpolable<MPoint>, Comparable<MPoint>, IsAlmostEquals<MPoint> {
    //constructor(x: Double, y: Double) : this(x.toFloat(), y.toFloat())
    constructor(p: Point) : this(p.x, p.y)
    constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())
    constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())

    val point: Point get() = Point(x, y)

    val niceStr: String get() = "(${x.niceStr}, ${y.niceStr})"
    fun niceStr(decimalPlaces: Int): String = "(${x.niceStr(decimalPlaces)}, ${y.niceStr(decimalPlaces)})"

    val angle: Angle get() = Angle.between(0.0, 0.0, this.x, this.y)
    fun transformX(m: MMatrix?): Double = m?.transformX(this) ?: x
    fun transformY(m: MMatrix?): Double = m?.transformY(this) ?: y
    val mutable: MPoint get() = MPoint(x, y)
    val immutable: Point get() = Point(x, y)
    override fun isAlmostEquals(other: MPoint, epsilon: Double): Boolean =
        this.x.isAlmostEquals(other.x, epsilon) && this.y.isAlmostEquals(other.y, epsilon)

    fun clear() = setToZero()
    fun setToZero() = setTo(0.0, 0.0)
    fun setToOne() = setTo(1.0, 1.0)
    fun setToUp() = setTo(0.0, -1.0)
    fun setToDown() = setTo(0.0, +1.0)
    fun setToLeft() = setTo(-1.0, 0.0)
    fun setToRight() = setTo(+1.0, 0.0)

    fun floor() = setTo(floor(x), floor(y))
    fun round() = setTo(round(x), round(y))
    fun ceil() = setTo(ceil(x), ceil(y))

    fun setToRoundDecimalPlaces(places: Int) = setTo(x.roundDecimalPlaces(places), y.roundDecimalPlaces(places))
    fun setTo(x: Int, y: Int): MPoint = setTo(x.toDouble(), y.toDouble())

    fun setTo(x: Double, y: Double): MPoint {
        this.x = x
        this.y = y
        return this
    }

    fun setTo(x: Float, y: Float): MPoint {
        this.x = x.toDouble()
        this.y = y.toDouble()
        return this
    }

    fun setTo(p: Point): MPoint = setTo(p.x, p.y)

    /** Updates a point from polar coordinates determined by an [angle] and a [length]. Angle 0 is pointing to the right, and the direction is counter-clock-wise */
    fun setToPolar(angle: Angle, length: Double = 1.0): MPoint = setToPolar(0.0, 0.0, angle, length)
    fun setToPolar(base: Point, angle: Angle, length: Float = 1f): MPoint = setToPolar(base.x.toFloat(), base.y.toFloat(), angle, length)
    fun setToPolar(base: MPoint, angle: Angle, length: Double = 1.0): MPoint = setToPolar(base.x, base.y, angle, length)
    fun setToPolar(x: Double, y: Double, angle: Angle, length: Double = 1.0): MPoint = setTo(x + angle.cosine * length, y + angle.sine * length)
    fun setToPolar(x: Float, y: Float, angle: Angle, length: Float = 1f): MPoint = setTo(x + angle.cosine * length, y + angle.sine * length)

    /** Rotates the vector/point -90 degrees (not normalizing it) */
    fun setToNormal(): MPoint = setTo(-this.y, this.x)
    fun neg() = setTo(-this.x, -this.y)
    fun mul(s: Double) = setTo(this.x * s, this.y * s)
    fun mul(s: Float) = mul(s.toDouble())
    fun mul(s: Int) = mul(s.toDouble())

    fun add(p: Point) = this.setTo(x + p.x, y + p.y)
    fun add(p: MPoint) = this.setToAdd(this, p)
    fun add(x: Double, y: Double): MPoint = this.setTo(this.x + x, this.y + y)

    fun sub(p: Point) = this.setTo(x - p.x, y - p.y)
    fun sub(p: MPoint) = this.setToSub(this, p)
    fun sub(x: Double, y: Double): MPoint = this.setTo(this.x - x, this.y - y)

    fun copyFrom(that: Point) = setTo(that.x, that.y)
    fun copyFrom(that: MPoint) = setTo(that.x, that.y)

    fun setToTransform(mat: MMatrix, p: MPoint): MPoint = setToTransform(mat, p.x, p.y)
    fun setToTransform(mat: MMatrix, x: Double, y: Double): MPoint = setTo(mat.transformX(x, y), mat.transformY(x, y))

    fun setToAdd(a: MPoint, b: MPoint): MPoint = setTo(a.x + b.x, a.y + b.y)
    fun setToSub(a: MPoint, b: MPoint): MPoint = setTo(a.x - b.x, a.y - b.y)
    fun setToMul(a: MPoint, b: MPoint): MPoint = setTo(a.x * b.x, a.y * b.y)
    fun setToMul(a: MPoint, s: Double): MPoint = setTo(a.x * s, a.y * s)
    fun setToMul(a: MPoint, s: Float): MPoint = setToMul(a, s.toDouble())
    fun setToDiv(a: MPoint, b: MPoint): MPoint = setTo(a.x / b.x, a.y / b.y)
    fun setToDiv(a: MPoint, s: Double): MPoint = setTo(a.x / s, a.y / s)
    fun setToDiv(a: MPoint, s: Float): MPoint = setToDiv(a, s.toDouble())

    operator fun plusAssign(that: MPoint) { setTo(this.x + that.x, this.y + that.y) }
    operator fun minusAssign(that: MPoint) { setTo(this.x - that.x, this.y - that.y) }
    operator fun remAssign(that: MPoint) { setTo(this.x % that.x, this.y % that.y) }
    operator fun remAssign(scale: Double) { setTo(this.x % scale, this.y % scale) }
    operator fun divAssign(that: MPoint) { setTo(this.x / that.x, this.y / that.y) }
    operator fun divAssign(scale: Double) { setTo(this.x / scale, this.y / scale) }
    operator fun timesAssign(that: MPoint) { setTo(this.x * that.x, this.y * that.y) }
    operator fun timesAssign(scale: Double) { setTo(this.x * scale, this.y * scale) }

    @Deprecated("allocates") operator fun plus(that: MPoint): MPoint = MPoint(this.x + that.x, this.y + that.y)
    @Deprecated("allocates") operator fun minus(that: MPoint): MPoint = MPoint(this.x - that.x, this.y - that.y)
    @Deprecated("allocates") operator fun times(that: MPoint): MPoint = MPoint(this.x * that.x, this.y * that.y)
    @Deprecated("allocates") operator fun div(that: MPoint): MPoint = MPoint(this.x / that.x, this.y / that.y)
    @Deprecated("allocates") infix fun dot(that: MPoint): Double = this.x * that.x + this.y * that.y

    @Deprecated("allocates") operator fun times(scale: Double): MPoint = MPoint(this.x * scale, this.y * scale)
    @Deprecated("allocates") operator fun times(scale: Float): MPoint = this * scale.toDouble()
    @Deprecated("allocates") operator fun times(scale: Int): MPoint = this * scale.toDouble()

    @Deprecated("allocates") operator fun div(scale: Double): MPoint = MPoint(this.x / scale, this.y / scale)
    @Deprecated("allocates") operator fun div(scale: Float): MPoint = this / scale.toDouble()
    @Deprecated("allocates") operator fun div(scale: Int): MPoint = this / scale.toDouble()

    fun distanceTo(x: Double, y: Double): Double = hypot(x - this.x, y - this.y)
    fun distanceTo(x: Int, y: Int): Double = distanceTo(x.toDouble(), y.toDouble())
    fun distanceTo(x: Float, y: Float): Float = distanceTo(x.toDouble(), y.toDouble()).toFloat()
    fun distanceTo(that: MPoint): Double = distanceTo(that.x, that.y)

    fun angleTo(other: MPoint): Angle = Angle.between(this.x, this.y, other.x, other.y)
    fun angleTo(other: Point): Angle = Angle.between(this.x, this.y, other.x, other.y)

    fun transformed(mat: MMatrix, out: MPoint = MPoint()): MPoint = out.setToTransform(mat, this)
    operator fun get(index: Int): Double = when (index) {
        0 -> this.x; 1 -> this.y
        else -> throw IndexOutOfBoundsException("IPoint doesn't have $index component")
    }
    fun copy() = MPoint(this.x, this.y)

    @Deprecated("Allocates") val unit: MPoint get() = this / length
    val squaredLength: Double get() = (x * x) + (y * y)
    val length: Double get() = hypot(this.x, this.y)
    val magnitude: Double get() = hypot(this.x, this.y)
    @Deprecated("Allocates") val normalized: MPoint
        get() {
            val imag = 1.0 / magnitude
            return MPoint(this.x * imag, this.y * imag)
        }

    fun normalize() {
        val len = this.length
        when {
            len.isAlmostZero() -> this.setTo(0, 0)
            else -> this.setTo(this.x / len, this.y / len)
        }
    }

    @Deprecated("Allocates") override fun interpolateWith(ratio: Ratio, other: MPoint): MPoint =
        MPoint().setToInterpolated(ratio, this, other)

    override fun setToInterpolated(ratio: Ratio, l: MPoint, r: MPoint): MPoint = setToInterpolated(ratio, l.x, l.y, r.x, r.y)

    fun setToInterpolated(ratio: Ratio, lx: Double, ly: Double, rx: Double, ry: Double): MPoint =
        this.setTo(ratio.interpolate(lx, rx), ratio.interpolate(ly, ry))

    override fun compareTo(other: MPoint): Int = compare(this.x, this.y, other.x, other.y)

    fun rotate(rotation: Angle, out: MPoint = MPoint()): MPoint =
        out.setToPolar(Angle.between(0.0, 0.0, this.x, this.y) + rotation, this.length)

    override fun toString(): String = "(${this.x.niceStr}, ${this.y.niceStr})"

    @Deprecated("")
    companion object {
        @Deprecated("")
        val POOL: ConcurrentPool<MPoint> = ConcurrentPool<MPoint>({ it.setTo(0.0, 0.0) }) { MPoint() }

        @Deprecated("")
        val Zero: MPoint = MPoint(0.0, 0.0)
        @Deprecated("")
        val One: MPoint = MPoint(1.0, 1.0)
        @Deprecated("")
        val Up: MPoint = MPoint(0.0, -1.0)
        @Deprecated("")
        val Down: MPoint = MPoint(0.0, +1.0)
        @Deprecated("")
        val Left: MPoint = MPoint(-1.0, 0.0)
        @Deprecated("")
        val Right: MPoint = MPoint(+1.0, 0.0)

        //inline operator fun invoke(): Point = Point(0.0, 0.0) // @TODO: // e: java.lang.NullPointerException at org.jetbrains.kotlin.com.google.gwt.dev.js.JsAstMapper.mapFunction(JsAstMapper.java:562) (val pt = Array(1) { Point() })
        operator fun invoke(): MPoint = MPoint(0.0, 0.0)
        operator fun invoke(v: MPoint): MPoint = MPoint(v.x, v.y)
        operator fun invoke(x: Double, y: Double): MPoint = MPoint(x, y)
        operator fun invoke(x: Float, y: Float): MPoint = MPoint(x, y)
        operator fun invoke(x: Int, y: Int): MPoint = MPoint(x, y)
        operator fun invoke(xy: Int): MPoint = MPoint(xy.toDouble(), xy.toDouble())
        operator fun invoke(xy: Float): MPoint = MPoint(xy.toDouble(), xy.toDouble())
        operator fun invoke(xy: Double): MPoint = MPoint(xy, xy)
        inline operator fun invoke(x: Number, y: Number): MPoint = MPoint(x.toDouble(), y.toDouble())

        /** Constructs a point from polar coordinates determined by an [angle] and a [length]. Angle 0 is pointing to the right, and the direction is counter-clock-wise */
        inline operator fun invoke(angle: Angle, length: Double = 1.0): MPoint = fromPolar(angle, length)

        fun angleArc(a: Point, b: Point): Angle = Angle.fromRadians(acos((a.dot(b)) / (a.length * b.length)))
        fun angleArc(a: MPoint, b: MPoint): Angle = Angle.fromRadians(acos((a.dot(b)) / (a.length * b.length)))
        fun angleFull(a: MPoint, b: MPoint): Angle = Angle.between(a.immutable, b.immutable)

        fun middle(a: MPoint, b: MPoint): MPoint = MPoint((a.x + b.x) * 0.5, (a.y + b.y) * 0.5)

        /** Constructs a point from polar coordinates determined by an [angle] and a [length]. Angle 0 is pointing to the right, and the direction is counter-clock-wise */
        fun fromPolar(x: Double, y: Double, angle: Angle, length: Double = 1.0, out: MPoint = MPoint()): MPoint = out.setTo(x + angle.cosine * length, y + angle.sine * length)
        fun fromPolar(angle: Angle, length: Double = 1.0, out: MPoint = MPoint()): MPoint = fromPolar(0.0, 0.0, angle, length, out)
        fun fromPolar(base: MPoint, angle: Angle, length: Double = 1.0, out: MPoint = MPoint()): MPoint = fromPolar(base.x, base.y, angle, length, out)

        fun direction(a: MPoint, b: MPoint, out: MPoint = MPoint()): MPoint = out.setTo(b.x - a.x, b.y - a.y)
        fun middle(a: MPoint, b: MPoint, out: MPoint = MPoint()): MPoint = out.setTo((a.x + b.x) * 0.5, (a.y + b.y) * 0.5)

        fun angle(ax: Double, ay: Double, bx: Double, by: Double): Angle = Angle.between(ax, ay, bx, by)
        //acos(((ax * bx) + (ay * by)) / (hypot(ax, ay) * hypot(bx, by)))

        fun compare(l: MPoint, r: MPoint): Int = compare(l.x, l.y, r.x, r.y)
        fun compare(lx: Double, ly: Double, rx: Double, ry: Double): Int {
            val ret = ly.compareTo(ry)
            return if (ret == 0) lx.compareTo(rx) else ret
        }

        fun angle(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double): Angle = Angle.between(x1 - x2, y1 - y2, x1 - x3, y1 - y3)

        private fun square(x: Double) = x * x
        private fun square(x: Int) = x * x

        fun distanceSquared(x1: Double, y1: Double, x2: Double, y2: Double): Double = square(x1 - x2) + square(y1 - y2)
        fun distanceSquared(x1: Int, y1: Int, x2: Int, y2: Int): Int = square(x1 - x2) + square(y1 - y2)

        fun distance(a: MPoint, b: MPoint): Double = distance(a.x, a.y, b.x, b.y)
        fun distance(a: MPointInt, b: MPointInt): Double = distance(a.x, a.y, b.x, b.y)
        fun distance(a: Double, b: Double): Double = kotlin.math.abs(a - b)
        fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double = kotlin.math.hypot(x1 - x2, y1 - y2)
        fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Double = distance(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())
        fun distance(x1: Int, y1: Int, x2: Int, y2: Int): Double = distance(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())

        fun distanceSquared(a: MPoint, b: MPoint): Double = distanceSquared(a.x, a.y, b.x, b.y)
        fun distanceSquared(a: MPointInt, b: MPointInt): Int = distanceSquared(a.x, a.y, b.x, b.y)

        fun dot(aX: Double, aY: Double, bX: Double, bY: Double): Double = (aX * bX) + (aY * bY)
        fun dot(a: MPoint, b: MPoint): Double = dot(a.x, a.y, b.x, b.y)
        fun isCollinear(xa: Double, ya: Double, x: Double, y: Double, xb: Double, yb: Double): Boolean {
            return (((x - xa) / (y - ya)) - ((xa - xb) / (ya - yb))).absoluteValue.isAlmostZero()
        }

        fun isCollinear(xa: Int, ya: Int, x: Int, y: Int, xb: Int, yb: Int): Boolean = isCollinear(
            xa.toDouble(), ya.toDouble(),
            x.toDouble(), y.toDouble(),
            xb.toDouble(), yb.toDouble(),
        )

        // https://algorithmtutor.com/Computational-Geometry/Determining-if-two-consecutive-segments-turn-left-or-right/
        /** < 0 left, > 0 right, 0 collinear */
        fun orientation(p1: MPoint, p2: MPoint, p3: MPoint): Double =
            orientation(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
        fun orientation(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double): Double =
            crossProduct(cx - ax, cy - ay, bx - ax, by - ay)

        fun crossProduct(ax: Double, ay: Double, bx: Double, by: Double): Double = (ax * by) - (bx * ay)
        fun crossProduct(p1: MPoint, p2: MPoint): Double = crossProduct(p1.x, p1.y, p2.x, p2.y)

        //val ax = x1 - x2
        //val ay = y1 - y2
        //val al = hypot(ax, ay)
        //val bx = x1 - x3
        //val by = y1 - y3
        //val bl = hypot(bx, by)
        //return acos((ax * bx + ay * by) / (al * bl))
    }
}

fun List<MPoint>.getPolylineLength(): Double = getPolylineLength(size) { get(it).point }

fun List<MPoint>.bounds(out: MRectangle = MRectangle(), bb: MBoundsBuilder = MBoundsBuilder()): MRectangle = bb.add(this).getBounds(out)
fun Iterable<MPoint>.bounds(out: MRectangle = MRectangle(), bb: MBoundsBuilder = MBoundsBuilder()): MRectangle = bb.add(this).getBounds(out)

fun min(a: MPoint, b: MPoint, out: MPoint = MPoint()): MPoint = out.setTo(kotlin.math.min(a.x, b.x), kotlin.math.min(a.y, b.y))
fun max(a: MPoint, b: MPoint, out: MPoint = MPoint()): MPoint = out.setTo(kotlin.math.max(a.x, b.x), kotlin.math.max(a.y, b.y))
fun MPoint.clamp(min: Double, max: Double, out: MPoint = MPoint()): MPoint = out.setTo(x.clamp(min, max), y.clamp(min, max))

val Vector2I.mutable: MPointInt get() = MPointInt(x, y)

@KormaMutableApi
@Deprecated("Use PointInt instead")
inline class MPointInt(val p: MPoint) : Comparable<MPointInt>, MutableInterpolable<MPointInt> {
    override fun compareTo(other: MPointInt): Int = compare(this.x, this.y, other.x, other.y)

    val point: Vector2I get() = Vector2I(x, y)

    companion object {
        operator fun invoke(): MPointInt = MPointInt(0, 0)
        operator fun invoke(x: Int, y: Int): MPointInt = MPointInt(MPoint(x, y))
        operator fun invoke(that: MPointInt): MPointInt = MPointInt(MPoint(that.x, that.y))

        fun compare(lx: Int, ly: Int, rx: Int, ry: Int): Int {
            val ret = ly.compareTo(ry)
            return if (ret == 0) lx.compareTo(rx) else ret
        }
    }
    var x: Int ; set(value) { p.x = value.toDouble() } get() = p.x.toIntRound()
    var y: Int ; set(value) { p.y = value.toDouble() } get() = p.y.toIntRound()
    fun setTo(x: Int, y: Int) : MPointInt {
        this.x = x
        this.y = y
        return this
    }
    fun setTo(that: MPointInt) = this.setTo(that.x, that.y)

    operator fun plusAssign(other: MPointInt): Unit { setTo(this.x + other.x, this.y + other.y) }
    operator fun minusAssign(other: MPointInt): Unit { setTo(this.x - other.x, this.y - other.y) }
    operator fun timesAssign(other: MPointInt): Unit { setTo(this.x * other.x, this.y * other.y) }
    operator fun divAssign(other: MPointInt): Unit { setTo(this.x / other.x, this.y / other.y) }
    operator fun remAssign(other: MPointInt): Unit { setTo(this.x % other.x, this.y % other.y) }

    override fun setToInterpolated(ratio: Ratio, l: MPointInt, r: MPointInt): MPointInt =
        setTo(ratio.interpolate(l.x, r.x), ratio.interpolate(l.y, r.y))

    override fun toString(): String = "($x, $y)"
}

fun MPoint.asInt(): MPointInt = MPointInt(this)
fun MPointInt.asDouble(): MPoint = this.p

@KormaMutableApi
@Deprecated("Use Rectangle")
data class MRectangle(
    var x: Double, var y: Double,
    var width: Double, var height: Double
) : MutableInterpolable<MRectangle>, Interpolable<MRectangle>, Sizeable, MSizeable, IsAlmostEquals<MRectangle> {

    operator fun contains(that: Point) = contains(that.x, that.y)
    operator fun contains(that: MPoint) = contains(that.x, that.y)
    operator fun contains(that: MPointInt) = contains(that.x, that.y)
    fun contains(x: Double, y: Double) = (x >= left && x < right) && (y >= top && y < bottom)
    fun contains(x: Float, y: Float) = contains(x.toDouble(), y.toDouble())
    fun contains(x: Int, y: Int) = contains(x.toDouble(), y.toDouble())

    val area: Double get() = width * height
    val isEmpty: Boolean get() = width == 0.0 && height == 0.0
    val isNotEmpty: Boolean get() = width != 0.0 || height != 0.0
    val mutable: MRectangle get() = MRectangle(x, y, width, height)

    val topLeft: Point get() = Point(left, top)
    val topRight: Point get() = Point(right, top)
    val bottomLeft: Point get() = Point(left, bottom)
    val bottomRight: Point get() = Point(right, bottom)

    val center: Point get() = Point((right + left) * 0.5, (bottom + top) * 0.5)

    override fun isAlmostEquals(other: MRectangle, epsilon: Double): Boolean =
        this.x.isAlmostEquals(other.x, epsilon) &&
            this.y.isAlmostEquals(other.y, epsilon) &&
            this.width.isAlmostEquals(other.width, epsilon) &&
            this.height.isAlmostEquals(other.height, epsilon)

    /**
     * Circle that touches or contains all the corners ([topLeft], [topRight], [bottomLeft], [bottomRight]) of the rectangle.
     */
    fun outerCircle(): Circle {
        return Circle(center, Point.distance(center, topRight))
    }


    fun without(padding: Margin): MRectangle = MRectangle.fromBounds(
        left + padding.left,
        top + padding.top,
        right - padding.right,
        bottom - padding.bottom
    )

    fun with(margin: Margin): MRectangle = MRectangle.fromBounds(
        left - margin.left,
        top - margin.top,
        right + margin.right,
        bottom + margin.bottom
    )

    infix fun intersects(that: MRectangle): Boolean = intersectsX(that) && intersectsY(that)

    infix fun intersectsX(that: MRectangle): Boolean =
        that.left <= this.right && that.right >= this.left

    infix fun intersectsY(that: MRectangle): Boolean =
        that.top <= this.bottom && that.bottom >= this.top

    fun intersection(that: MRectangle, target: MRectangle = MRectangle()) =
        if (this intersects that) target.setBounds(
            kotlin.math.max(this.left, that.left), kotlin.math.max(this.top, that.top),
            kotlin.math.min(this.right, that.right), kotlin.math.min(this.bottom, that.bottom)
        ) else null

    companion object {
        val POOL: ConcurrentPool<MRectangle> = ConcurrentPool<MRectangle>({ it.clear() }) { MRectangle() }

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
        operator fun invoke(point1: MPoint, point2: MPoint): MRectangle {
            val left = minOf(point1.x, point2.x)
            val top = minOf(point1.y, point2.y)
            val right = maxOf(point1.x, point2.x)
            val bottom = maxOf(point1.y, point2.y)
            return MRectangle(left, top, right - left, bottom - top)
        }

        operator fun invoke(): MRectangle = MRectangle(0.0, 0.0, 0.0, 0.0)
        operator fun invoke(x: Int, y: Int, width: Int, height: Int): MRectangle = MRectangle(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
        operator fun invoke(x: Float, y: Float, width: Float, height: Float): MRectangle = MRectangle(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
        operator fun invoke(topLeft: MPoint, size: MSize): MRectangle = MRectangle(topLeft.x, topLeft.y, size.width, size.height)
        operator fun invoke(topLeft: Point, size: Size): MRectangle = MRectangle(topLeft.x, topLeft.y, size.width, size.height)
        fun fromBounds(left: Double, top: Double, right: Double, bottom: Double): MRectangle = MRectangle().setBounds(left, top, right, bottom)
        fun fromBounds(left: Int, top: Int, right: Int, bottom: Int): MRectangle = MRectangle().setBounds(left, top, right, bottom)
        fun fromBounds(left: Float, top: Float, right: Float, bottom: Float): MRectangle = MRectangle().setBounds(left, top, right, bottom)
        fun fromBounds(point1: MPoint, point2: MPoint): MRectangle = MRectangle(point1, point2)
        fun isContainedIn(a: MRectangle, b: MRectangle): Boolean = a.x >= b.x && a.y >= b.y && a.x + a.width <= b.x + b.width && a.y + a.height <= b.y + b.height
    }

    fun setXY(x: Double, y: Double) {
        this.x = x
        this.y = y
    }

    var left: Double ; get() = x; set(value) { width += (x - value); x = value }
    var top: Double ; get() = y; set(value) { height += (y - value); y = value }

    var right: Double ; get() = x + width ; set(value) { width = value - x }
    var bottom: Double ; get() = y + height ; set(value) { height = value - y }

    val pos: Point get() = Point(x, y)
    val mPosition: MPoint get() = MPoint(x, y)
    override val size: Size get() = Size(width, height)
    override val mSize: MSize get() = MSize(width, height)

    fun setToBounds(left: Double, top: Double, right: Double, bottom: Double): MRectangle = setTo(left, top, right - left, bottom - top)

    fun setTo(x: Double, y: Double, width: Double, height: Double): MRectangle {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
        return this
    }

    fun setTo(x: Int, y: Int, width: Int, height: Int): MRectangle = setTo(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
    fun setTo(x: Float, y: Float, width: Float, height: Float): MRectangle = setTo(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

    fun copyFrom(that: Rectangle): MRectangle = setTo(that.x, that.y, that.width, that.height)
    fun copyFrom(that: MRectangle): MRectangle = setTo(that.x, that.y, that.width, that.height)
    fun setBounds(left: Double, top: Double, right: Double, bottom: Double): MRectangle = setTo(left, top, right - left, bottom - top)
    fun setBounds(left: Int, top: Int, right: Int, bottom: Int): MRectangle = setBounds(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())
    fun setBounds(left: Float, top: Float, right: Float, bottom: Float): MRectangle = setBounds(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())

    operator fun times(scale: Double): MRectangle = MRectangle(x * scale, y * scale, width * scale, height * scale)
    operator fun times(scale: Float): MRectangle = this * scale.toDouble()
    operator fun times(scale: Int): MRectangle = this * scale.toDouble()

    operator fun div(scale: Double): MRectangle = MRectangle(x / scale, y / scale, width / scale, height / scale)
    operator fun div(scale: Float): MRectangle = this / scale.toDouble()
    operator fun div(scale: Int): MRectangle = this / scale.toDouble()

    operator fun contains(that: MRectangle) = isContainedIn(that, this)

    fun setToIntersection(a: MRectangle, b: MRectangle): MRectangle? =
        if (a.intersection(b, this) != null) this else null

    fun setToUnion(a: MRectangle, b: MRectangle): MRectangle = setToBounds(
        kotlin.math.min(a.left, b.left),
        kotlin.math.min(a.top, b.top),
        kotlin.math.max(a.right, b.right),
        kotlin.math.max(a.bottom, b.bottom)
    )

    infix fun intersection(that: MRectangle) = intersection(that, MRectangle())

    fun displaced(dx: Double, dy: Double) = MRectangle(this.x + dx, this.y + dy, width, height)
    fun displaced(dx: Float, dy: Float) = displaced(dx.toDouble(), dy.toDouble())
    fun displaced(dx: Int, dy: Int) = displaced(dx.toDouble(), dy.toDouble())

    fun displace(dx: Double, dy: Double) = setTo(this.x + dx, this.y + dy, this.width, this.height)
    fun displace(dx: Float, dy: Float) = displace(dx.toDouble(), dy.toDouble())
    fun displace(dx: Int, dy: Int) = displace(dx.toDouble(), dy.toDouble())

    fun place(item: MSize, anchor: Anchor, scale: ScaleMode, out: MRectangle = MRectangle()): MRectangle =
        place(item.width, item.height, anchor, scale, out)

    fun place(
        width: Double,
        height: Double,
        anchor: Anchor,
        scale: ScaleMode,
        out: MRectangle = MRectangle()
    ): MRectangle {
        val (ow, oh) = scale.transform(Size(width, height), Size(this.width, this.height))
        val x = (this.width - ow) * anchor.sx
        val y = (this.height - oh) * anchor.sy
        return out.setTo(x, y, ow.toDouble(), oh.toDouble())
    }

    fun inflate(
        left: Double,
        top: Double = left,
        right: Double = left,
        bottom: Double = top
    ): MRectangle = setBounds(this.left - left, this.top - top, this.right + right, this.bottom + bottom)

    fun clear(): MRectangle = setTo(0.0, 0.0, 0.0, 0.0)

    fun clone(): MRectangle = MRectangle(x, y, width, height)

    fun setToAnchoredRectangle(item: MRectangle, anchor: Anchor, container: MRectangle): MRectangle =
        setToAnchoredRectangle(item.mSize, anchor, container)

    fun setToAnchoredRectangle(item: MSize, anchor: Anchor, container: MRectangle): MRectangle =
        setToAnchoredRectangle(item.immutable, anchor, container)

    fun setToAnchoredRectangle(item: Size, anchor: Anchor, container: MRectangle): MRectangle = setTo(
        (container.x + anchor.sx * (container.width - item.width)),
        (container.y + anchor.sy * (container.height - item.height)),
        item.width,
        item.height
    )

    fun applyTransform(m: MMatrix): MRectangle {
        val tl = m.transform(left, top)
        val tr = m.transform(right, top)
        val bl = m.transform(left, bottom)
        val br = m.transform(right, bottom)

        val minX = korlibs.math.min(tl.x, tr.x, bl.x, br.x)
        val minY = korlibs.math.min(tl.y, tr.y, bl.y, br.y)
        val maxX = korlibs.math.max(tl.x, tr.x, bl.x, br.x)
        val maxY = korlibs.math.max(tl.y, tr.y, bl.y, br.y)

        //val l = m.transformX(left, top)
        //val t = m.transformY(left, top)
        //val r = m.transformX(right, bottom)
        //val b = m.transformY(right, bottom)
        return setBounds(minX, minY, maxX, maxY)
    }

    //override fun toString(): String = "Rectangle([${left.niceStr}, ${top.niceStr}]-[${right.niceStr}, ${bottom.niceStr}])"
    override fun toString(): String = "Rectangle(x=${x.niceStr}, y=${y.niceStr}, width=${width.niceStr}, height=${height.niceStr})"
    fun toStringBounds(): String = "Rectangle([${left.niceStr},${top.niceStr}]-[${right.niceStr},${bottom.niceStr}])"
    fun toStringSize(): String = "Rectangle([${left.niceStr},${top.niceStr}],[${width.niceStr},${height.niceStr}])"
    fun toStringCompat(): String = "Rectangle(x=${left.niceStr}, y=${top.niceStr}, w=${width.niceStr}, h=${height.niceStr})"

    override fun equals(other: Any?): Boolean = other is MRectangle
        && x.isAlmostEquals(other.x)
        && y.isAlmostEquals(other.y)
        && width.isAlmostEquals(other.width)
        && height.isAlmostEquals(other.height)

    override fun interpolateWith(ratio: Ratio, other: MRectangle): MRectangle =
        MRectangle().setToInterpolated(ratio, this, other)

    override fun setToInterpolated(ratio: Ratio, l: MRectangle, r: MRectangle): MRectangle =
        this.setTo(
            ratio.interpolate(l.x, r.x),
            ratio.interpolate(l.y, r.y),
            ratio.interpolate(l.width, r.width),
            ratio.interpolate(l.height, r.height)
        )

    val immutable: Rectangle get() = Rectangle(x, y, width, height)

    fun toInt(): MRectangleInt = MRectangleInt(x.toInt(), y.toInt(), width.toInt(), height.toInt())
    fun floor(): MRectangle = setTo(
        kotlin.math.floor(x),
        kotlin.math.floor(y),
        kotlin.math.floor(width),
        kotlin.math.floor(height)
    )

    fun round(): MRectangle = setTo(
        kotlin.math.round(x),
        kotlin.math.round(y),
        kotlin.math.round(width),
        kotlin.math.round(height)
    )

    fun roundDecimalPlaces(places: Int): MRectangle = setTo(
        x.roundDecimalPlaces(places),
        y.roundDecimalPlaces(places),
        width.roundDecimalPlaces(places),
        height.roundDecimalPlaces(places)
    )

    fun ceil(): MRectangle = setTo(
        kotlin.math.ceil(x),
        kotlin.math.ceil(y),
        kotlin.math.ceil(width),
        kotlin.math.ceil(height)
    )

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

    fun expand(left: Double, top: Double, right: Double, bottom: Double): MRectangle =
        this.setToBounds(this.left - left, this.top - top, this.right + right, this.bottom + bottom)

    inline fun expand(left: Number, top: Number, right: Number, bottom: Number): MRectangle =
        expand(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())

    fun expand(margin: Margin): MRectangle =
        expand(margin.left, margin.top, margin.right, margin.bottom)

    fun expand(margin: MarginInt): MRectangle =
        expand(margin.left, margin.top, margin.right, margin.bottom)

    fun toRectangle(): Rectangle = Rectangle(x, y, width, height)
    @KormaMutableApi fun asInt(): MRectangleInt = MRectangleInt(this)
    @KormaMutableApi val int: MRectangleInt get() = MRectangleInt(x, y, width, height)
    val value: Rectangle get() = Rectangle(x, y, width, height)

}

fun Rectangle.copyTo(out: MRectangle = MRectangle()): MRectangle = out.copyFrom(this)


@KormaMutableApi
fun Iterable<MRectangle>.bounds(target: MRectangle = MRectangle()): MRectangle {
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

@KormaMutableApi
@Deprecated("Use RectangleInt instead")
inline class MRectangleInt(val rect: MRectangle) {
    val immutable: RectangleInt get() = RectangleInt(x, y, width, height)

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
    operator fun contains(that: Vector2I) = contains(that.x, that.y)
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
        ((container.width - this.width) * anchor.sx).toInt(),
        ((container.height - this.height) * anchor.sy).toInt(),
        width,
        height
    )

    fun getAnchorPosition(anchor: Anchor, out: MPointInt = MPointInt()): MPointInt =
        out.setTo((x + width * anchor.sx).toInt(), (y + height * anchor.sy).toInt())

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
    val value: Rectangle get() = Rectangle(x, y, width, height)
}

@Deprecated("")
val RectangleInt.mutable: MRectangleInt get() = MRectangleInt(x, y, width, height)


@KormaMutableApi
@Deprecated("Use Size instead")
inline class MSize(val p: MPoint) : MutableInterpolable<MSize>, Interpolable<MSize>, Sizeable, MSizeable {
    companion object {
        operator fun invoke(): MSize = MSize(MPoint(0, 0))
        operator fun invoke(width: Double, height: Double): MSize = MSize(MPoint(width, height))
        operator fun invoke(width: Int, height: Int): MSize = MSize(MPoint(width, height))
        operator fun invoke(width: Float, height: Float): MSize = MSize(MPoint(width, height))
    }

    fun copy() = MSize(p.copy())

    override val size: Size get() = immutable
    override val mSize: MSize get() = this

    val area: Double get() = width * height
    val perimeter: Double get() = width * 2 + height * 2
    val min: Double get() = kotlin.math.min(width, height)
    val max: Double get() = kotlin.math.max(width, height)

    var width: Double
        set(value) { p.x = value }
        get() = p.x
    var height: Double
        set(value) { p.y = value }
        get() = p.y

    fun setTo(width: Double, height: Double): MSize {
        this.width = width
        this.height = height
        return this
    }
    fun setTo(width: Int, height: Int) = setTo(width.toDouble(), height.toDouble())
    fun setTo(width: Float, height: Float) = setTo(width.toDouble(), height.toDouble())
    fun setTo(that: MSize) = setTo(that.width, that.height)
    fun setTo(that: Size) = setTo(that.width, that.height)

    fun setToScaled(sx: Double, sy: Double) = setTo((this.width * sx), (this.height * sy))
    fun setToScaled(sx: Float, sy: Float) = setToScaled(sx.toDouble(), sy.toDouble())
    fun setToScaled(sx: Int, sy: Int) = setToScaled(sx.toDouble(), sy.toDouble())

    fun clone() = MSize(width, height)

    override fun interpolateWith(ratio: Ratio, other: MSize): MSize = MSize(0, 0).setToInterpolated(ratio, this, other)

    override fun setToInterpolated(ratio: Ratio, l: MSize, r: MSize): MSize = this.setTo(
        ratio.interpolate(l.width, r.width),
        ratio.interpolate(l.height, r.height)
    )

    override fun toString(): String = "Size(width=${width.niceStr}, height=${height.niceStr})"
}

@KormaMutableApi
@Deprecated("Use SizeInt instead")
inline class MSizeInt(val float: MSize) {
    companion object {
        operator fun invoke(): MSizeInt = MSizeInt(MSize(0, 0))
        operator fun invoke(width: Int, height: Int): MSizeInt = MSizeInt(MSize(width, height))
        operator fun invoke(that: SizeInt): MSizeInt = MSizeInt(MSize(that.width, that.height))
    }

    val immutable: SizeInt get() = SizeInt(width, height)
    fun clone(): MSizeInt = MSizeInt(float.clone())

    fun setTo(width: Int, height: Int) : MSizeInt {
        this.width = width
        this.height = height

        return this
    }

    fun setTo(that: SizeInt) = setTo(that.width, that.height)
    fun setTo(that: MSizeInt) = setTo(that.width, that.height)

    fun setToScaled(sx: Double, sy: Double) = setTo((this.width * sx).toInt(), (this.height * sy).toInt())
    fun setToScaled(sx: Int, sy: Int) = setToScaled(sx.toDouble(), sy.toDouble())
    fun setToScaled(sx: Float, sy: Float) = setToScaled(sx.toDouble(), sy.toDouble())

    fun anchoredIn(container: MRectangleInt, anchor: Anchor, out: MRectangleInt = MRectangleInt()): MRectangleInt {
        return out.setTo(
            ((container.width - this.width) * anchor.sx).toInt(),
            ((container.height - this.height) * anchor.sy).toInt(),
            width,
            height
        )
    }

    operator fun contains(v: MSizeInt): Boolean = (v.width <= width) && (v.height <= height)
    operator fun times(v: Double) = MSizeInt(MSize((width * v).toInt(), (height * v).toInt()))
    operator fun times(v: Int) = this * v.toDouble()
    operator fun times(v: Float) = this * v.toDouble()

    fun getAnchorPosition(anchor: Anchor, out: MPointInt = MPointInt(0, 0)): MPointInt =
        out.setTo((width * anchor.sx).toInt(), (height * anchor.sy).toInt())

    var width: Int
        set(value) { float.width = value.toDouble() }
        get() = float.width.toInt()
    var height: Int
        set(value) { float.height = value.toDouble() }
        get() = float.height.toInt()

    //override fun toString(): String = "SizeInt($width, $height)"
    override fun toString(): String = "SizeInt(width=$width, height=$height)"
}

interface MSizeable {
    val mSize: MSize
}

@KormaMutableApi
@Deprecated("")
fun mvec(x: Float, y: Float, z: Float): MVector3 = MVector3(x, y, z)

@KormaMutableApi
@Deprecated("")
sealed interface IVector3 {
    val x: Float
    val y: Float
    val z: Float

    operator fun get(index: Int): Float = when (index) {
        0 -> x
        1 -> y
        2 -> z
        else -> 0f
    }
}

@KormaMutableApi
@Deprecated("")
class MVector3 : IVector3 {
    val data: FloatArray = FloatArray(3)

    override var x: Float get() = data[0]; set(value) { data[0] = value }
    override var y: Float get() = data[1]; set(value) { data[1] = value }
    override var z: Float get() = data[2]; set(value) { data[2] = value }

    val vector: Vector3F get() = Vector3F(x, y, z)

    val lengthSquared: Float get() = (x * x) + (y * y) + (z * z)
    val length: Float get() = sqrt(lengthSquared)

    override operator fun get(index: Int): Float = data[index]
    operator fun set(index: Int, value: Float) { data[index] = value }

    companion object {
        operator fun invoke(x: Float, y: Float, z: Float): MVector3 = MVector3().setTo(x, y, z)
        operator fun invoke(x: Double, y: Double, z: Double): MVector3 = MVector3().setTo(x, y, z)
        operator fun invoke(x: Int, y: Int, z: Int): MVector3 = MVector3().setTo(x, y, z)

        fun length(x: Double, y: Double, z: Double): Double = sqrt(lengthSq(x, y, z))
        fun length(x: Float, y: Float, z: Float): Float = sqrt(lengthSq(x, y, z))

        fun lengthSq(x: Double, y: Double, z: Double): Double = x * x + y * y + z * z
        fun lengthSq(x: Float, y: Float, z: Float): Float = x * x + y * y + z * z
    }

    fun setTo(x: Float, y: Float, z: Float): MVector3 {
        this.x = x
        this.y = y
        this.z = z
        return this
    }
    fun setTo(x: Double, y: Double, z: Double): MVector3 = setTo(x.toFloat(), y.toFloat(), z.toFloat())
    fun setTo(x: Int, y: Int, z: Int): MVector3 = setTo(x.toFloat(), y.toFloat(), z.toFloat())

    inline fun setToFunc(func: (index: Int) -> Float): MVector3 = setTo(func(0), func(1), func(2))
    inline fun setToFunc(l: MVector4, r: MVector4, func: (l: Float, r: Float) -> Float) = setTo(
        func(l.x, r.x),
        func(l.y, r.y),
        func(l.z, r.z),
    )
    fun setToInterpolated(left: MVector4, right: MVector4, t: Double): MVector3 = setToFunc { t.toRatio().interpolate(left[it], right[it]) }

    fun copyFrom(other: MVector3) = setTo(other.x, other.y, other.z)

    fun scale(scale: Float) = this.setTo(this.x * scale, this.y * scale, this.z * scale)
    fun scale(scale: Int) = scale(scale.toFloat())
    fun scale(scale: Double) = scale(scale.toFloat())

    fun transform(mat: MMatrix3D) = mat.transform(this, this)
    fun transformed(mat: MMatrix3D, out: MVector3 = MVector3()) = mat.transform(this, out)

    fun normalize(vector: MVector3 = this): MVector3 {
        val norm = 1.0 / vector.length
        setTo(vector.x * norm, vector.y * norm, vector.z * norm)
        return this
    }

    fun normalized(out: MVector3 = MVector3()): MVector3 = out.copyFrom(this).normalize()

    fun dot(v2: MVector3): Float = (this.x * v2.x) + (this.y * v2.y) + (this.z * v2.y)

    operator fun plus(that: MVector3): MVector3 = MVector3(this.x + that.x, this.y + that.y, this.z + that.z)
    operator fun minus(that: MVector3): MVector3 = MVector3(this.x - that.x, this.y - that.y, this.z - that.z)
    operator fun times(scale: Float): MVector3 = MVector3(x * scale, y * scale, z * scale)

    fun sub(l: MVector3, r: MVector3): MVector3 = setTo(l.x - r.x, l.y - r.y, l.z - r.z)
    fun add(l: MVector3, r: MVector3): MVector3 = setTo(l.x + r.x, l.y + r.y, l.z + r.z)
    fun cross(a: MVector3, b: MVector3): MVector3 = setTo(
        (a.y * b.z - a.z * b.y),
        (a.z * b.x - a.x * b.z),
        (a.x * b.y - a.y * b.x),
    )

    fun clone() = MVector3(x, y, z)

    override fun equals(other: Any?): Boolean = (other is MVector3) && almostEquals(this.x, other.x) && almostEquals(
        this.y,
        other.y
    ) && almostEquals(this.z, other.z)
    override fun hashCode(): Int = data.contentHashCode()

    override fun toString(): String = "(${x.niceStr}, ${y.niceStr}, ${z.niceStr})"
}

@Deprecated("")
typealias MVector3D = MVector4

/*
sealed interface IVector3 {
    val x: Float
    val y: Float
    val z: Float
}

sealed interface Vector3 : IVector3 {
    override var x: Float
    override var y: Float
    override var z: Float
}

interface IVector4 : IVector3 {
    val w: Float
}

interface Vector4 : Vector3, IVector4 {
    override var w: Float
}
*/

// @TODO: To inline class wrapping FloatArray?
@KormaMutableApi
@Deprecated("")
class MVector4 {
    val data = floatArrayOf(0f, 0f, 0f, 1f)

    var x: Float get() = data[0]; set(value) { data[0] = value }
    var y: Float get() = data[1]; set(value) { data[1] = value }
    var z: Float get() = data[2]; set(value) { data[2] = value }
    var w: Float get() = data[3]; set(value) { data[3] = value }

    val lengthSquared: Float get() = (x * x) + (y * y) + (z * z) + (w * w)
    val length: Float get() = sqrt(lengthSquared)

    val length3Squared: Float get() = (x * x) + (y * y) + (z * z)
    val length3: Float get() = sqrt(length3Squared)

    operator fun get(index: Int): Float = data[index]
    operator fun set(index: Int, value: Float) { data[index] = value }

    companion object {
        operator fun invoke(x: Float, y: Float, z: Float, w: Float = 1f): MVector4 = MVector4().setTo(x, y, z, w)
        operator fun invoke(x: Double, y: Double, z: Double, w: Double = 1.0): MVector4 = MVector4().setTo(x, y, z, w)
        operator fun invoke(x: Int, y: Int, z: Int, w: Int = 1): MVector4 = MVector4().setTo(x, y, z, w)

        fun length(x: Double, y: Double, z: Double, w: Double): Double = sqrt(lengthSq(x, y, z, w))
        fun length(x: Double, y: Double, z: Double): Double = sqrt(lengthSq(x, y, z))
        fun length(x: Float, y: Float, z: Float, w: Float): Float = sqrt(lengthSq(x, y, z, w))
        fun length(x: Float, y: Float, z: Float): Float = sqrt(lengthSq(x, y, z))

        fun lengthSq(x: Double, y: Double, z: Double, w: Double): Double = x * x + y * y + z * z + w * w
        fun lengthSq(x: Double, y: Double, z: Double): Double = x * x + y * y + z * z
        fun lengthSq(x: Float, y: Float, z: Float, w: Float): Float = x * x + y * y + z * z + w * w
        fun lengthSq(x: Float, y: Float, z: Float): Float = x * x + y * y + z * z
    }

    fun copyFrom(other: MVector4) = setTo(other.x, other.y, other.z, other.w)

    fun setTo(x: Float, y: Float, z: Float, w: Float): MVector4 = this.apply { this.x = x; this.y = y; this.z = z; this.w = w }
    fun setTo(x: Double, y: Double, z: Double, w: Double): MVector4 = setTo(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
    fun setTo(x: Int, y: Int, z: Int, w: Int): MVector4 = setTo(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

    fun setTo(x: Float, y: Float, z: Float): MVector4 = setTo(x, y, z, 1f)
    fun setTo(x: Double, y: Double, z: Double): MVector4 = setTo(x, y, z, 1.0)
    fun setTo(x: Int, y: Int, z: Int): MVector4 = setTo(x, y, z, 1)

    inline fun setToFunc(func: (index: Int) -> Float): MVector4 = setTo(func(0), func(1), func(2), func(3))
    inline fun setToFunc(l: MVector4, r: MVector4, func: (l: Float, r: Float) -> Float) = setTo(
        func(l.x, r.x),
        func(l.y, r.y),
        func(l.z, r.z),
        func(l.w, r.w)
    )
    fun setToInterpolated(left: MVector4, right: MVector4, t: Double): MVector4 = setToFunc { t.toRatio().interpolate(left[it], right[it]) }

    fun scale(scale: Float) = this.setTo(this.x * scale, this.y * scale, this.z * scale, this.w * scale)
    fun scale(scale: Int) = scale(scale.toFloat())
    fun scale(scale: Double) = scale(scale.toFloat())

    fun transform(mat: MMatrix3D) = mat.transform(this, this)

    fun normalize(vector: MVector4 = this): MVector4 = this.apply {
        val norm = 1.0 / vector.length3
        setTo(vector.x * norm, vector.y * norm, vector.z * norm, 1.0)
    }

    fun normalized(out: MVector4 = MVector4()): MVector4 = out.copyFrom(this).normalize()

    fun dot(v2: MVector4): Float = this.x*v2.x + this.y*v2.y + this.z*v2.z

    operator fun plus(that: MVector4): MVector4 = MVector4(this.x + that.x, this.y + that.y, this.z + that.z, this.w + that.w)
    operator fun minus(that: MVector4): MVector4 = MVector4(this.x - that.x, this.y - that.y, this.z - that.z, this.w - that.w)
    operator fun times(scale: Float): MVector4 = MVector4(x * scale, y * scale, z * scale, w * scale)

    fun sub(l: MVector4, r: MVector4): MVector4 = setTo(l.x - r.x, l.y - r.y, l.z - r.z, l.w - r.w)
    fun add(l: MVector4, r: MVector4): MVector4 = setTo(l.x + r.x, l.y + r.y, l.z + r.z, l.w + r.w)
    fun cross(a: MVector4, b: MVector4): MVector4 = setTo(
        (a.y * b.z - a.z * b.y),
        (a.z * b.x - a.x * b.z),
        (a.x * b.y - a.y * b.x),
        1f
    )
    override fun equals(other: Any?): Boolean = (other is MVector4) && almostEquals(this.x, other.x) && almostEquals(this.y, other.y) && almostEquals(this.z, other.z) && almostEquals(this.w, other.w)
    override fun hashCode(): Int = data.contentHashCode()

    override fun toString(): String = if (w == 1f) "(${x.niceStr}, ${y.niceStr}, ${z.niceStr})" else "(${x.niceStr}, ${y.niceStr}, ${z.niceStr}, ${w.niceStr})"
}

fun MVector4.asIntVector3D() = MVector4Int(this)

typealias MPosition3D = MVector4
typealias MScale3D = MVector4

@KormaMutableApi
inline class MVector4Int(val v: MVector4) {
    var x: Int get() = v.x.toInt(); set(value) { v.x = value.toFloat() }
    var y: Int get() = v.y.toInt(); set(value) { v.y = value.toFloat() }
    var z: Int get() = v.z.toInt(); set(value) { v.z = value.toFloat() }
    var w: Int get() = v.w.toInt(); set(value) { v.w = value.toFloat() }
}

/////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////

@KormaMutableApi
sealed interface IScale {
    val scaleX: Double
    val scaleY: Double
}

@KormaMutableApi
data class MScale(
    override var scaleX: Double,
    override var scaleY: Double,
) : IScale {
    constructor() : this(1.0, 1.0)
}

fun Scale.toMutable(out: MScale = MScale()): MScale {
    out.scaleX = scaleX
    out.scaleY = scaleY
    return out
}
fun MScale.toImmutable(): Scale = Scale(scaleX, scaleY)

val Size.mutable: MSize get() = MSize(width, height)

val MSize.immutable: Size get() = Size(width, height)

fun MSize.asInt(): MSizeInt = MSizeInt(this)
fun MSizeInt.asDouble(): MSize = this.float

fun MPoint.asSize(): MSize = MSize(this)

@Deprecated("")
val Rectangle.mutable: MRectangle get() = MRectangle(x, y, width, height)
@Deprecated("")
fun Rectangle.mutable(out: MRectangle = MRectangle()): MRectangle = out.copyFrom(this)
