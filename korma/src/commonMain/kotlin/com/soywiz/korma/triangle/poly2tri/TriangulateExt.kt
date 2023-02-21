package com.soywiz.korma.triangle.poly2tri

import com.soywiz.kds.IntArrayList
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korma.geom.PointArrayList
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.shape.ops.internal.Clipper
import com.soywiz.korma.geom.shape.ops.internal.DefaultClipper
import com.soywiz.korma.geom.shape.ops.internal.executePaths
import com.soywiz.korma.geom.shape.ops.internal.toClipperPaths
import com.soywiz.korma.geom.shape.ops.internal.toPathList
import com.soywiz.korma.geom.triangle.TriangleList
import com.soywiz.korma.geom.vector.VectorPath

fun VectorPath.triangulateSafe(doClipper: Boolean = true): TriangleList {
    val pathList = if (doClipper) {
        val clipper = DefaultClipper()
        val path = this
        clipper.addPaths(path.toClipperPaths(), Clipper.PolyType.SUBJECT, true)
        clipper.executePaths(Clipper.ClipType.UNION).toPathList()
    } else {
        this.toPathPointList()
    }

    val sweep = Poly2Tri.SweepContext()
    sweep.addHoles(pathList)
    sweep.triangulate()
    val triangles = sweep.getTriangles()
    val points = PointArrayList(triangles.size * 3)
    val indices = IntArrayList(triangles.size * 3)
    triangles.fastForEach {
        indices.add(points.size + 0)
        indices.add(points.size + 1)
        indices.add(points.size + 2)
        points.add(it.p0.x, it.p0.y)
        points.add(it.p1.x, it.p1.y)
        points.add(it.p2.x, it.p2.y)
    }
    return TriangleList(points, indices.toShortArray())
}
