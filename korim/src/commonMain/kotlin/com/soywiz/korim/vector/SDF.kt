package com.soywiz.korim.vector

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.bezier.*
import com.soywiz.korma.geom.vector.*

fun VectorPath.sdf(width: Int, height: Int): FloatArray2 = sdf(FloatArray2(width, height, 0f))

// @TODO: We should optimize this as much as possible, to not query all the edges, but as few as possible!
// CHECK: https://math.stackexchange.com/questions/4079605/how-to-find-closest-point-to-polygon-shape-from-any-coordinate
fun VectorPath.sdf(data: FloatArray2): FloatArray2 {
    val path = this
    val curvesList = path.toCurvesList()
    val p = Point()
    val pp = Bezier.ProjectedPoint()
    for (y in 0 until data.height) {
        for (x in 0 until data.width) {
            p.setTo(x + 0.5, y + 0.5)
            val inside = path.containsPoint(p)
            var min = Double.POSITIVE_INFINITY
            curvesList.fastForEach { curves ->
                //curves.beziers.size
                curves.beziers.fastForEachWithIndex { index, edge ->
                    //val color = index % 3
                    val result = edge.project(p, pp)
                    min = kotlin.math.min(min, result.d)
                    //val dist = Point.distance(result.p, p)
                    //println()
                }
            }
            if (inside) min = -min
            data[x, y] = min.toFloat()
        }
    }
    return data
}

private fun msdfColor(index: Int, size: Int, closed: Boolean): RGBA {
    if (size == 1) return Colors.WHITE
    if (index == 0) return Colors.FUCHSIA
    return if ((index - 1) % 2 == 0) Colors.YELLOW else Colors.CYAN
}

fun VectorPath.msdf(width: Int, height: Int): FloatBitmap32 = msdf(FloatBitmap32(width, height))

// @TODO: We should optimize this as much as possible, to not query all the edges, but as few as possible!
// CHECK: https://math.stackexchange.com/questions/4079605/how-to-find-closest-point-to-polygon-shape-from-any-coordinate
fun VectorPath.msdf(data: FloatBitmap32): FloatBitmap32 {
    val path = this
    val curvesList = path.toCurvesList()
    val p = Point()
    val pp = Bezier.ProjectedPoint()
    for (y in 0 until data.height) {
        for (x in 0 until data.width) {
            p.setTo(x + 0.5, y + 0.5)
            val inside = path.containsPoint(p)
            var minR = Double.POSITIVE_INFINITY
            var minG = Double.POSITIVE_INFINITY
            var minB = Double.POSITIVE_INFINITY
            var minA = Double.POSITIVE_INFINITY
            curvesList.fastForEach { curves ->
                val edgeCount = curves.beziers.size
                curves.beziers.fastForEachWithIndex { index, edge ->
                    val color = msdfColor(index, edgeCount, curves.closed)
                    val result = edge.project(p, pp)
                    if (color.r != 0) minR = kotlin.math.min(minR, result.d)
                    if (color.g != 0) minG = kotlin.math.min(minG, result.d)
                    if (color.b != 0) minB = kotlin.math.min(minB, result.d)
                    minA = kotlin.math.min(minA, result.d)
                    //val dist = Point.distance(result.p, p)
                    //println()
                }
            }
            if (inside) {
                minR = -minR
                minG = -minG
                minB = -minB
                minA = -minA
            }
            data.setRgbaf(x, y, minR.toFloat(), minG.toFloat(), minB.toFloat(), minA.toFloat())
        }
    }
    return data
}
