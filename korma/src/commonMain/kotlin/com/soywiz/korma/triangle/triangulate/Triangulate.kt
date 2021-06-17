package com.soywiz.korma.triangle.triangulate

import com.soywiz.kds.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.triangle.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.triangle.*
import com.soywiz.korma.triangle.internal.*
import kotlin.jvm.*

fun List<IPoint>.triangulate(): List<Triangle> {
    val sc = SweepContext(this)
    val s = Sweep(sc)
    s.triangulate()
    return sc.triangles.toList()
}

@JvmName("triangulateListPointArrayList")
fun List<IPointArrayList>.triangulate(): List<Triangle> {
    val sc = SweepContext()
    for (points in this) {
        if (points.orientation() == Orientation.CLOCK_WISE) {
            sc.addPolyline(points.toPoints())
        } else {
            sc.addHole(points.toPoints())
        }
    }
    val s = Sweep(sc)
    s.triangulate()
    return sc.triangles.toList()
}

fun VectorPath.triangulatePoly2tri(): TriangleList = this.toPathList().triangulatePoly2tri()

fun List<IPointArrayList>.triangulatePoly2tri(): TriangleList {
    val triangles = triangulate()
    val points = PointArrayList(triangles.size * 3)
    val indices = IntArrayList(triangles.size * 3)
    for (triangle in triangles) {
        indices.add(points.size + 0)
        indices.add(points.size + 1)
        indices.add(points.size + 2)
        points.add(triangle.p0)
        points.add(triangle.p1)
        points.add(triangle.p2)
    }
    return TriangleList(points, indices.toIntArray())
}


fun Shape2d.triangulate(): List<List<Triangle>> = this.paths.map { it.toPoints().triangulate() }
fun Shape2d.triangulateFlat(): List<Triangle> = triangulate().flatMap { it }

//fun VectorPath.triangulate(): List<List<Triangle>> = this.toPathList().triangulate()
fun VectorPath.triangulate(): List<Triangle> = this.toPathList().triangulate()

fun VectorPath.triangulateNew(): List<Triangle> {
    val points = arrayListOf<IPoint>()
    val pathList = this.toPathList()
    for (path in pathList) {
        path.fastForEach { x, y ->
            points.add(Point(x, y))
        }
    }
    val sc = SweepContext(points)
    for (path in pathList) {
        for (n in 0 until path.size - 1) {
            val x0 = path.getX(n)
            val y0 = path.getY(n)
            val x1 = path.getX(n + 1)
            val y1 = path.getY(n + 1)
            //sc.edgeContext.createEdge(Point(x0, y0), Point(x1, y1))
        }
    }
    sc.validateTriangle = { triangle ->
        false
    }
    val s = Sweep(sc)
    s.triangulate()
    return sc.triangles.toList()
}
