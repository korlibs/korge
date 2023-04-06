package korlibs.math.geom.bezier

import korlibs.math.geom.*
import korlibs.math.internal.*
import korlibs.math.math.*
import korlibs.memory.*

data class CurveSplit(
    val base: Bezier,
    val left: SubBezier,
    val right: SubBezier,
    val t: Float,
    val hull: PointList?
) {
    val leftCurve: Bezier get() = left.curve
    val rightCurve: Bezier get() = right.curve

    fun roundDecimalPlaces(places: Int) = CurveSplit(
        base.roundDecimalPlaces(places),
        left.roundDecimalPlaces(places),
        right.roundDecimalPlaces(places),
        t.roundDecimalPlaces(places),
        hull?.roundDecimalPlaces(places)
    )
}

class SubBezier(val curve: Bezier, val t1: Float, val t2: Float, val parent: Bezier?) {
    constructor(curve: Bezier) : this(curve, 0f, 1f, null)

    val boundingBox: Rectangle get() = curve.boundingBox

    companion object {
        private val LEFT = listOf(null, null, intArrayOf(0, 3, 5), intArrayOf(0, 4, 7, 9))
        private val RIGHT = listOf(null, null, intArrayOf(5, 4, 2), intArrayOf(9, 8, 6, 3))

        private fun BezierCurveFromIndices(indices: IntArray, points: PointList): Bezier {
            val p = PointArrayList(indices.size)
            for (index in indices) p.add(points, index)
            return Bezier(p)
        }
    }

    fun calc(t: Float): Point = curve.calc(t.convertRange(t1, t2, 0f, 1f))

    private fun _split(t: Float, hull: PointList?, left: Boolean): SubBezier {
        val rt = t.convertRange(0f, 1f, t1, t2)
        val rt1: Float = if (left) t1 else rt
        val rt2: Float = if (left) rt else t2
        // Line
        val curve = if (curve.order < 2) {
            val p1 = calc(rt1)
            val p2 = calc(rt2)
            Bezier(p1, p2)
        } else {
            val indices = if (left) LEFT else RIGHT
            BezierCurveFromIndices(indices[curve.order]!!, hull!!)
        }
        return SubBezier(curve, rt1, rt2, parent)
    }

    private fun _splitLeft(t: Float, hull: PointList? = curve.hullOrNull(t)): SubBezier = _split(t, hull, left = true)
    private fun _splitRight(t: Float, hull: PointList? = curve.hullOrNull(t)): SubBezier = _split(t, hull, left = false)

    fun splitLeft(t: Float): SubBezier = _splitLeft(t)
    fun splitRight(t: Float): SubBezier = _splitRight(t)

    fun split(t: Float): CurveSplit {
        val hull = curve.hullOrNull(t)
        return CurveSplit(
            base = curve,
            t = t,
            left = _splitLeft(t, hull),
            right = _splitRight(t, hull),
            hull = hull
        )
    }

    override fun toString(): String = "SubBezier[${t1.niceStr}..${t2.niceStr}]($curve)"
    fun roundDecimalPlaces(places: Int): SubBezier =
        SubBezier(curve.roundDecimalPlaces(places), t1.roundDecimalPlaces(places), t2.roundDecimalPlaces(places), parent?.roundDecimalPlaces(places))
}
