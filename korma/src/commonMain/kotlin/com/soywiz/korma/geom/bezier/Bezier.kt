package com.soywiz.korma.geom.bezier

import com.soywiz.kds.DoubleArrayList
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kds.mapDouble
import com.soywiz.kds.sort
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.IPoint
import com.soywiz.korma.geom.IPointArrayList
import com.soywiz.korma.geom.IRectangle
import com.soywiz.korma.geom.Line
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.PointArrayList
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.bottom
import com.soywiz.korma.geom.clone
import com.soywiz.korma.geom.fastForEach
import com.soywiz.korma.geom.firstX
import com.soywiz.korma.geom.firstY
import com.soywiz.korma.geom.get
import com.soywiz.korma.geom.getComponentList
import com.soywiz.korma.geom.getPoint
import com.soywiz.korma.geom.lastX
import com.soywiz.korma.geom.lastY
import com.soywiz.korma.geom.left
import com.soywiz.korma.geom.mapPoints
import com.soywiz.korma.geom.mutable
import com.soywiz.korma.geom.pointArrayListOf
import com.soywiz.korma.geom.radians
import com.soywiz.korma.geom.right
import com.soywiz.korma.geom.roundDecimalPlaces
import com.soywiz.korma.geom.setToRoundDecimalPlaces
import com.soywiz.korma.geom.top
import com.soywiz.korma.interpolation.interpolate
import com.soywiz.korma.math.convertRange
import com.soywiz.korma.math.isAlmostEquals
import com.soywiz.korma.math.isAlmostZero
import com.soywiz.korma.math.normalizeZero
import com.soywiz.korma.math.roundDecimalPlaces
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

interface IBezier : Curve {
    val points: IPointArrayList
    val dims: Int
    val dpoints: List<IPointArrayList>
    val direction: Angle
    val clockwise: Boolean
    val extrema: Bezier.Extrema
    val boundingBox: IRectangle
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
    fun get(t: Double, out: Point = Point()): IPoint = compute(t, out)

    fun getLUT(steps: Int = 100, out: CurveLUT = CurveLUT(this, steps + 1)): CurveLUT
    fun project(point: IPoint, out: Bezier.ProjectedPoint = Bezier.ProjectedPoint()): Bezier.ProjectedPoint
    fun inflections(): DoubleArray
    fun reduce(): List<SubBezier>
    fun overlaps(curve: Bezier): Boolean
    fun offset(d: Double): List<Bezier>
    fun offset(t: Double, d: Double, out: Point = Point()): IPoint
    fun scaleSimple(d: (Double) -> Double): Bezier
    fun selfIntersections(threshold: Double = 0.5, out: DoubleArrayList = DoubleArrayList()): DoubleArrayList
    fun intersections(line: Line): DoubleArray
    fun intersections(curve: Bezier, threshold: Double = 0.5): List<Pair<Double, Double>>
    fun compute(t: Double, out: Point = Point()): IPoint
}

/**
 * Port of the operations of the library Bezier.JS with some adjustments,
 * Original library created by Pomax: https://github.com/Pomax/bezierjs
 * Based on algorithms described here: https://pomax.github.io/bezierinfo/
 */
class Bezier(
    points: IPointArrayList,
) : IBezier {
    private val _points = PointArrayList(points.size).copyFrom(points)
    override val points: IPointArrayList get() = _points

    init {
        if (points.size > 4) error("Only supports quad and cubic beziers")
    }

    constructor() : this(PointArrayList(0.0, 0.0, 0.0, 0.0))
    constructor(vararg points: IPoint) : this(PointArrayList(*points))
    constructor(vararg points: Double) : this(PointArrayList(*points))
    constructor(vararg points: Float) : this(PointArrayList(*points))
    constructor(vararg points: Int) : this(PointArrayList(*points))
    @Deprecated("Boxing in K/N debug builds")
    constructor(vararg points: Number) : this(PointArrayList(*points))

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

    fun setPoints(points: IPointArrayList): Bezier = _setPoints { copyFrom(points) }

    fun setPoints(vararg points: IPoint): Bezier = _setPoints {
        points.fastForEach { add(it) }
    }

    fun setPoints(vararg points: Double): Bezier = _setPoints {
        addRaw(*points)
    }

    fun setPoints(x0: Double, y0: Double, x1: Double, y1: Double): Bezier = _setPoints {
        add(x0, y0)
        add(x1, y1)
    }

    fun setPoints(x0: Double, y0: Double, x1: Double, y1: Double, x2: Double, y2: Double): Bezier = _setPoints {
        add(x0, y0)
        add(x1, y1)
        add(x2, y2)
    }

    fun setPoints(x0: Double, y0: Double, x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double): Bezier = _setPoints {
        add(x0, y0)
        add(x1, y1)
        add(x2, y2)
        add(x3, y3)
    }

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
    }

    fun setToRoundDecimalPlaces(places: Int) {
        _points.setToRoundDecimalPlaces(places)
        invalidate()
    }
    fun roundDecimalPlaces(places: Int): Bezier = Bezier(points.roundDecimalPlaces(places))

    override fun getBounds(target: Rectangle): Rectangle = target.copyFrom(boundingBox)

    override fun calc(t: Double, target: Point): Point {
        this.compute(t, target)
        return target
    }

    override fun equals(other: Any?): Boolean = other is Bezier && this.points == other.points
    override fun hashCode(): Int = points.hashCode()
    override fun toString(): String = "Bezier($points)"

    override val dims: Int get() = 2 // Always 2D for now
    override val order: Int get() = points.size - 1

    private val _aligned: PointArrayList = PointArrayList(points.size)
    private val aligned: IPointArrayList get() {
        if (alignedValid) return _aligned
        alignedValid = true
        _aligned.clear()
        align(points, Line(points.firstX, points.firstY, points.lastX, points.lastY), _aligned)
        return _aligned
    }

    private var _dpoints: List<IPointArrayList> = emptyList()
    override val dpoints: List<IPointArrayList> get() {
        if (dpointsValid) return _dpoints
        dpointsValid = true
        _dpoints = derive(points)
        return _dpoints
    }

    private var _direction: Angle = Angle.ZERO
    override val direction: Angle get() {
        if (directionValid) return _direction
        directionValid = true
        _direction = Angle.between(
            points.getX(0), points.getY(0),
            points.getX(order), points.getY(order),
            points.getX(1), points.getY(1),
        )
        return _direction
    }
    override val clockwise: Boolean get() = direction > Angle.ZERO

    private var _extrema: Extrema = Extrema(EMPTY_DOUBLE_ARRAY, EMPTY_DOUBLE_ARRAY)
    override val extrema: Extrema get() {
        if (extremaValid) return _extrema
        extremaValid = true
        val out = (0 until dims).map { dim ->
            //println("extrema dim=$dim, p=${p.toList()}, droots=${droots(p).toList()}")
            var out = droots(dpoints[0].getComponentList(dim))
            if (order == 3) out = combineSmallDistinctSorted(out, droots(dpoints[1].getComponentList(dim)))
            out
        }
        _extrema = Extrema(out[0], out[1])
        return _extrema
    }

    private val _boundingBox: Rectangle = Rectangle()
    override val boundingBox: IRectangle get() {
        if (boundingBoxValid) return _boundingBox
        boundingBoxValid = true
        var xmin = 0.0
        var ymin = 0.0
        var xmax = 0.0
        var ymax = 0.0
        for (n in 0..1) {
            //println("extrema=$extrema")
            val ext = doubleArrayOf(0.0, *extrema.dimt(n), 1.0)
            var min = Double.POSITIVE_INFINITY
            var max = Double.NEGATIVE_INFINITY
            ext.fastForEach { t ->
                val p = get(t)
                val value = p[n]
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

        _boundingBox.setBounds(xmin, ymin, xmax, ymax)
        return _boundingBox
    }

    private val temp = Point()

    private var _length: Double = Double.NaN

    /** Calculates the length of this curve .*/
    override val length: Double get() {
        if (lengthValid) return _length
        lengthValid = true
        val z = 0.5
        var sum = 0.0

        for (i in T_VALUES.indices) {
            val t = z * T_VALUES[i] + z
            derivative(t, out = temp)
            sum += C_VALUES[i] * temp.length
        }
        _length = z * sum
        return _length
    }

    private val _lut = CurveLUT(this, 101)
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
        val baseLength = kotlin.math.hypot(
            points.getX(order) - points.getX(0),
            points.getY(order) - points.getY(0)
        )
        _isLinear = (0 until aligned.size).sumOf { aligned.getY(it).absoluteValue } < baseLength / 50.0
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
            val a1 = angle(this.points.getPoint(0), this.points.getPoint(3), this.points.getPoint(1))
            val a2 = angle(this.points.getPoint(0), this.points.getPoint(3), this.points.getPoint(2))
            if ((a1 > 0.0 && a2 < 0.0) || (a1 < 0.0 && a2 > 0.0)) {
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
            compute(t, out.temp)
            out.add(t, out.temp)
        }
        return out
    }

    override fun project(point: IPoint, out: ProjectedPoint): ProjectedPoint {
        val LUT = this.lut
        val l = LUT.steps.toDouble()
        val closest = LUT.closest(point)
        val mpos = closest.mpos
        val t1: Double = (mpos - 1) / l
        val t2: Double = (mpos + 1) / l
        val step: Double = 0.1 / l

        // step 2: fine check
        var mdist = closest.mdist
        var t = t1
        var ft = t
        mdist += 1
        while (t < t2 + step) {
            val p: IPoint = this.compute(t)
            val d = Point.distance(point, p)
            if (d < mdist) {
                mdist = d
                ft = t
            }
            t += step
        }
        out.p = this.compute(ft)
        out.t = when {
            ft < 0 -> 0.0
            ft > 1 -> 1.0
            else -> ft
        }
        out.d = mdist
        return out
    }

    data class ProjectedPoint(var p: IPoint = IPoint(), var t: Double = 0.0, var d: Double = 0.0) {
        fun roundDecimalPlaces(places: Int): ProjectedPoint = ProjectedPoint(
            p.mutable.setToRoundDecimalPlaces(places),
            t.roundDecimalPlaces(places),
            d.roundDecimalPlaces(places)
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
        val a = p.getX(2) * p.getY(1)
        val b = p.getX(3) * p.getY(1)
        val c = p.getX(1) * p.getY(2)
        val d = p.getX(3) * p.getY(2)
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
    override fun offset(t: Double, d: Double, out: Point): IPoint {
        // @TODO: Optimize to avoid allocations
        val pos = calc(t)
        val normal = normal(t)
        return out.copyFrom(pos + normal * d)
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
                this.points.getX(0) + (nv.x * r0),
                this.points.getY(0) + (nv.y * r0),
                this.points.getX(1) + (nv.x * r1),
                this.points.getY(1) + (nv.y * r1),
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
                            val p = np.getPoint(t * order)
                            val d = this.derivative(t.toDouble())
                            val p2 = Point(p.x + d.x, p.y + d.y)
                            np.add(lli4(p, p2, o, points.getPoint(t + 1)) ?: error("Invalid curve"))
                        }
                        else -> {
                            val t = n - 1
                            val pX = points.getX(t + 1)
                            val pY = points.getY(t + 1)
                            var ovX = pX - o.x
                            var ovY = pY - o.y
                            var rc = d((t + 1) / order.toDouble())
                            if (!clockwise) rc = -rc
                            val m = kotlin.math.hypot(ovX, ovY)
                            ovX /= m
                            ovY /= m
                            np.add(pX + rc * ovX, pY + rc * ovY)
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
        np.add(p.getX(0), p.getY(0))
        val k = p.size
        for (i in 1 until k) {
            val piX = p.getX(i)
            val piY = p.getY(i)
            val pimX = p.getX(i - 1)
            val pimY = p.getY(i - 1)
            np.add(
                ((k - i) / k) * piX + (i.toDouble() / k.toDouble()) * pimX,
                ((k - i) / k) * piY + (i.toDouble() / k.toDouble()) * pimY,
            )
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

    override fun intersections(line: Line): DoubleArray {
        val minX = line.minX
        val minY = line.minY
        val maxX = line.maxX
        val maxY = line.maxY
        return roots(this.points, line).filter { t ->
            val p = this.get(t)
            between(p.x, minX, maxX) && between(p.y, minY, maxY)
        }.toDoubleArray()
    }

    override fun intersections(curve: Bezier, threshold: Double): List<Pair<Double, Double>> =
        curveintersects(this.reduce(), curve.reduce(), threshold)

    /** Computes the point of the curve at [t] */
    override fun compute(t: Double, out: Point): IPoint = compute(t, this.points, out)

    /** The derivate vector of the curve at [t] (normalized when [normalize] is set to true) */
    fun derivative(t: Double, normalize: Boolean = false, out: Point = Point()): IPoint {
        compute(t, dpoints[0], out)
        if ((t == 0.0 || t == 1.0) && out.squaredLength.isAlmostZero()) {
            for (n in 0 until 10) {
                val newT = 10.0.pow(-(10 - n))
                val nt = if (t == 1.0) 1.0 - newT else newT
                //println("newT=$newT, nt=$nt")
                compute(nt, dpoints[0], out)
                if (!out.squaredLength.isAlmostZero()) break
            }
        }
        if (normalize) out.normalize()
        return out
    }

    /** The normal vector of the curve at [t] (normalized when [normalize] is set to true) */
    fun normal(t: Double, normalize: Boolean = true, out: Point = Point()): IPoint {
        derivative(t, normalize, out)
        return out.setToNormal()
        //return out.setTo(-out.y, out.x)
    }

    override fun normal(t: Double, target: Point): Point {
        normal(t, normalize = true, target)
        return target
    }

    override fun tangent(t: Double, target: Point): Point {
        derivative(t, normalize = true, out = target)
        return target
    }

    fun hull(t: Double, out: PointArrayList = PointArrayList()): IPointArrayList {
        if (order < 2) error("Can't compute hull of order=$order < 2")
        return hullOrNull(t, out)!!
    }

    fun curvature(t: Double, kOnly: Boolean = false): Curvature {
        return curvature(t, dpoints[0], dpoints[1], dims, kOnly)
    }

    fun hullOrNull(t: Double, out: PointArrayList = PointArrayList()): IPointArrayList? {
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
                val px = t.interpolate(p.getX(i), p.getX(i + 1))
                val py = t.interpolate(p.getY(i), p.getY(i + 1))
                out.add(px, py)
                next.add(px, py)
            }
            p = next
        }
        return out
    }

    fun split(t0: Double, t1: Double): SubBezier =
        SubBezier(splitRight(t0).splitLeft(map(t1, t0, 1.0, 0.0, 1.0)).curve, t0, t1, this)

    fun split(t: Double): CurveSplit = SubBezier(this).split(t)
    fun splitLeft(t: Double): SubBezier = SubBezier(this).splitLeft(t)
    fun splitRight(t: Double): SubBezier = SubBezier(this).splitRight(t)

    class Extrema(
        val xt: DoubleArray, val yt: DoubleArray
    ) {
        val allt: DoubleArray = combineSmallDistinctSorted(xt, yt)
        fun dimt(index: Int): DoubleArray = if (index == 0) xt else yt

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

    fun toLine(out: Bezier = Bezier()): Bezier {
        val x0 = points.getX(0)
        val y0 = points.getY(0)
        val x1 = points.getX(order)
        val y1 = points.getY(order)
        return out.setPoints(x0, y0, x1, y1)
    }

    fun toCubic(out: Bezier = Bezier()): Bezier {
        return when (order) {
            1 -> {
                val x0 = points.getX(0)
                val y0 = points.getY(0)
                val x1 = points.getX(1)
                val y1 = points.getY(1)
                val xd = x1 - x0
                val yd = y1 - y0
                val r1 = 1.0 / 3.0
                val r2 = 2.0 / 3.0
                out.setPoints(
                    x0, y0,
                    x0 + (xd * r1), y0 + (yd * r1),
                    x0 + (xd * r2), y0 + (yd * r2),
                    x1, y1,
                )
            }
            2 -> {
                val x0 = points.getX(0)
                val y0 = points.getY(0)
                val xc = points.getX(1)
                val yc = points.getY(1)
                val x1 = points.getX(2)
                val y1 = points.getY(2)
                out.setPoints(
                    x0, y0,
                    quadToCubic1(x0, xc), quadToCubic1(y0, yc),
                    quadToCubic2(xc, x1), quadToCubic2(yc, y1),
                    x1, y1
                )
            }
            3 -> out.copyFrom(this) // No conversion
            else -> TODO("Unsupported higher order curves")
        }
    }

    fun toQuad(out: Bezier = Bezier()): Bezier {
        return when (order) {
            1 -> {
                val x0 = points.getX(0)
                val y0 = points.getY(0)
                val x1 = points.getX(1)
                val y1 = points.getY(1)
                out.setPoints(
                    x0, y0,
                    (x0 + x1) * 0.5, (y0 + y1) * 0.5,
                    y0, y1,
                )
            }
            2 -> out.copyFrom(this) // No conversion
            3 -> {
                val x0 = points.getX(0)
                val y0 = points.getY(0)
                val xc1 = points.getX(1)
                val yc1 = points.getY(1)
                val xc2 = points.getX(2)
                val yc2 = points.getY(2)
                val x1 = points.getX(3)
                val y1 = points.getY(3)
                val xc = -0.25*x0 + .75*xc1 + .75*xc2 -0.25*x1
                val yc = -0.25*y0 + .75*yc1 + .75*yc2 -0.25*y1
                return out.setPoints(x0, y0, xc, yc, x1, y1)
            }
            else -> TODO("Unsupported higher order curves")
        }
    }

    fun toQuadList(): List<Bezier> {
        if (this.order == 2) return listOf(this)
        return toSimpleList().map { it.curve.toQuad() }
    }

    fun outline(d1: Double, d2: Double = d1, d3: Double = d1, d4: Double = d2): Curves {
        if (this.isLinear) {
            // TODO: find the actual extrema, because they might
            //       be before the start, or past the end.

            val n = this.normal(0.0);
            val start = this.points.getPoint(0)
            val end = this.points.getPoint(this.points.size - 1)

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

            val ls = makeline(bline.getPoint(2), fline.getPoint(0))
            val le = makeline(fline.getPoint(2), bline.getPoint(0))
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
        val fs = fcurves[0].points.getPoint(0)
        val fe = fcurves[len - 1].points.getPoint(fcurves[len - 1].points.size - 1)
        val bs = bcurves[len - 1].points.getPoint(bcurves[len - 1].points.size - 1)
        val be = bcurves[0].points.getPoint(0)
        val ls = makeline(bs, fs)
        val le = makeline(fe, be)

        return (listOf(ls) + fcurves + le + bcurves).toCurves(closed = true)
    }

    fun translate(dx: Double, dy: Double): Bezier =
        Bezier(points.mapPoints { x, y, out -> out.setTo(x + dx, y + dy) })

    fun transform(m: Matrix): Bezier =
        Bezier(points.mapPoints { x, y, out -> m.transform(x, y, out) })

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

        private fun curvature(t: Double, d1: IPointArrayList, d2: IPointArrayList, dims: Int, kOnly: Boolean = false): Curvature {
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
                qdsum.pow(3.0 / 2.0)
            }

            if (num == 0.0 || dnm == 0.0) {
                return Curvature(k = 0.0, r = 0.0)
            }

            val k = num / dnm
            val r = dnm / num

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

        val IRectangle.midX: Double get() = (left + right) * 0.5
        val IRectangle.midY: Double get() = (top + bottom) * 0.5

        private fun bboxoverlap(a: IRectangle, b: IRectangle): Boolean {
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

        //@Deprecated("", ReplaceWith("v.convertRange(ds, de, ts, te)", "com.soywiz.korma.math.convertRange"))
        private fun map(v: Double, ds: Double, de: Double, ts: Double, te: Double): Double {
            return v.convertRange(ds, de, ts, te)
            //val d1 = de - ds
            //val d2 = te - ts
            //val v2 = v - ds
            //val r = v2 / d1
            //return ts + d2 * r
        }

        private fun angle(o: IPoint, v1: IPoint, v2: IPoint): Double {
            val dx1 = v1.x - o.x
            val dy1 = v1.y - o.y
            val dx2 = v2.x - o.x
            val dy2 = v2.y - o.y
            val cross = dx1 * dy2 - dy1 * dx2
            val dot = dx1 * dx2 + dy1 * dy2
            return atan2(cross, dot)
        }

        private fun compute(t: Double, points: IPointArrayList, out: Point = Point()): IPoint {
            val p = points
            val order = p.size - 1
            if (t == 0.0) return p.getPoint(0, out)
            if (t == 1.0) return p.getPoint(order, out)
            if (order == 0) return p.getPoint(0, out)
            val mt = 1 - t
            val mt2 = mt * mt
            val t2 = t * t
            return when (order) {
                1 -> {
                    //println("compute: t=$t, mt=$mt")
                    out.setTo(
                        (mt * p.getX(0)) + (t * p.getX(1)),
                        (mt * p.getY(0)) + (t * p.getY(1)),
                    )
                }
                2 -> {
                    val a = mt2
                    val b = mt * t * 2
                    val c = t2

                    out.setTo(
                        a * p.getX(0) + b * p.getX(1) + c * p.getX(2),
                        a * p.getY(0) + b * p.getY(1) + c * p.getY(2),
                    )
                }
                3 -> {
                    val a = mt2 * mt
                    val b = mt2 * t * 3
                    val c = mt * t2 * 3
                    val d = t * t2

                    out.setTo(
                        a * p.getX(0) + b * p.getX(1) + c * p.getX(2) + d * p.getX(3),
                        a * p.getY(0) + b * p.getY(1) + c * p.getY(2) + d * p.getY(3),
                    )
                }
                else -> TODO("higher order curves")
            }
        }

        private fun derive(points: IPointArrayList): List<IPointArrayList> {
            val out = arrayListOf<IPointArrayList>()

            var current = points
            while (current.size >= 2) {
                val new = PointArrayList(current.size - 1)
                val c = (current.size - 1).toDouble()
                for (n in 0 until current.size - 1) {
                    new.add(
                        c * (current.getX(n + 1) - current.getX(n)),
                        c * (current.getY(n + 1) - current.getY(n)),
                    )
                }
                out.add(new)
                current = new
            }

            return out
        }

        private fun crt(v: Double): Double = if (v < 0.0) -(-v).pow(1.0 / 3.0) else v.pow(1.0 / 3.0)

        private fun align(
            points: IPointArrayList,
            line: Line,
            out: PointArrayList = PointArrayList()
        ): IPointArrayList {
            val p1 = line.a
            val p2 = line.b
            val tx = p1.x
            val ty = p1.y
            val a = -atan2(p2.y - ty, p2.x - tx)
            points.fastForEach { x, y ->
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

        private val X_AXIS = Line(0, 0, 1, 0)

        private fun roots(points: IPointArrayList, line: Line = X_AXIS): DoubleArray {
            val order = points.size - 1
            val aligned = align(points, line)

            if (order == 2) {
                val a: Double = aligned.getY(0)
                val b: Double = aligned.getY(1)
                val c: Double = aligned.getY(2)
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
            val pa: Double = aligned.getY(0)
            val pb: Double = aligned.getY(1)
            val pc: Double = aligned.getY(2)
            val pd: Double = aligned.getY(3)

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
                        (((r * (c1.t1 + c1.t2)) / 2.0).toInt()) / r.toDouble(),
                        (((r * (c2.t1 + c2.t2)) / 2.0).toInt()) / r.toDouble()
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

        private fun lli8(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double, x4: Double, y4: Double, out: Point = Point()): IPoint? {
            val d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)
            if (d == 0.0) return null
            val nx = (x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)
            val ny = (x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)
            return out.setTo(nx / d, ny / d)
        }

        private fun lli4(p1: IPoint, p2: IPoint, p3: IPoint, p4: IPoint, out: Point = Point()): IPoint? =
            lli8(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, p4.x, p4.y, out)

        fun cubicFromPoints(S: IPoint, B: IPoint, E: IPoint, t: Double = 0.5, d1: Double? = null): Bezier {
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
            return Bezier(S, nc1, nc2, E);
        }

        fun quadraticFromPoints(p1: IPoint, p2: IPoint, p3: IPoint, t: Double = 0.5): Bezier {
            // shortcuts, although they're really dumb
            if (t == 0.0) return Bezier(p2, p2, p3)
            if (t == 1.0) return Bezier(p1, p2, p2)
            // real fitting.
            val abc = Bezier.getABC(2, p1, p2, p3, t);
            return Bezier(p1, abc.A, p3);
        }

        private fun getABC(order: Int, S: IPoint, B: IPoint, E: IPoint, t: Double = 0.5): ABCResult {
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
            val A: IPoint,
            val B: IPoint,
            val C: IPoint,
            val S: IPoint,
            val E: IPoint
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

        private fun dist(p1: IPoint, p2: IPoint): Double {
            val dx = p1.x - p2.x
            val dy = p1.y - p2.y
            return kotlin.math.sqrt(dx * dx + dy * dy)
        }

        private fun combineSmallDistinctSorted(a: DoubleArray, b: DoubleArray): DoubleArray {
            val out = DoubleArrayList(a.size + b.size)
            out.add(a)
            b.fastForEach { if (!out.contains(it)) out.add(it) }
            out.sort()
            return out.toDoubleArray()
        }

        private fun makeline(p1: IPoint, p2: IPoint): Bezier =
            Bezier(p1.x, p1.y, (p1.x + p2.x) / 2, (p1.y + p2.y) / 2, p2.x, p2.y)

        inline fun <T> quadCalc(
            x0: Double, y0: Double,
            xc: Double, yc: Double,
            x1: Double, y1: Double,
            t: Double,
            emit: (x: Double, y: Double) -> T
        ): T {
            val t1 = (1 - t)
            val a = t1 * t1
            val c = t * t
            val b = 2 * t1 * t
            return emit(
                a * x0 + b * xc + c * x1,
                a * y0 + b * yc + c * y1
            )
        }

        inline fun <T> cubicCalc(
            x0: Double, y0: Double, x1: Double, y1: Double,
            x2: Double, y2: Double, x3: Double, y3: Double,
            t: Double,
            emit: (x: Double, y: Double) -> T
        ): T {
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
            p0: IPoint, p1: IPoint, p2: IPoint, p3: IPoint,
            t: Double, target: Point = Point()
        ): IPoint = cubicCalc(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, t, target)

        fun cubicCalc(
            x0: Double, y0: Double, x1: Double, y1: Double,
            x2: Double, y2: Double, x3: Double, y3: Double,
            t: Double, target: Point = Point()
        ): Point = cubicCalc(x0, y0, x1, y1, x2, y2, x3, y3, t) { x, y -> target.setTo(x, y) }

        fun quadCalc(
            p0: IPoint, p1: IPoint, p2: IPoint,
            t: Double, target: Point = Point()
        ): IPoint = quadCalc(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, t, target)

        fun quadCalc(
            x0: Double, y0: Double,
            xc: Double, yc: Double,
            x1: Double, y1: Double,
            t: Double,
            target: Point = Point()
        ): Point = quadCalc(x0, y0, xc, yc, x1, y1, t) { x, y -> target.setTo(x, y) }

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
