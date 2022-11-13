package com.soywiz.korim.vector

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.bezier.*
import com.soywiz.korma.geom.vector.*

fun VectorPath.sdf(width: Int, height: Int): FloatArray2 = sdf(FloatArray2(width, height, 0f))

// @TODO: We should optimize this as much as possible, to not query all the edges, but as few as possible!
// CHECK: https://math.stackexchange.com/questions/4079605/how-to-find-closest-point-to-polygon-shape-from-any-coordinate
fun VectorPath.sdf(data: FloatArray2): FloatArray2 {
    val path = this
    val curves = path.toCurves()
    val p = Point()
    val pp = Bezier.ProjectedPoint()
    for (y in 0 until data.height) {
        for (x in 0 until data.width) {
            p.setTo(x + 0.5, y + 0.5)
            val inside = path.containsPoint(p)
            var min = Double.POSITIVE_INFINITY
            curves.beziers.fastForEach { edge ->
                val result = edge.project(p, pp)
                min = kotlin.math.min(min, result.d)
                //val dist = Point.distance(result.p, p)
                //println()
            }
            if (inside) min = -min
            data[x, y] = min.toFloat()
        }
    }
    return data
}
