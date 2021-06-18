package com.soywiz.korma.triangle.poly2tri

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.shape.ops.internal.*
import com.soywiz.korma.geom.triangle.*
import com.soywiz.korma.geom.vector.*

fun VectorPath.triangulateSafe(): TriangleList {
    val clipper = DefaultClipper()
    val path = this
    clipper.addPaths(path.toClipperPaths(), Clipper.PolyType.SUBJECT, true)
    val out = clipper.executePaths(Clipper.ClipType.UNION)
    val sweep = Poly2Tri.SweepContext()
    sweep.addHoles(out.toPathList())
    val triangles = sweep.triangulate().getTriangles()
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
    return TriangleList(points, indices.toIntArray())
}
