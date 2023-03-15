package com.soywiz.korma.geom.bezier

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*
import com.soywiz.korma.math.*
import com.soywiz.korma.math.isAlmostZero
import kotlin.contracts.*
import kotlin.math.*

sealed interface IBezier : Curve {
    val points: PointList
    val dims: Int
    val dpoints: List<PointList>
    val direction: Angle
    val clockwise: Boolean
    val extrema: Bezier.Extrema
    val boundingBox: MRectangle
    val lut: CurveLUT
    val isLinear: Boolean
    val isSimple: Boolean

    /**
     * Gets a list of [isSimple] bezier sub bezier curves.
     */
    fun toSimpleList(): List<SubBezier> = reduce()
    fun scaleSimple(d: Double): Bezier = scaleSimple { d }
    fun scaleSimple(d0: Double, d1: Double): Bezier = scaleSimple { if (it < 0.5) d0 else d1 }
    /** Computes the point of the curve at [t] */
    operator fun get(t: Double): Point = compute(t)

    fun getLUT(steps: Int = 100, out: CurveLUT = CurveLUT(this, steps + 1)): CurveLUT
    fun project(point: Point, out: Bezier.ProjectedPoint = Bezier.ProjectedPoint()): Bezier.ProjectedPoint
    fun inflections(): DoubleArray
    fun reduce(): List<SubBezier>
    fun overlaps(curve: Bezier): Boolean
    fun offset(d: Double): List<Bezier>
    fun offset(t: Double, d: Double): Point
    fun scaleSimple(d: (Double) -> Double): Bezier
    fun selfIntersections(threshold: Double = 0.5, out: DoubleArrayList = DoubleArrayList()): DoubleArrayList
    fun intersections(line: MLine): DoubleArray
    fun intersections(curve: Bezier, threshold: Double = 0.5): List<Pair<Double, Double>>
    fun compute(t: Double): Point
    fun derivative(t: Double, normalize: Boolean = false): Point
    fun normal(t: Double, normalize: Boolean = true): Point
    fun hull(t: Double, out: PointArrayList = PointArrayList()): PointList
    fun curvature(t: Double, kOnly: Boolean = false): Bezier.Curvature
    fun hullOrNull(t: Double, out: PointArrayList = PointArrayList()): PointList?
    fun split(t0: Double, t1: Double): SubBezier
    fun split(t: Double): CurveSplit
    fun splitLeft(t: Double): SubBezier
    fun splitRight(t: Double): SubBezier

    fun toLine(out: MLine = MLine()): MLine
    fun toLineBezier(out: Bezier = Bezier()): Bezier
    fun toCubic(out: Bezier = Bezier()): Bezier
    fun toQuad(out: Bezier = Bezier()): Bezier
    fun toQuadList(): List<Bezier>
    fun outline(d1: Double, d2: Double = d1, d3: Double = d1, d4: Double = d2): Curves
    fun translate(dx: Double, dy: Double, out: Bezier = Bezier()): Bezier
    fun transform(m: MMatrix, out: Bezier = Bezier()): Bezier
}

/**
 * Port of the operations of the library Bezier.JS with some adjustments,
 * Original library created by Pomax: https://github.com/Pomax/bezierjs
 * Based on algorithms described here: https://pomax.github.io/bezierinfo/
 */
class Bezier(
    points: PointList,
) : IBezier {
    private val _points = PointArrayList(points.size).copyFrom(points)
    override val points: PointList get() = _points

    init {
        if (points.size > 4) error("Only supports quad and cubic beziers")
    }

    constructor() : this(Point(0f, 0f), Point(0f, 0f))
    constructor(p0: Point, p1: Point) : this(pointArrayListOf(p0, p1))
    constructor(p0: Point, p1: Point, p2: Point) : this(pointArrayListOf(p0, p1, p2))
    constructor(p0: Point, p1: Point, p2: Point, p3: Point) : this(pointArrayListOf(p0, p1, p2, p3))

    fun copyFrom(bezier: IBezier): Bezier {
        this._points.copyFrom(bezier.points)
        return this
    }

    private inline fun _setPoints(block: PointArrayList.() -> Unit): Bezier {
        val p = this._points
        p.clear()
        block(p)
        invalidate()
        return this
    }

    fun setPoints(points: PointList): Bezier = _setPoints { copyFrom(points) }
    fun setPoints(p0: Point, p1: Point): Bezier = _setPoints { add(p0); add(p1) }
    fun setPoints(p0: Point, p1: Point, p2: Point): Bezier = _setPoints { add(p0); add(p1); add(p2) }
    fun setPoints(p0: Point, p1: Point, p2: Point, p3: Point): Bezier = _setPoints { add(p0); add(p1); add(p2); add(p3) }

    private var boundingBoxValid = false
    private var isSimpleValid = false
    private var lengthValid = false
    private var lutValid = false
    private var directionValid = false
    private var isLinearValid = false
    private var alignedValid = false
    private var dpointsValid = false
    private var extremaValid = false

    private fun invalidate() {
        lutValid = false
        boundingBoxValid = false
        isSimpleValid = false
        lengthValid = false
        directionValid = false
        isLinearValid = false
        alignedValid = false
        dpointsValid = false
        extremaValid = false
        _outerCircle = null
    }

    fun setToRoundDecimalPlaces(places: Int) {
        _points.setToRoundDecimalPlaces(places)
        invalidate()
    }
    fun roundDecimalPlaces(places: Int): Bezier = Bezier(points.roundDecimalPlaces(places))

    override fun getBounds(target: MRectangle): MRectangle = target.copyFrom(boundingBox)
    fun getBounds(target: MRectangle, m: Matrix): MRectangle = _getBoundingBox(target, m)

    override fun calc(t: Double): Point = this.compute(t)

    override fun equals(other: Any?): Boolean = other is Bezier && this.points == other.points
    override fun hashCode(): Int = points.hashCode()
    override fun toString(): String = "Bezier($points)"

    override val dims: Int get() = 2 // Always 2D for now
    override val order: Int get() = points.size - 1

    private val _aligned: PointArrayList = PointArrayList(points.size)
    private val aligned: PointList get() {
        if (alignedValid) return _aligned
        alignedValid = true
        _aligned.clear()
        align(points, MLine(points.first, points.last), _aligned)
        return _aligned
    }

    private var _dpoints: List<PointList> = emptyList()
    override val dpoints: List<PointList> get() {
        if (dpointsValid) return _dpoints
        dpointsValid = true
        _dpoints = derive(points)
        return _dpoints
    }

    private var _direction: Angle = Angle.ZERO
    override val direction: Angle get() {
        if (directionValid) return _direction
        directionValid = true
        _direction = Angle.between(points[0], points[order], points[1])
        return _direction
    }
    override val clockwise: Boolean get() = direction > Angle.ZERO

    private var _extrema: Extrema = Extrema(EMPTY_DOUBLE_ARRAY, EMPTY_DOUBLE_ARRAY)
    override val extrema: Extrema get() {
        if (extremaValid) return _extrema
        extremaValid = true
        val out = (0 until dims).map { dim ->
            //println("extrema dim=$dim, p=${p.toList()}, droots=${droots(p).toList()}")
            var out = droots(dpoints[0].getComponentList(dim).mapDouble { it.toDouble() })
            if (order == 3) out = combineSmallDistinctSorted(out, droots(dpoints[1].getComponentList(dim).mapDouble { it.toDouble() }))
            out
        }
        _extrema = Extrema(out[0], out[1])
        return _extrema
    }

    private var _outerCircle: MCircle? = null
    val outerCircle: ICircle get() {
        if (_outerCircle == null) {
            _outerCircle = boundingBox.outerCircle()
        }
        return _outerCircle!!
    }

    private val _boundingBox: MRectangle = MRectangle()
    override val boundingBox: MRectangle get() {
        if (boundingBoxValid) return _boundingBox
        boundingBoxValid = true
        _getBoundingBox(_boundingBox, Matrix.NIL)
        return _boundingBox
    }

    private fun _getBoundingBox(out: MRectangle, m: Matrix): MRectangle {
        var xmin = 0f
        var ymin = 0f
        var xmax = 0f
        var ymax = 0f
        for (n in 0..1) {
            //println("extrema=$extrema")
            val ext = extrema.dimt01(n)
            var min = Float.POSITIVE_INFINITY
            var max = Float.NEGATIVE_INFINITY
            ext.fastForEach { t ->
                val p = get(t)
                val value = when (n) {
                    0 -> p.transformX(m)
                    else -> p.transformY(m)
                }
                min = kotlin.math.min(min, value)
                max = kotlin.math.max(max, value)
                //println("t=$t, v=$value, c=${p.x}, ${p.y}, min=$min, max=$max")
            }
            //println("d=$n, min=$min, max=$max")
            //println("[$n]: min=$min, max=$max")
            if (n == 0) {
                xmin = min
                xmax = max
            } else {
                ymin = min
                ymax = max
            }
        }

        out.setBounds(xmin, ymin, xmax, ymax)
        return out
    }

    private var _length: Double = Double.NaN

    /** Calculates the length of this curve .*/
    override val length: Double get() {
        if (lengthValid) return _length
        lengthValid = true
        val z = 0.5
        var sum = 0.0

        for (i in T_VALUES.indices) {
            val t = z * T_VALUES[i] + z
            val temp = derivative(t)
            sum += C_VALUES[i] * temp.length
        }
        _length = z * sum
        return _length
    }

    private val _lut by lazy { CurveLUT(this, 101) }
    override val lut: CurveLUT get() {
        if (lutValid) return _lut
        lutValid = true
        getLUT(out = _lut)
        return _lut
    }

    //val equidistantLUT: CurveLUT = { lut.toEquidistantLUT() }

    private var _isLinear = false
    override val isLinear: Boolean get() {
        if (isLinearValid) return _isLinear
        isLinearValid = true
        val baseLength = (points[order] - points[0]).length
        _isLinear = (0 until aligned.size).sumOf { aligned.getY(it).absoluteValue.toDouble() } < baseLength / 50.0
        return _isLinear
    }

    /**
     * Determines if this [Bezier] is simple.
     *
     * Simpleness is defined as having all control
     * points on the same side of the baseline
     * (cubics having the additional constraint that the control-to-end-point lines may not cross),
     * and an angle between the end point normals no greater than 60 degrees.
     */
    private var _isSimple: Boolean = false
    override val isSimple: Boolean get() {
        if (isSimpleValid) return _isSimple
        isSimpleValid = true
        if (this.order == 3) {
            val a1 = angle(this.points[0], this.points[3], this.points[1])
            val a2 = angle(this.points[0], this.points[3], this.points[2])
            if ((a1 > 0f && a2 < 0f) || (a1 < 0f && a2 > 0f)) {
                _isSimple = false
                return _isSimple
            }
        }
        val n1 = this.normal(0.0)
        val n2 = this.normal(1.0)
        val s = n1.x * n2.x + n1.y * n2.y
        //if (this._3d) s += n1.z * n2.z
        _isSimple = abs(kotlin.math.acos(s)) < kotlin.math.PI / 3.0
        return _isSimple
    }

    override fun ratioFromLength(length: Double): Double {
        return lut.estimateAtLength(length).ratio
    }

    override fun getLUT(steps: Int, out: CurveLUT): CurveLUT {
        out.clear()
        for (n in 0..steps) {
            val t = n.toDouble() / steps.toDouble()
            out.add(t, compute(t))
        }
        return out
    }

    private fun findNearestLine(pX: Double, pY: Double, aX: Double, aY: Double, bX: Double, bY: Double, out: ProjectedPoint = ProjectedPoint()): ProjectedPoint {
        val atobX = bX - aX
        val atobY = bY - aY

        val atopX = pX - aX
        val atopY = pY - aY

        val len = atobX * atobX + atobY * atobY
        val dot = atopX * atobX + atopY * atobY

        val t = kotlin.math.min( 1.0, kotlin.math.max( 0.0, dot / len ) );
        //dot = ( bX - aX ) * ( pY - aY ) - ( bY - aY ) * ( pX - aX );
        out.p = Point(aX + atobX * t, aY + atobY * t)
        out.t = t
        out.dSq = Point.distanceSquared(out.p.xD, out.p.yD, pX, pY)
        return out
    }

    override fun project(point: Point, out: ProjectedPoint): ProjectedPoint {
        if (points.size == 2) {
            val p0 = points.get(0)
            val p1 = points.get(1)
            return findNearestLine(point.xD, point.yD, p0.xD, p0.yD, p1.xD, p1.yD, out)
        }

        val LUT = this.lut
        val l = LUT.steps.toDouble()
        val closest = LUT.closest(point)
        val mpos = closest.mpos
        val t1: Double = (mpos - 1) / l
        val t2: Double = (mpos + 1) / l
        val step: Double = 0.1 / l

        // step 2: fine check
        var mdistSq = closest.mdistSq
        var t = t1
        var ft = t
        mdistSq += 1
        while (t < t2 + step) {
            val p: Point = this.compute(t)
            val d = Point.distanceSquared(point, p).toDouble()
            if (d < mdistSq) {
                mdistSq = d
                ft = t
                out.p = p
            }
            t += step
        }
        out.p = this.compute(ft)
        out.t = when {
            ft < 0 -> 0.0
            ft > 1 -> 1.0
            else -> ft
        }
        out.dSq = mdistSq
        return out
    }

    data class ProjectedPoint(var p: Point = Point(), var t: Double = 0.0, var dSq: Double = 0.0) {
        val d: Double get() = sqrt(dSq)
        fun roundDecimalPlaces(places: Int): ProjectedPoint = ProjectedPoint(
            p.roundDecimalPlaces(places),
            t.roundDecimalPlaces(places),
            dSq.roundDecimalPlaces(places)
        )
    }

    //private var BezierCurve._t1: Double by Extra.Property { 0.0 }
    //private var BezierCurve._t2: Double by Extra.Property { 0.0 }

    /** Returns the [t] values where the curve changes its sign */
    override fun inflections(): DoubleArray {
        if (points.size < 4) return doubleArrayOf()

        // FIXME: TODO: add in inflection abstraction for quartic+ curves?
        //val p = align(points, Line(points.firstX, points.firstY, points.lastX, points.lastY))
        val p = aligned
        val p1 = p.get(index = 1)
        val p2 = p[2]
        val p3 = p[3]
        val a = p2.x * p1.y
        val b = p3.x * p1.y
        val c = p1.x * p2.y
        val d = p3.x * p2.y
        val v1 = 18.0 * (-3.0 * a + 2.0 * b + 3.0 * c - d)
        val v2 = 18.0 * (3.0 * a - b - 3.0 * c)
        val v3 = 18.0 * (c - a)

        if (v1.isAlmostEquals(0.0)) {
            if (!v2.isAlmostEquals(0.0)) {
                val t = -v3 / v2
                if (t in 0.0..1.0) return doubleArrayOf(t)
            }
            return doubleArrayOf()
        }
        val d2 = 2.0 * v1
        if (d2.isAlmostEquals(0.0)) return doubleArrayOf()
        val trm = v2 * v2 - 4.0 * v1 * v3
        if (trm < 0) return doubleArrayOf()
        val sq = kotlin.math.sqrt(trm)
        return listOf((sq - v2) / d2, -(v2 + sq) / d2).filter { it in 0.0..1.0 }.toDoubleArray()
    }

    override fun reduce(): List<SubBezier> {
        if (isLinear) return listOf(SubBezier(this))

        val step = 0.01
        val pass1 = arrayListOf<SubBezier>()
        val pass2 = arrayListOf<SubBezier>()
        // first pass: split on extrema
        var extrema = this.extrema.allt

        if (extrema.indexOfFirst { it == 0.0 } < 0) extrema = doubleArrayOf(0.0) + extrema
        if (extrema.indexOfFirst { it == 1.0 } < 0) extrema += doubleArrayOf(1.0)

        run {
            var t1 = extrema[0]
            for (i in 1 until extrema.size) {
                val t2 = extrema[i]
                val segment = this.split(t1, t2)
                pass1.add(segment)
                t1 = t2
            }
        }

        // second pass: further reduce these segments to simple segments
        for (p1Curve in pass1) {
            val p1 = p1Curve.curve
            var t1 = 0.0
            var t2 = 0.0
            while (t2 <= 1) {
                t2 = t1 + step
                while (t2 <= 1 + step) {
                    var segment = p1.split(t1, t2)
                    if (!segment.curve.isSimple) {
                        t2 -= step
                        if (abs(t1 - t2) < step) {
                            // we can never form a reduction
                            return listOf()
                        }
                        segment = p1.split(t1, t2)
                        pass2.add(
                            SubBezier(
                                segment.curve,
                                map(t1, 0.0, 1.0, p1Curve.t1, p1Curve.t2),
                                map(t2, 0.0, 1.0, p1Curve.t1, p1Curve.t2),
                                this
                            )
                        )
                        t1 = t2
                        break
                    }
                    t2 += step
                }
            }
            if (t1 < 1.0) {
                val segment = p1.split(t1, 1.0)
                pass2.add(
                    SubBezier(
                        segment.curve,
                        map(t1, 0.0, 1.0, p1Curve.t1, p1Curve.t2),
                        p1Curve.t2,
                        this
                    )
                )
            }
        }
        return pass2
    }

    override fun overlaps(curve: Bezier): Boolean {
        val lbbox = this.boundingBox
        val tbbox = curve.boundingBox
        return bboxoverlap(lbbox, tbbox)
    }

    /**
     * This function creates a new curve, offset along the curve normals,
     * at distance [d]. Note that deep magic lies here and the offset curve
     * of a Bezier curve cannot ever be another Bezier curve.
     *
     * As such, this function "cheats" and yields an array of curves which,
     * taken together, form a single continuous curve equivalent
     * to what a theoretical offset curve would be.
     */
    override fun offset(d: Double): List<Bezier> =
        this.toSimpleList().map { it.curve.scaleSimple { d } }

    /**
     * A coordinate is returned, representing the point on the curve at
     * [t]=..., offset along its normal by a distance [d]
     */
    override fun offset(t: Double, d: Double): Point {
        val pos = calc(t)
        val normal = normal(t)
        return pos + normal * d
    }

    /**
     * Scales a curve with respect to the intersection
     * between the end point normals at a [d] distance.
     *
     * Note that this will only work if that point exists,
     * which is only guaranteed for simple segments.
     *
     * You can call [isSimple] to check if this is suitable,
     * and [toSimpleList] to get a list of simple curves.
     */
    override fun scaleSimple(d: (Double) -> Double): Bezier {
        val r0 = d(0.0)
        val r1 = d(1.0)

        if (isLinear) {
            val nv = this.normal(0.0)
            return Bezier(
                this.points[0] + (nv * r0),
                this.points[1] + (nv * r1),
            )
        }

        val c0 = calc(0.0)
        val c1 = calc(1.0)
        val n0 = normal(0.0)
        val n1 = normal(1.0)
        val v = listOf(this.offset(0.0, 10.0), this.offset(1.0, 10.0))
        val o = lli4(v[0], c0, v[1], c1)
            ?: error("cannot scale this curve. Try reducing it first.")
        val np = PointArrayList(this.points.size)

        val singleDist = r0 == r1 && r0 == d(0.5)

        // move all points by distance 'd' wrt the origin 'o',
        // and move end points by fixed distance along normal.
        for (n in 0..order) {
            when (n) {
                0 -> np.add(r0 * n0.x, r0 * n0.y)
                order -> np.add(r1 * n1.x, r1 * n1.y)
                else -> {
                    when {
                        singleDist -> {
                            val t = n - 1
                            val p = np[t * order]
                            val d = this.derivative(t.toDouble())
                            val p2 = Point(p.x + d.x, p.y + d.y)
                            np.add(lli4(p, p2, o, points[t + 1]) ?: error("Invalid curve"))
                        }
                        else -> {
                            val t = n - 1
                            val p = points[t + 1]
                            val ov = p - o
                            var rc = d((t + 1) / order.toDouble())
                            if (!clockwise) rc = -rc
                            np.add(p + (ov.normalized * rc))
                        }
                    }
                }
            }
        }

        return Bezier(np)
    }

    private fun raise(): Bezier {
        val p = this.points
        val np = PointArrayList()
        np.add(p[0])
        val k = p.size
        for (i in 1 until k) {
            val pi = p[i]
            val pim = p[i - 1]
            np.add(((pi * (k - i) / k)) + (pim * (i.toDouble() / k.toDouble())))
        }
        np.add(p, k - 1)
        return Bezier(np)
    }

    override fun selfIntersections(threshold: Double, out: DoubleArrayList): DoubleArrayList {
        val reduced = this.reduce()
        val len = reduced.size - 2
        val results = out

        for (i in 0 until len) {
            val left = reduced.slice(i until i + 1)
            val right = reduced.slice(i + 2 until reduced.size)
            val result = curveintersects(left, right, threshold)
            results.add(result.mapDouble { it.first })
        }
        return results
    }

    override fun intersections(line: MLine): DoubleArray {
        val minX = line.minX
        val minY = line.minY
        val maxX = line.maxX
        val maxY = line.maxY
        return roots(this.points, line).filter { t ->
            val p = this.get(t)
            between(p.xD, minX, maxX) && between(p.yD, minY, maxY)
        }.toDoubleArray()
    }

    override fun intersections(curve: Bezier, threshold: Double): List<Pair<Double, Double>> =
        curveintersects(this.reduce(), curve.reduce(), threshold)

    /** Computes the point of the curve at [t] */
    override fun compute(t: Double): Point = compute(t, this.points)

    /** The derivate vector of the curve at [t] (normalized when [normalize] is set to true) */
    override fun derivative(t: Double, normalize: Boolean): Point {
        var out = compute(t, dpoints[0])
        if ((t == 0.0 || t == 1.0) && out.squaredLength.isAlmostZero()) {
            for (n in 0 until 10) {
                val newT = 10.0.pow(-(10 - n))
                val nt = if (t == 1.0) 1.0 - newT else newT
                //println("newT=$newT, nt=$nt")
                out = compute(nt, dpoints[0])
                if (!out.squaredLength.isAlmostZero()) break
            }
        }
        if (normalize) out = out.normalized
        return out
    }

    /** The normal vector of the curve at [t] (normalized when [normalize] is set to true) */
    override fun normal(t: Double, normalize: Boolean): Point = derivative(t, normalize).toNormal()
    override fun normal(t: Double): Point = normal(t, normalize = true)
    override fun tangent(t: Double): Point = derivative(t, normalize = true)

    override fun hull(t: Double, out: PointArrayList): PointList {
        if (order < 2) error("Can't compute hull of order=$order < 2")
        return hullOrNull(t, out)!!
    }

    override fun curvature(t: Double, kOnly: Boolean): Curvature {
        return curvature(t, dpoints[0], dpoints[1], dims, kOnly)
    }

    override fun hullOrNull(t: Double, out: PointArrayList): PointList? {
        if (order < 2) return null
        var p = this.points
        out.add(p, 0)
        out.add(p, 1)
        out.add(p, 2)
        if (this.order == 3) {
            out.add(p, 3)
        }
        // we lerp between all points at each iteration, until we have 1 point left.
        while (p.size > 1) {
            val next = PointArrayList()
            for (i in 0 until p.size - 1) {
                val p = t.toRatio().interpolate(p[i], p[i + 1])
                out.add(p)
                next.add(p)
            }
            p = next
        }
        return out
    }

    override fun split(t0: Double, t1: Double): SubBezier =
        SubBezier(splitRight(t0).splitLeft(map(t1, t0, 1.0, 0.0, 1.0)).curve, t0, t1, this)

    override fun split(t: Double): CurveSplit = SubBezier(this).split(t)
    override fun splitLeft(t: Double): SubBezier = SubBezier(this).splitLeft(t)
    override fun splitRight(t: Double): SubBezier = SubBezier(this).splitRight(t)

    class Extrema(
        val xt: DoubleArray, val yt: DoubleArray
    ) {
        val allt: DoubleArray by lazy { combineSmallDistinctSorted(xt, yt) }

        val xt01 by lazy { doubleArrayOf(0.0, *xt, 1.0) }
        val yt01 by lazy { doubleArrayOf(0.0, *yt, 1.0) }

        fun dimt(index: Int): DoubleArray = if (index == 0) xt else yt
        fun dimt01(index: Int): DoubleArray = if (index == 0) xt01 else yt01

        override fun equals(other: Any?): Boolean =
            other is Extrema && this.xt.contentEquals(other.xt) && this.yt.contentEquals(other.yt)

        override fun hashCode(): Int = xt.contentHashCode() + yt.contentHashCode() * 7
        override fun toString(): String = "Extrema(x=${xt.contentToString()}, y=${yt.contentToString()})"
    }

    data class Curvature(
        val k: Double = 0.0,
        val r: Double = 0.0,
        val dk: Double = 0.0,
        val adk: Double = 0.0,
    )

    override fun toLine(out: MLine): MLine = out.setTo(points[0], points[order])

    override fun toLineBezier(out: Bezier): Bezier = out.setPoints(points[0], points[order])

    override fun toCubic(out: Bezier): Bezier {
        return when (order) {
            1 -> {
                val p0 = points[0]
                val p1 = points[1]
                val pd = p1 - p0
                val r1 = 1.0 / 3.0
                val r2 = 2.0 / 3.0
                out.setPoints(p0, p0 + (pd * r1), p0 + (pd * r2), p1)
            }
            2 -> {
                val p0 = points[0]
                val pc = points[1]
                val p1 = points[2]
                out.setPoints(p0, quadToCubic1(p0, pc), quadToCubic2(pc, p1), p1)
            }
            3 -> out.copyFrom(this) // No conversion
            else -> TODO("Unsupported higher order curves")
        }
    }

    override fun toQuad(out: Bezier): Bezier {
        return when (order) {
            1 -> {
                val p0 = points[0]
                val p1 = points[1]
                out.setPoints(p0, (p0 + p1) * 0.5, p1)
            }
            2 -> out.copyFrom(this) // No conversion
            3 -> {
                val p0 = points[0]
                val pc1 = points[1]
                val pc2 = points[2]
                val p1 = points[3]
                val pc = -(p0 * .25f) + (pc1 * .75f) + (pc2 * .75f) - (p1 * .25f)
                return out.setPoints(p0, pc, p1)
            }
            else -> TODO("Unsupported higher order curves")
        }
    }

    override fun toQuadList(): List<Bezier> {
        if (this.order == 2) return listOf(this)
        return toSimpleList().map { it.curve.toQuad() }
    }

    override fun outline(d1: Double, d2: Double, d3: Double, d4: Double): Curves {
        if (this.isLinear) {
            // TODO: find the actual extrema, because they might
            //       be before the start, or past the end.

            val n = this.normal(0.0)
            val start = this.points[0]
            val end = this.points[this.points.size - 1]

            val fline = run {
                val s = Point(start.x + n.x * d1, start.y + n.y * d1)
                val e = Point(end.x + n.x * d3, end.y + n.y * d3)
                val mid = Point((s.x + e.x) / 2.0, (s.y + e.y) / 2.0)
                pointArrayListOf(s, mid, e)
            }

            val bline = run {
                val s = Point(start.x - n.x * d2, start.y - n.y * d2)
                val e = Point(end.x - n.x * d4, end.y - n.y * d4)
                val mid = Point((s.x + e.x) / 2.0, (s.y + e.y) / 2.0)
                pointArrayListOf(e, mid, s)
            };

            val ls = makeline(bline[2], fline[0])
            val le = makeline(fline[2], bline[0])
            val segments = listOf(ls, Bezier(fline), le, Bezier(bline))
            return Curves(segments, closed = true)
        }

        val reduced = this.reduce()
        val len = reduced.size
        val fcurves = arrayListOf<Bezier>()
        val bcurves0 = arrayListOf<Bezier>()
        val tlen = this.length
        val graduated = d3 != d1 && d4 != d2

        var alen = 0.0

        fun linearDistanceFunction(s: Double, e: Double, tlen: Double, alen: Double, slen: Double): (Double) -> Double =
            fun (v: Double): Double {
                val f1 = alen / tlen
                val f2 = (alen + slen) / tlen
                val d = e - s
                return map(v, 0.0, 1.0, s + f1 * d, s + f2 * d);
            }

        // form curve oulines
        for (segment in reduced.map { it.curve }) {
            val slen = segment.length
            if (graduated) {
                fcurves.add(
                    segment.scaleSimple(linearDistanceFunction(d1, d3, tlen, alen, slen))
                )
                bcurves0.add(
                    segment.scaleSimple(linearDistanceFunction(-d2, -d4, tlen, alen, slen))
                )
            } else {
                fcurves.add(segment.scaleSimple(d1));
                bcurves0.add(segment.scaleSimple(-d2));
            }
            alen += slen;
        }

        // reverse the "return" outline
        val bcurves = bcurves0
            .map { Bezier(it.points.clone().also { it.reverse() }) }
            .reversed()

        // form the endcaps as lines
        val fs = fcurves[0].points[0]
        val fe = fcurves[len - 1].points[fcurves[len - 1].points.size - 1]
        val bs = bcurves[len - 1].points[bcurves[len - 1].points.size - 1]
        val be = bcurves[0].points[0]
        val ls = makeline(bs, fs)
        val le = makeline(fe, be)

        return (listOf(ls) + fcurves + le + bcurves).toCurves(closed = true)
    }

    override fun translate(dx: Double, dy: Double, out: Bezier): Bezier =
        out.setPoints(points.mapPoints { x, y, o -> o.setTo(x + dx, y + dy) })

    override fun transform(m: MMatrix, out: Bezier): Bezier =
        out.setPoints(points.mapPoints { x, y, o -> m.transform(x, y, o) })

    companion object {
        // Legendre-Gauss abscissae with n=24 (x_i values, defined at i=n as the roots of the nth order Legendre polynomial Pn(x))
        @PublishedApi internal val T_VALUES = doubleArrayOf(
            -0.06405689286260563, 0.06405689286260563, -0.1911188674736163, 0.1911188674736163,
            -0.3150426796961634, 0.3150426796961634, -0.4337935076260451, 0.4337935076260451,
            -0.5454214713888396, 0.5454214713888396, -0.6480936519369755, 0.6480936519369755,
            -0.7401241915785544, 0.7401241915785544, -0.820001985973903, 0.820001985973903,
            -0.8864155270044011, 0.8864155270044011, -0.9382745520027328, 0.9382745520027328,
            -0.9747285559713095, 0.9747285559713095, -0.9951872199970213, 0.9951872199970213,
        )

        // Legendre-Gauss weights with n=24 (w_i values, defined by a function linked to in the Bezier primer article)
        @PublishedApi internal val C_VALUES = doubleArrayOf(
            0.12793819534675216, 0.12793819534675216, 0.1258374563468283, 0.1258374563468283,
            0.12167047292780339, 0.12167047292780339, 0.1155056680537256, 0.1155056680537256,
            0.10744427011596563, 0.10744427011596563, 0.09761865210411388, 0.09761865210411388,
            0.08619016153195327, 0.08619016153195327, 0.0733464814110803, 0.0733464814110803,
            0.05929858491543678, 0.05929858491543678, 0.04427743881741981, 0.04427743881741981,
            0.028531388628933663, 0.028531388628933663, 0.0123412297999872, 0.0123412297999872
        )

        private fun curvature(t: Double, d1: PointList, d2: PointList, dims: Int, kOnly: Boolean = false): Curvature {
            val d = compute(t, d1)
            val dd = compute(t, d2)
            val qdsum = d.x * d.x + d.y * d.y

            val num = if (dims >= 3) {
                TODO()
                //sqrt(
                //    pow(d.y * dd.z - dd.y * d.z, 2) +
                //        pow(d.z * dd.x - dd.z * d.x, 2) +
                //        pow(d.x * dd.y - dd.x * d.y, 2)
                //);
            } else {
                d.x * dd.y - d.y * dd.x
            }

            val dnm = if (dims >= 3) {
                //pow(qdsum + d.z * d.z, 3.0 / 2.0);
                TODO()
            } else {
                qdsum.pow(3f / 2f)
            }

            if (num == 0.0f || dnm == 0.0f) {
                return Curvature(k = 0.0, r = 0.0)
            }

            val k = (num / dnm).toDouble()
            val r = (dnm / num).toDouble()

            // We're also computing the derivative of kappa, because
            // there is value in knowing the rate of change for the
            // curvature along the curve. And we're just going to
            // ballpark it based on an epsilon.
            return when {
                !kOnly -> {
                    // compute k'(t) based on the interval before, and after it,
                    // to at least try to not introduce forward/backward pass bias.
                    val pk = curvature(t - 0.001, d1, d2, dims, true).k
                    val nk = curvature(t + 0.001, d1, d2, dims, true).k
                    val dk = (nk - k + (k - pk)) / 2
                    val adk = (abs(nk - k) + abs(k - pk)) / 2
                    Curvature(k, r, dk, adk)
                }
                else -> {
                    Curvature(k, r)
                }
            }
        }

        val MRectangle.midX: Double get() = (left + right) * 0.5
        val MRectangle.midY: Double get() = (top + bottom) * 0.5

        private fun bboxoverlap(a: MRectangle, b: MRectangle): Boolean {
            if (abs(a.midX - b.midX) >= ((a.width + b.width) / 2.0)) return false
            if (abs(a.midY - b.midY) >= ((a.height + b.height) / 2.0)) return false
            return true

            //return a.intersects(b)
            //val dims = ["x", "y"],
            //val len = dims.length;
            //for (let i = 0, dim, l, t, d; i < len; i++) {
            //    dim = dims[i];
            //    l = b1[dim].mid;
            //    t = b2[dim].mid;
            //    d = (b1[dim].size + b2[dim].size) / 2;
            //    if (abs(l - t) >= d) return false;
            //}
            //return true;
        }

        private fun map(v: Double, ds: Double, de: Double, ts: Double, te: Double): Double {
            return v.convertRange(ds, de, ts, te)
            //val d1 = de - ds
            //val d2 = te - ts
            //val v2 = v - ds
            //val r = v2 / d1
            //return ts + d2 * r
        }

        private fun angle(o: Point, v1: Point, v2: Point): Float {
            val dx1 = v1.x - o.x
            val dy1 = v1.y - o.y
            val dx2 = v2.x - o.x
            val dy2 = v2.y - o.y
            val cross = dx1 * dy2 - dy1 * dx2
            val dot = dx1 * dx2 + dy1 * dy2
            return atan2(cross, dot)
        }

        private fun compute(t: Double, points: PointList): Point {
            val p = points
            val order = p.size - 1
            if (t == 0.0) return p[0]
            if (t == 1.0) return p[order]
            if (order == 0) return p[0]
            val mt = 1 - t
            val mt2 = mt * mt
            val t2 = t * t
            return when (order) {
                1 -> {
                    //println("compute: t=$t, mt=$mt")
                    (p[0] * mt) + (p[1] * t)
                }
                2 -> {
                    val a = mt2
                    val b = mt * t * 2
                    val c = t2

                    (p[0] * a) + (p[1] * b) + (p[2] * c)
                }
                3 -> {
                    val a = mt2 * mt
                    val b = mt2 * t * 3
                    val c = mt * t2 * 3
                    val d = t * t2
                    (p[0] * a) + (p[1] * b) + (p[2] * c) + (p[3] * d)
                }
                else -> TODO("higher order curves")
            }
        }

        private fun derive(points: PointList): List<PointList> {
            val out = arrayListOf<PointList>()

            var current = points
            while (current.size >= 2) {
                val new = PointArrayList(current.size - 1)
                val c = (current.size - 1).toDouble()
                for (n in 0 until current.size - 1) {
                    new.add((current[n + 1] - current[n]) * c)
                }
                out.add(new)
                current = new
            }

            return out
        }

        private fun crt(v: Double): Double = if (v < 0.0) -(-v).pow(1.0 / 3.0) else v.pow(1.0 / 3.0)

        private fun align(
            points: PointList,
            line: MLine,
            out: PointArrayList = PointArrayList()
        ): PointList {
            val p1 = line.a
            val p2 = line.b
            val tx = p1.x
            val ty = p1.y
            val a = -atan2(p2.y - ty, p2.x - tx)
            points.fastForEach { (x, y) ->
                out.add(
                    (x - tx) * cos(a) - (y - ty) * sin(a),
                    (x - tx) * sin(a) + (y - ty) * cos(a),
                )
            }
            return out
        }

        private const val tau = kotlin.math.PI * 2.0

        private fun between(v: Double, min: Double, max: Double): Boolean =
            ((min <= v) && (v <= max)) || v.isAlmostEquals(min) || v.isAlmostEquals(max)

        private val X_AXIS = MLine(0, 0, 1, 0)

        private fun roots(points: PointList, line: MLine = X_AXIS): DoubleArray {
            val order = points.size - 1
            val aligned = align(points, line)

            if (order == 2) {
                val a: Double = aligned.getY(0).toDouble()
                val b: Double = aligned.getY(1).toDouble()
                val c: Double = aligned.getY(2).toDouble()
                val d: Double = a - 2.0 * b + c
                if (d != 0.0) {
                    val m1 = -kotlin.math.sqrt(b * b - a * c)
                    val m2 = -a + b
                    val v1 = -(m1 + m2) / d
                    val v2 = -(-m1 + m2) / d
                    return doubleArrayOfValid01(v1, v2)
                } else if (b != c && d == 0.0) {
                    return doubleArrayOfValid01((2 * b - c) / (2 * b - 2 * c))
                }
                return doubleArrayOfValid01()
            }

            // see http://www.trans4mind.com/personal_development/mathematics/polynomials/cubicAlgebra.htm
            val pa: Double = aligned.getY(0).toDouble()
            val pb: Double = aligned.getY(1).toDouble()
            val pc: Double = aligned.getY(2).toDouble()
            val pd: Double = aligned.getY(3).toDouble()

            val d: Double = -pa + 3 * pb - 3 * pc + pd
            var a: Double = 3 * pa - 6 * pb + 3 * pc
            var b: Double = -3 * pa + 3 * pb
            var c: Double = pa

            if (d.isAlmostEquals(0.0)) {
                // this is not a cubic curve.
                if (a.isAlmostEquals(0.0)) {
                    // in fact, this is not a quadratic curve either.
                    if (b.isAlmostEquals(0.0)) {
                        // in fact in fact, there are no solutions.
                        return doubleArrayOfValid01()
                    }
                    // linear solution:
                    return doubleArrayOfValid01(-c / b)
                }
                // quadratic solution:
                val q = kotlin.math.sqrt(b * b - 4 * a * c)
                val a2 = 2 * a
                return doubleArrayOfValid01((q - b) / a2, (-b - q) / a2)
            }

            // at this point, we know we need a cubic solution:

            a /= d
            b /= d
            c /= d

            val p: Double = (3 * b - a * a) / 3.0
            val p3: Double = p / 3.0
            val q: Double = (2 * a * a * a - 9 * a * b + 27 * c) / 27.0
            val q2: Double = q / 2.0
            val discriminant: Double = q2 * q2 + p3 * p3 * p3

            if (discriminant < 0) {
                val mp3 = -p / 3.0
                val mp33 = mp3 * mp3 * mp3
                val r = kotlin.math.sqrt(mp33)
                val t = -q / (2.0 * r)
                val cosphi = if (t < -1.0) -1.0 else if (t > 1.0) 1.0 else t
                val phi = kotlin.math.acos(cosphi)
                val crtr = crt(r)
                val t1 = 2 * crtr
                val x1 = t1 * cos(phi / 3.0) - a / 3.0
                val x2 = t1 * cos((phi + tau) / 3.0) - a / 3.0
                val x3 = t1 * cos((phi + 2 * tau) / 3.0) - a / 3.0
                return doubleArrayOfValid01(x1, x2, x3)
            } else if (discriminant == 0.0) {
                val u1 = if (q2 < 0) crt(-q2) else -crt(q2)
                val x1 = 2.0 * u1 - a / 3.0
                val x2 = -u1 - a / 3.0
                return doubleArrayOfValid01(x1, x2)
            } else {
                val sd = kotlin.math.sqrt(discriminant)
                val u1 = crt(-q2 + sd)
                val v1 = crt(q2 + sd)
                return doubleArrayOfValid01(u1 - v1 - a / 3.0)
            }
        }

        private fun droots(p: DoubleArray): DoubleArray {
            when (p.size) {
                3 -> {
                    val a = p[0]
                    val b = p[1]
                    val c = p[2]
                    val d = a - 2 * b + c
                    if (d != 0.0) {
                        val m1 = -kotlin.math.sqrt(b * b - a * c)
                        val m2 = -a + b
                        val v1 = -(m1 + m2) / d
                        val v2 = -(-m1 + m2) / d
                        return doubleArrayOfValid01(v1, v2)
                    } else if (b != c && d == 0.0) {
                        return doubleArrayOfValid01((2.0 * b - c) / (2.0 * (b - c)))
                    }
                }
                2 -> {
                    val a = p[0]
                    val b = p[1]
                    if (a != b) return doubleArrayOfValid01(a / (a - b))
                }
                else -> {
                }
            }
            return doubleArrayOfValid01()
        }

        private val EMPTY_DOUBLE_ARRAY = DoubleArray(0)

        private fun doubleArrayOfValid01(v1: Double = Double.NaN, v2: Double = Double.NaN, v3: Double = Double.NaN): DoubleArray {
            val v1Valid = v1 in 0.0..1.0
            val v2Valid = v2 in 0.0..1.0
            val v3Valid = v3 in 0.0..1.0
            var validCount = 0
            if (v1Valid) validCount++
            if (v2Valid) validCount++
            if (v3Valid) validCount++
            if (validCount == 0) return EMPTY_DOUBLE_ARRAY
            var index = 0
            val out = DoubleArray(validCount)
            if (v1Valid) out[index++] = v1.normalizeZero()
            if (v2Valid) out[index++] = v2.normalizeZero()
            if (v3Valid) out[index++] = v3.normalizeZero()
            return out
        }

        private fun curveintersects(
            c1: List<SubBezier>,
            c2: List<SubBezier>,
            threshold: Double = 0.5
        ): List<Pair<Double, Double>> {
            val pairs = arrayListOf<Pair<SubBezier, SubBezier>>()
            // step 1: pair off any overlapping segments
            c1.fastForEach { l ->
                c2.fastForEach { r ->
                    if (l.curve.overlaps(r.curve)) {
                        pairs.add(l to r)
                    }
                }
            }
            // step 2: for each pairing, run through the convergence algorithm.
            return pairs.flatMap { pairiteration(it.first, it.second, threshold) }
        }

        private fun pairiteration(
            c1: SubBezier,
            c2: SubBezier,
            threshold: Double = 0.5
        ): List<Pair<Double, Double>> {
            val c1b = c1.boundingBox
            val c2b = c2.boundingBox
            val r = 100000.0

            if (
                c1b.width.absoluteValue + c1b.height.absoluteValue < threshold &&
                c2b.width.absoluteValue + c2b.height.absoluteValue < threshold
            ) {
                return listOf(
                    Pair(
                        (((r * (c1.t1 + c1.t2)) / 2.0).toInt()).toDouble() / r,
                        (((r * (c2.t1 + c2.t2)) / 2.0).toInt()).toDouble() / r
                    )
                )
            }

            val cc1 = c1.split(0.5)
            val cc2 = c2.split(0.5)
            val pairs2 = listOf(
                Pair(cc1.left, cc2.left),
                Pair(cc1.left, cc2.right),
                Pair(cc1.right, cc2.right),
                Pair(cc1.right, cc2.left),
            )

            val pairs = pairs2.filter { pair ->
                bboxoverlap(pair.first.curve.boundingBox, pair.second.curve.boundingBox)
            }

            val results = arrayListOf<Pair<Double, Double>>()

            if (pairs.isEmpty()) return results

            //println("pairs[${pairs.size}]=$pairs")
            pairs.forEach { pair ->
                results.addAll(pairiteration(pair.first, pair.second, threshold))
            }

            //return results.filterIndexed { index, pair -> results.indexOf(pair) == index }
            return results.distinct()
        }

        private fun lli8(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double, x4: Double, y4: Double): Point? {
            val d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)
            if (d == 0.0) return null
            val nx = (x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)
            val ny = (x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)
            return Point(nx / d, ny / d)
        }

        private fun lli4(p1: Point, p2: Point, p3: Point, p4: Point): Point? =
            lli8(p1.xD, p1.yD, p2.xD, p2.yD, p3.xD, p3.yD, p4.xD, p4.yD)

        fun cubicFromPoints(S: Point, B: Point, E: Point, t: Double = 0.5, d1: Double? = null): Bezier {
            val abc = getABC(3, S, B, E, t);
            val d1 = d1 ?: dist(B, abc.C)
            val d2 = (d1 * (1 - t)) / t

            val selen = dist(S, E)
            val lx = (E.x - S.x) / selen
            val ly = (E.y - S.y) / selen
            val bx1 = d1 * lx
            val by1 = d1 * ly
            val bx2 = d2 * lx
            val by2 = d2 * ly
            // derivation of new hull coordinates
            val e1 = Point(B.x - bx1, B.y - by1)
            val e2 = Point(B.x + bx2, B.y + by2)
            val A = abc.A
            val v1 = Point(A.x + (e1.x - A.x) / (1 - t), A.y + (e1.y - A.y) / (1 - t))
            val v2 = Point(A.x + (e2.x - A.x) / t, A.y + (e2.y - A.y) / t)
            val nc1 = Point(S.x + (v1.x - S.x) / t, S.y + (v1.y - S.y) / t)
            val nc2 = Point(
                E.x + (v2.x - E.x) / (1 - t),
                E.y + (v2.y - E.y) / (1 - t),
            )
            // ...done
            return Bezier(S, nc1, nc2, E)
        }

        fun quadraticFromPoints(p1: Point, p2: Point, p3: Point, t: Double = 0.5): Bezier {
            // shortcuts, although they're really dumb
            if (t == 0.0) return Bezier(p2, p2, p3)
            if (t == 1.0) return Bezier(p1, p2, p2)
            // real fitting.
            val abc = Bezier.getABC(2, p1, p2, p3, t);
            return Bezier(p1, abc.A, p3);
        }

        private fun getABC(order: Int, S: Point, B: Point, E: Point, t: Double = 0.5): ABCResult {
            val u = projectionratio(t, order)
            val um = 1.0 - u
            val C = Point(
                u * S.x + um * E.x,
                u * S.y + um * E.y,
            )
            val s = abcratio(t, order)
            val A = Point(
                B.x + (B.x - C.x) / s,
                B.y + (B.y - C.y) / s,
            )
            return ABCResult(A = A, B = B, C = C, S = S, E = E)
        }

        data class ABCResult(
            val A: Point,
            val B: Point,
            val C: Point,
            val S: Point,
            val E: Point,
        )

        private fun projectionratio(t: Double = 0.5, n: Int): Double {
            // see u(t) note on http://pomax.github.io/bezierinfo/#abc
            if (n != 2 && n != 3) return Double.NaN
            if (t == 0.0 || t == 1.0) return t
            val top = (1 - t).pow(n)
            val bottom = t.pow(n) + top
            return top / bottom
        }

        private fun abcratio(t: Double, n: Int): Double {
            // see ratio(t) note on http://pomax.github.io/bezierinfo/#abc
            if (n != 2 && n != 3) return Double.NaN
            if (t == 0.0 || t == 1.0) return t
            val bottom = (t).pow(n) + (1 - t).pow(n)
            val top = bottom - 1
            return abs(top / bottom)
        }

        private fun dist(p1: Point, p2: Point): Double {
            val dx = p1.x - p2.x
            val dy = p1.y - p2.y
            return kotlin.math.sqrt(dx * dx + dy * dy).toDouble()
        }

        private fun combineSmallDistinctSorted(a: DoubleArray, b: DoubleArray): DoubleArray {
            val out = DoubleArrayList(a.size + b.size)
            out.add(a)
            b.fastForEach { if (!out.contains(it)) out.add(it) }
            out.sort()
            return out.toDoubleArray()
        }

        private fun makeline(p1: Point, p2: Point): Bezier = Bezier(p1, (p1 + p2) / 2, p2)

        @OptIn(ExperimentalContracts::class)
        inline fun <T> quadCalc(
            x0: Double, y0: Double,
            xc: Double, yc: Double,
            x1: Double, y1: Double,
            t: Double,
            emit: (x: Double, y: Double) -> T
        ): T {
            contract {
                callsInPlace(emit, InvocationKind.EXACTLY_ONCE)
            }
            val t1 = (1 - t)
            val a = t1 * t1
            val c = t * t
            val b = 2 * t1 * t
            return emit(
                a * x0 + b * xc + c * x1,
                a * y0 + b * yc + c * y1
            )
        }

        @OptIn(ExperimentalContracts::class)
        inline fun <T> cubicCalc(
            x0: Double, y0: Double, x1: Double, y1: Double,
            x2: Double, y2: Double, x3: Double, y3: Double,
            t: Double,
            emit: (x: Double, y: Double) -> T
        ): T {
            contract {
                callsInPlace(emit, InvocationKind.EXACTLY_ONCE)
            }

            val cx = 3f * (x1 - x0)
            val bx = 3f * (x2 - x1) - cx
            val ax = x3 - x0 - cx - bx

            val cy = 3f * (y1 - y0)
            val by = 3f * (y2 - y1) - cy
            val ay = y3 - y0 - cy - by

            val tSquared = t * t
            val tCubed = tSquared * t

            return emit(
                ax * tCubed + bx * tSquared + cx * t + x0,
                ay * tCubed + by * tSquared + cy * t + y0
            )
        }

        fun cubicCalc(
            p0: Point, p1: Point, p2: Point, p3: Point,
            t: Float
        ): Point {
            var out: Point
            cubicCalc(p0.xD, p0.yD, p1.xD, p1.yD, p2.xD, p2.yD, p3.xD, p3.yD, t.toDouble()) { x, y -> out = Point(x, y) }
            return out
        }

        fun quadCalc(
            p: Point,
            c: Point,
            a: Point,
            t: Double,
        ): Point {
            var out: Point
            quadCalc(p.xD, p.yD, c.xD, c.yD, a.xD, a.yD, t) { x, y -> out = Point(x, y) }
            return out
        }

        @PublishedApi internal fun quadToCubic1(v0: Point, v1: Point): Point = v0 + (v1 - v0) * (2.0 / 3.0)
        @PublishedApi internal fun quadToCubic2(v1: Point, v2: Point): Point = v2 + (v1 - v2) * (2.0 / 3.0)

        @PublishedApi internal fun quadToCubic1(v0: Double, v1: Double) = v0 + (v1 - v0) * (2.0 / 3.0)
        @PublishedApi internal fun quadToCubic2(v1: Double, v2: Double) = v2 + (v1 - v2) * (2.0 / 3.0)

        //@InlineOnly
        @OptIn(ExperimentalContracts::class)
        inline fun <T> quadToCubic(
            x0: Double, y0: Double, xc: Double, yc: Double, x1: Double, y1: Double,
            bezier: (qx0: Double, qy0: Double, qx1: Double, qy1: Double, qx2: Double, qy2: Double, qx3: Double, qy3: Double) -> T
        ): T {
            contract {
                callsInPlace(bezier, InvocationKind.EXACTLY_ONCE)
            }
            return bezier(
                x0, y0,
                quadToCubic1(x0, xc), quadToCubic1(y0, yc),
                quadToCubic2(xc, x1), quadToCubic2(yc, y1),
                x1, y1
            )
        }
    }
}

fun MLine.toBezier(): Bezier = Bezier(Point(x0, y0), Point(x1, y1))
