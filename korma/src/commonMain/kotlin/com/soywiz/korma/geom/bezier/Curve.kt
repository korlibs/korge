package com.soywiz.korma.geom.bezier

import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*

fun Curve.calcOffset(t: Ratio, offset: Float): Point {
    val p = calc(t)
    val n = normal(t)
    return p + (n * offset)
}

interface Curve {
    val order: Int
    fun getBounds(target: MRectangle = MRectangle()): MRectangle
    fun normal(t: Ratio): Point
    fun tangent(t: Ratio): Point
    fun calc(t: Ratio): Point
    fun ratioFromLength(length: Double): Ratio = TODO()
    val length: Double
    // @TODO: We should probably have a function to get ratios in the function to place the points maybe based on inflection points?
    fun recommendedDivisions(): Int = DEFAULT_STEPS

    companion object {
        const val DEFAULT_STEPS = 100
    }
}

@PublishedApi
internal fun Curve._getPoints(count: Int = this.recommendedDivisions(), equidistant: Boolean = false, out: PointArrayList = PointArrayList()): IPointArrayList {
    val curveLength = length
    Ratio.forEachRatio(count) { ratio ->
        val t = if (equidistant) ratioFromLength(ratio.valueD * curveLength) else ratio
        //println("${this::class.simpleName}: ratio: $ratio, point=$point")
        out.add(calc(t))
    }
    return out
}

fun Curve.getPoints(count: Int = this.recommendedDivisions(), out: PointArrayList = PointArrayList()): IPointArrayList {
    return _getPoints(count, equidistant = false, out = out)
}

fun Curve.getEquidistantPoints(count: Int = this.recommendedDivisions(), out: PointArrayList = PointArrayList()): IPointArrayList {
    return _getPoints(count, equidistant = true, out = out)
}
