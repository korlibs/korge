package korlibs.math.geom.bezier

import korlibs.datastructure.*
import korlibs.math.geom.*
import korlibs.math.internal.*
import korlibs.math.interpolation.*
import korlibs.math.roundDecimalPlaces
import korlibs.memory.*
import korlibs.number.*

data class CurveLUT(val curve: Curve, val points: PointArrayList, val ts: FloatArrayList, private val _estimatedLengths: FloatArrayList) {
    constructor(curve: Curve, capacity: Int) : this(
        curve,
        PointArrayList(capacity),
        FloatArrayList(capacity),
        FloatArrayList(capacity)
    )

    val estimatedLengths: FloatArrayList get() {
        if (_estimatedLengths.isEmpty()) {
            _estimatedLengths.add(0f)
        }
        while (_estimatedLengths.size < size) {
            val pos = _estimatedLengths.size
            val prev = _estimatedLengths.last()
            _estimatedLengths.add(prev + Point.distance(points[pos - 1], points[pos]))
        }
        return _estimatedLengths
    }
    val estimatedLength: Float get() = estimatedLengths.last()
    val steps: Int get() = points.size - 1
    val size: Int get() = points.size

    fun clear() {
        points.clear()
        ts.clear()
        _estimatedLengths.clear()
    }

    fun add(t: Float, p: Point) {
        points.add(p)
        ts.add(t)
    }

    class ClosestResult(val mdistSq: Float, val mpos: Int) {
        val mdist: Float get() = kotlin.math.sqrt(mdistSq)
    }

    fun closest(point: Point): ClosestResult {
        var mdistSq: Float = Float.POSITIVE_INFINITY
        var mpos: Int = 0
        for (n in 0 until size) {
            val d = Point.distanceSquared(this.points[n], point).toFloat()
            if (d < mdistSq) {
                mdistSq = d
                mpos = n
            }
        }
        return ClosestResult(mdistSq = mdistSq, mpos = mpos)
    }

    data class Estimation(var point: Point = Point(), var ratio: Float = 0f, var length: Float = 0f) {
        fun roundDecimalDigits(places: Int): Estimation = Estimation(point.roundDecimalPlaces(places), ratio.roundDecimalPlaces(places), length.roundDecimalPlaces(places))
        override fun toString(): String = "Estimation(point=${point.niceStr}, ratio=${ratio.niceStr}, length=${length.niceStr})"
    }

    fun Estimation.setAtIndexRatio(index: Int, ratio: Float): Estimation {
        val ratio0 = ts[index]
        //println("estimatedLengths=$estimatedLengths")
        val length0 = estimatedLengths[index]
        val point0 = points[index]
        if (ratio == 0f) {
            this.ratio = ratio0
            this.length = length0
            this.point = point0
        } else {
            val ratio1 = ts[index + 1]
            val length1 = estimatedLengths[index + 1]
            val point1 = points[index + 1]
            this.ratio = ratio.toRatio().interpolate(ratio0, ratio1)
            this.length = ratio.toRatio().interpolate(length0, length1)
            this.point = ratio.toRatio().interpolate(point0, point1)
        }

        return this
    }

    private fun estimateAt(
        values: FloatArrayList,
        value: Float,
        out: Estimation = Estimation()
    ): Estimation {
        val result = values.binarySearch(value)
        if (result.found) return out.setAtIndexRatio(result.index, 0f)
        val index = result.nearIndex
        if (value <= 0.0) return out.setAtIndexRatio(0, 0f)
        if (index >= values.size - 1) return out.setAtIndexRatio(points.size - 1, 0f)
        // @TODO: Since we have the curve, we can try to be more accurate and actually find a better point between found points
        val ratio0 = values[index]
        val ratio1 = values[index + 1]
        val ratio = value.convertRange(ratio0, ratio1, 0f, 1f)
        return out.setAtIndexRatio(index, ratio)
    }

    fun estimateAtT(t: Float, out: Estimation = Estimation()): Estimation {
        return estimateAt(ts, t, out)
    }

    fun estimateAtEquidistantRatio(ratio: Float, out: Estimation = Estimation()): Estimation {
        return estimateAtLength(estimatedLength * ratio, out)
    }

    fun estimateAtLength(length: Float, out: Estimation = Estimation()): Estimation {
        return estimateAt(estimatedLengths, length, out)
    }

    fun toEquidistantLUT(out: CurveLUT = CurveLUT(curve, points.size)): CurveLUT {
        val steps = this.steps
        val length = estimatedLength
        val result = Estimation()
        Ratio.forEachRatio(steps) { ratio ->
            val len = ratio.convertToRange(0f, length)
            val est = estimateAtLength(len, result)
            add(est.ratio, est.point)
        }
        return out
    }

    override fun toString(): String =
        "CurveLUT[$curve](${
            (0 until size).joinToString(", ") {
                "${ts[it]},len=${estimatedLengths[it]}: ${points[it]}"
            }
        })"
}
