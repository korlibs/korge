package com.soywiz.korma.geom.bezier

import com.soywiz.kds.forEachRatio01
import com.soywiz.korma.geom.IPointArrayList
import com.soywiz.korma.geom.MPoint
import com.soywiz.korma.geom.PointArrayList
import com.soywiz.korma.geom.MRectangle

fun Curve.calcOffset(t: Double, offset: Double, out: MPoint = MPoint()): MPoint {
    calc(t, out)
    val px = out.x
    val py = out.y
    normal(t, out)
    val nx = out.x
    val ny = out.y
    return out.setTo(
        px + nx * offset,
        py + ny * offset,
    )
}

interface Curve {
    val order: Int
    fun getBounds(target: MRectangle = MRectangle()): MRectangle
    fun normal(t: Double, target: MPoint = MPoint()): MPoint
    fun tangent(t: Double, target: MPoint = MPoint()): MPoint
    fun calc(t: Double, target: MPoint = MPoint()): MPoint
    fun ratioFromLength(length: Double): Double = TODO()
    val length: Double
    // @TODO: We should probably have a function to get ratios in the function to place the points maybe based on inflection points?
    fun recommendedDivisions(): Int = DEFAULT_STEPS

    companion object {
        const val DEFAULT_STEPS = 100
    }
}

@PublishedApi
internal fun Curve._getPoints(count: Int = this.recommendedDivisions(), equidistant: Boolean = false, out: PointArrayList = PointArrayList()): IPointArrayList {
    val temp = MPoint()
    val curveLength = length
    forEachRatio01(count) { ratio ->
        val t = if (equidistant) ratioFromLength(ratio * curveLength) else ratio
        val point = calc(t, temp)
        //println("${this::class.simpleName}: ratio: $ratio, point=$point")
        out.add(point)
    }
    return out
}

fun Curve.getPoints(count: Int = this.recommendedDivisions(), out: PointArrayList = PointArrayList()): IPointArrayList {
    return _getPoints(count, equidistant = false, out = out)
}

fun Curve.getEquidistantPoints(count: Int = this.recommendedDivisions(), out: PointArrayList = PointArrayList()): IPointArrayList {
    return _getPoints(count, equidistant = true, out = out)
}
