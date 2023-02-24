package com.soywiz.korma.triangle

import com.soywiz.kds.IntArrayList
import com.soywiz.korma.geom.IPointArrayList
import com.soywiz.korma.geom.Orientation
import com.soywiz.korma.geom.PointArrayList
import com.soywiz.korma.geom.orientation
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.triangle.TriangleList
import com.soywiz.korma.geom.vector.VectorPath
import mapbox.earcut.EarCut

object EarCutTriangulator {
    fun triangulate(points: PointArrayList, holeIndices: IntArray?): TriangleList {
        val floats = FloatArray(points.size * 2)
        for (n in 0 until points.size) {
            floats[n * 2 + 0] = points.getX(n).toFloat()
            floats[n * 2 + 1] = points.getY(n).toFloat()
        }
        val result = EarCut.earcut(floats, holeIndices, 2)
        return TriangleList(points, result.toShortArray())
    }
}

fun VectorPath.triangulateEarCut() = this.toPathPointList().triangulateEarCut()

fun List<IPointArrayList>.triangulateEarCut(): TriangleList {
    val allPoints = PointArrayList(this.sumOf { it.size })
    val holeIndices = IntArrayList()
    //var lastClockWise = true
    for (path in this) {
        //val clockWise = path.orientation() != Orientation.COUNTER_CLOCK_WISE
        if (path.orientation() == Orientation.COUNTER_CLOCK_WISE) {
            //if (lastClockWise != clockWise) {
            //lastClockWise = clockWise
            holeIndices.add(allPoints.size)
        }
        allPoints.add(path)
    }
    return EarCutTriangulator.triangulate(allPoints, holeIndices.toIntArray())
}
