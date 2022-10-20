package com.soywiz.korma.geom.parallelogram

import com.soywiz.kds.*

data class ParallelogramInt(
    val x0a: Int, val x0b: Int, val y0: Int,
    val x1a: Int, val x1b: Int, val y1: Int,
) {
    fun triangulate(out: MutableList<TriangleInt> = fastArrayListOf()): List<TriangleInt> {
        when {
            x0a == x0b -> out.add(TriangleInt(x1a, y1, x0a, y0, x1b, y1))
            x1a == x1b -> out.add(TriangleInt(x0a, y0, x0b, y0, x1a, y1))
            else -> {
                out.add(TriangleInt(x0a, y0, x0b, y0, x1a, y1))
                out.add(TriangleInt(x1a, y1, x0b, y0, x1b, y1))
            }
        }
        return out
    }
}
