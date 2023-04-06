package korlibs.math.geom.bezier

import korlibs.math.geom.*
import korlibs.math.interpolation.*

interface Curve {
    val order: Int
    fun getBounds(): Rectangle
    fun normal(t: Float): Point
    fun tangent(t: Float): Point
    fun calc(t: Float): Point
    fun ratioFromLength(length: Float): Float = TODO()
    val length: Float
    // @TODO: We should probably have a function to get ratios in the function to place the points maybe based on inflection points?
    fun recommendedDivisions(): Int = DEFAULT_STEPS
    fun calcOffset(t: Float, offset: Float): Point = calc(t) + normal(t) * offset


    companion object {
        const val DEFAULT_STEPS = 100
    }
}

@PublishedApi
internal fun Curve._getPoints(count: Int = this.recommendedDivisions(), equidistant: Boolean = false, out: PointArrayList = PointArrayList()): PointList {
    val curveLength = length
    Ratio.forEachRatio(count) { ratio ->
        val t = if (equidistant) ratioFromLength(ratio.toFloat() * curveLength) else ratio.toFloat()
        //println("${this::class.simpleName}: ratio: $ratio, point=$point")
        out.add(calc(t))
    }
    return out
}

fun Curve.getPoints(count: Int = this.recommendedDivisions(), out: PointArrayList = PointArrayList()): PointList {
    return _getPoints(count, equidistant = false, out = out)
}

fun Curve.getEquidistantPoints(count: Int = this.recommendedDivisions(), out: PointArrayList = PointArrayList()): PointList {
    return _getPoints(count, equidistant = true, out = out)
}
