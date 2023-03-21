package com.soywiz.korma.geom.bezier

import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*

interface Curve {
    val order: Int
    fun getBounds(): Rectangle
    fun normal(t: Double): Point
    fun tangent(t: Double): Point
    fun calc(t: Double): Point
    fun ratioFromLength(length: Double): Double = TODO()
    val length: Double
    // @TODO: We should probably have a function to get ratios in the function to place the points maybe based on inflection points?
    fun recommendedDivisions(): Int = DEFAULT_STEPS
    fun calcOffset(t: Double, offset: Double): Point = calc(t) + normal(t) * offset


    companion object {
        const val DEFAULT_STEPS = 100
    }
}

@PublishedApi
internal fun Curve._getPoints(count: Int = this.recommendedDivisions(), equidistant: Boolean = false, out: PointArrayList = PointArrayList()): PointList {
    val curveLength = length
    Ratio.forEachRatio(count) { ratio ->
        val t = if (equidistant) ratioFromLength(ratio.toDouble() * curveLength) else ratio.toDouble()
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
