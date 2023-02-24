package com.soywiz.korma.geom.bezier

import com.soywiz.kds.DoubleArrayList
import com.soywiz.kds.binarySearch
import com.soywiz.kds.forEachRatio01
import com.soywiz.korma.geom.*
import com.soywiz.korma.internal.*
import com.soywiz.korma.interpolation.interpolate
import com.soywiz.korma.math.*

data class CurveLUT(val curve: Curve, val points: PointArrayList, val ts: DoubleArrayList, private val _estimatedLengths: DoubleArrayList) {
    constructor(curve: Curve, capacity: Int) : this(
        curve,
        PointArrayList(capacity),
        DoubleArrayList(capacity),
        DoubleArrayList(capacity)
    )

    val estimatedLengths: DoubleArrayList get() {
        if (_estimatedLengths.isEmpty()) {
            _estimatedLengths.add(0.0)
        }
        while (_estimatedLengths.size < size) {
            val pos = _estimatedLengths.size
            val prev = _estimatedLengths.last()
            _estimatedLengths.add(prev + MPoint.distance(points.getX(pos - 1), points.getY(pos - 1), points.getX(pos), points.getY(pos)))
        }
        return _estimatedLengths
    }
    val estimatedLength: Double get() = estimatedLengths.last()
    val steps: Int get() = points.size - 1
    val size: Int get() = points.size
    val temp = MPoint()

    fun clear() {
        points.clear()
        ts.clear()
        _estimatedLengths.clear()
    }

    fun add(t: Double, p: IPoint) {
        points.add(p)
        ts.add(t)
    }

    class ClosestResult(val mdistSq: Double, val mpos: Int) {
        val mdist: Double get() = kotlin.math.sqrt(mdistSq)
    }

    fun closest(point: IPoint): ClosestResult {
        var mdistSq: Double = Double.POSITIVE_INFINITY
        var mpos: Int = 0
        for (n in 0 until size) {
            val d = MPoint.distanceSquared(this.points.getX(n), this.points.getY(n), point.x, point.y)
            if (d < mdistSq) {
                mdistSq = d
                mpos = n
            }
        }
        return ClosestResult(mdistSq = mdistSq, mpos = mpos)
    }

    data class Estimation(var point: MPoint = MPoint(), var ratio: Double = 0.0, var length: Double = 0.0) {
        fun roundDecimalDigits(places: Int): Estimation = Estimation(point.copy().setToRoundDecimalPlaces(places), ratio.roundDecimalPlaces(places), length.roundDecimalPlaces(places))
        override fun toString(): String = "Estimation(point=${point.niceStr}, ratio=${ratio.niceStr}, length=${length.niceStr})"
    }

    fun Estimation.setAtIndexRatio(index: Int, ratio: Double): Estimation {
        val ratio0 = ts[index]
        //println("estimatedLengths=$estimatedLengths")
        val length0 = estimatedLengths[index]
        val pointX0 = points.getX(index)
        val pointY0 = points.getY(index)
        if (ratio == 0.0) {
            this.ratio = ratio0
            this.length = length0
            this.point.setTo(pointX0, pointY0)
        } else {
            val ratio1 = ts[index + 1]
            val length1 = estimatedLengths[index + 1]
            val pointX1 = points.getX(index + 1)
            val pointY1 = points.getY(index + 1)
            this.ratio = ratio.interpolate(ratio0, ratio1)
            this.length = ratio.interpolate(length0, length1)
            this.point.setToInterpolated(ratio, pointX0, pointY0, pointX1, pointY1)
        }

        return this
    }

    private fun estimateAt(
        values: DoubleArrayList,
        value: Double,
        out: Estimation = Estimation()
    ): Estimation {
        val result = values.binarySearch(value)
        if (result.found) return out.setAtIndexRatio(result.index, 0.0)
        val index = result.nearIndex
        if (value <= 0.0) return out.setAtIndexRatio(0, 0.0)
        if (index >= values.size - 1) return out.setAtIndexRatio(points.size - 1, 0.0)
        // @TODO: Since we have the curve, we can try to be more accurate and actually find a better point between found points
        val ratio0 = values[index]
        val ratio1 = values[index + 1]
        val ratio = value.convertRange(ratio0, ratio1, 0.0, 1.0)
        return out.setAtIndexRatio(index, ratio)
    }

    fun estimateAtT(t: Double, out: Estimation = Estimation()): Estimation {
        return estimateAt(ts, t, out)
    }

    fun estimateAtEquidistantRatio(ratio: Double, out: Estimation = Estimation()): Estimation {
        return estimateAtLength(estimatedLength * ratio, out)
    }

    fun estimateAtLength(length: Double, out: Estimation = Estimation()): Estimation {
        return estimateAt(estimatedLengths, length, out)
    }

    fun toEquidistantLUT(out: CurveLUT = CurveLUT(curve, points.size)): CurveLUT {
        val steps = this.steps
        val length = estimatedLength
        val result = Estimation()
        forEachRatio01(steps) { ratio ->
            val len = length * ratio
            val est = estimateAtLength(len, result)
            add(est.ratio, est.point)
        }
        return out
    }

    override fun toString(): String =
        "CurveLUT[$curve](${
            (0 until size).joinToString(", ") {
                "${ts[it]},len=${estimatedLengths[it]}: ${
                    points.getPoint(
                        it
                    )
                }"
            }
        })"
}
