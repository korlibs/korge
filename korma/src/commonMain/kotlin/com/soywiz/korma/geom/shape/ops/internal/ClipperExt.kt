package com.soywiz.korma.geom.shape.ops.internal

import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.vector.*
import kotlin.math.*

fun Path.toPoints(): List<MPoint> = (0 until this.size).map { this@toPoints[it] }

fun Path.toShape2d(): Shape2d {
    if (this.size == 4) {
        for (n in 0 until 4) {
            val tl = this[(n + 0) % 4]
            val tr = this[(n + 1) % 4]
            val br = this[(n + 2) % 4]
            val bl = this[(n + 3) % 4]

            if ((tl.x == bl.x) && (tr.x == br.x) && (tl.y == tr.y) && (bl.y == br.y)) {
                val xmin = min(tl.x, tr.x)
                val xmax = max(tl.x, tr.x)
                val ymin = min(tl.y, bl.y)
                val ymax = max(tl.y, bl.y)
                //println("($xmin,$ymin)-($xmax-$ymax) : $tl,$tr,$br,$bl")
                return Shape2d.Rectangle(xmin, ymin, xmax - xmin, ymax - ymin)
            }
        }
    }
    // @TODO: Try to detect rectangle
    return Shape2d.Polygon(PointArrayList(this.toPoints()))
}

fun Paths.toShape2d(): Shape2d {
    return when (size) {
        0 -> Shape2d.Empty
        1 -> first().toShape2d()
        else -> Shape2d.Complex(this.map(Path::toShape2d))
    }
}

fun PointList.toClipperPath() = Path(toPoints())
fun List<PointList>.toClipperPaths() = Paths(this.map { it.toClipperPath() })
fun VectorPath.toClipperPaths() = this.toPathPointList().toClipperPaths()

fun Shape2d.clipperOp(other: Shape2d, op: Clipper.ClipType): Shape2d {
    val clipper = DefaultClipper()
    val solution = Paths()
    clipper.addPaths(this.paths.toClipperPaths(), Clipper.PolyType.CLIP, other.closed)
    clipper.addPaths(other.paths.toClipperPaths(), Clipper.PolyType.SUBJECT, other.closed)
    clipper.execute(op, solution)
    return solution.toShape2d()
}

fun VectorPath.clipperOp(other: VectorPath, op: Clipper.ClipType): Shape2d {
    val clipper = DefaultClipper()
    val solution = Paths()
    clipper.addPaths(this.toPathPointList().toClipperPaths(), Clipper.PolyType.CLIP, true)
    clipper.addPaths(other.toPathPointList().toClipperPaths(), Clipper.PolyType.SUBJECT, true)
    clipper.execute(op, solution)
    return solution.toShape2d()
}

fun LineCap.toClipper(): Clipper.EndType = when (this) {
    LineCap.BUTT -> Clipper.EndType.OPEN_BUTT
    LineCap.SQUARE -> Clipper.EndType.OPEN_SQUARE
    LineCap.ROUND -> Clipper.EndType.OPEN_ROUND
}

fun LineJoin.toClipper(): Clipper.JoinType = when (this) {
    LineJoin.BEVEL -> Clipper.JoinType.SQUARE
    LineJoin.ROUND -> Clipper.JoinType.ROUND
    LineJoin.MITER -> Clipper.JoinType.MITER
}

fun Paths.toPathList(): List<PointList> {
    val out = arrayListOf<PointList>()
    for (path in this) {
        val points = PointArrayList()
        for (point in path) {
            points.add(point.x, point.y)
        }
        out.add(points)
    }
    return out
}

fun Paths.toVectorPath(): VectorPath {
    return buildVectorPath(VectorPath()) {
        for (path in this@toVectorPath) {
            var first = true
            for (point in path) {
                if (first) {
                    first = false
                    moveTo(point.point)
                } else {
                    lineTo(point.point)
                }
            }
            close()
        }
    }
}
