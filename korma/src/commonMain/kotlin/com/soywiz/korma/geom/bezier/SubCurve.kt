package com.soywiz.korma.geom.bezier

import com.soywiz.korma.geom.IPointArrayList
import com.soywiz.korma.geom.IRectangle
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.PointArrayList
import com.soywiz.korma.math.convertRange

data class CurveSplit(
    val base: BezierCurve,
    val left: SubBezierCurve,
    val right: SubBezierCurve,
    val t: Double,
    val hull: IPointArrayList?
) {
    val leftCurve: BezierCurve get() = left.curve
    val rightCurve: BezierCurve get() = right.curve
}

class SubBezierCurve(val curve: BezierCurve, val t1: Double, val t2: Double, val parent: BezierCurve?) {
    constructor(curve: BezierCurve) : this(curve, 0.0, 1.0, null)

    val boundingBox: IRectangle get() = curve.boundingBox

    companion object {
        private val LEFT = listOf(null, null, intArrayOf(0, 3, 5), intArrayOf(0, 4, 7, 9))
        private val RIGHT = listOf(null, null, intArrayOf(5, 4, 2), intArrayOf(9, 8, 6, 3))

        private fun BezierCurveFromIndices(indices: IntArray, points: IPointArrayList): BezierCurve {
            val p = PointArrayList(indices.size)
            for (index in indices) p.add(points, index)
            return BezierCurve(p)
        }
    }

    fun calc(t: Double, target: Point = Point()): Point = curve.calc(t.convertRange(t1, t2, 0.0, 1.0), target)

    private fun _split(t: Double, hull: IPointArrayList?, left: Boolean): SubBezierCurve {
        val rt = t.convertRange(0.0, 1.0, t1, t2)
        val rt1 = if (left) t1 else rt
        val rt2 = if (left) rt else t2
        // Line
        val curve = if (curve.order < 2) {
            val p1 = calc(rt1)
            val p2 = calc(rt2)
            BezierCurve(p1, p2)
        } else {
            val indices = if (left) LEFT else RIGHT
            BezierCurveFromIndices(indices[curve.order]!!, hull!!)
        }
        return SubBezierCurve(curve, rt1, rt2, parent)
    }

    private fun _splitLeft(t: Double, hull: IPointArrayList? = curve.hullOrNull(t)): SubBezierCurve = _split(t, hull, left = true)
    private fun _splitRight(t: Double, hull: IPointArrayList? = curve.hullOrNull(t)): SubBezierCurve = _split(t, hull, left = false)

    fun splitLeft(t: Double): SubBezierCurve = _splitLeft(t)
    fun splitRight(t: Double): SubBezierCurve = _splitRight(t)

    fun split(t: Double): CurveSplit {
        val hull = curve.hullOrNull(t)
        return CurveSplit(
            base = curve,
            t = t,
            left = _splitLeft(t, hull),
            right = _splitRight(t, hull),
            hull = hull
        )
    }

    override fun toString(): String = "SubBezierCurve[$t1..$t2]($curve)"
}
