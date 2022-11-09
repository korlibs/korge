package com.soywiz.korma.geom.trapezoid

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korma.geom.*
import kotlin.math.*

/**
 * https://en.wikipedia.org/wiki/Trapezoid
 *
 *   (x0a, y0)  (x0b, y0)
 *   +----------+
 *  /            \
 * +--------------+
 * (x1a, y1)     (x1b, y1)
 */
data class TrapezoidInt(
    val x0a: Int, val x0b: Int, val y0: Int,
    val x1a: Int, val x1b: Int, val y1: Int,
) {
    val height: Int get() = y1 - y0
    val baseA: Int get() = x0b - x0a
    val baseB: Int get() = x1b - x1a
    val area: Double get() = ((baseA + baseB) * height) / 2.0

    companion object {
        fun inside(
            x0a: Int, x0b: Int, y0: Int,
            x1a: Int, x1b: Int, y1: Int,
            x: Int, y: Int
        ): Boolean {
            if (y < y0 || y > y1) return false
            if ((x < x0a && x < x1a) || (x > x0b && x > x1b)) return false
            val sign1 = det(x1a - x, y1 - y, x0a - x, y0 - y).sign
            val sign2 = det(x1b - x, y1 - y, x0b - x, y0 - y).sign
            return sign1 != sign2
        }

        fun triangulate(
            x0a: Int, x0b: Int, y0: Int,
            x1a: Int, x1b: Int, y1: Int,
            out: FTrianglesInt = FTrianglesInt()
        ): FTrianglesInt {
            when {
                x0a == x0b -> out.add(x1a, y1, x0a, y0, x1b, y1)
                x1a == x1b -> out.add(x0a, y0, x0b, y0, x1a, y1)
                else -> {
                    out.add(x0a, y0, x0b, y0, x1a, y1)
                    out.add(x1a, y1, x0b, y0, x1b, y1)
                }
            }
            return out
        }

        private fun det(x0: Int, y0: Int, x1: Int, y1: Int) = (x0 * y1) - (x1 * y0)
    }

    fun inside(x: Int, y: Int): Boolean = inside(x0a, x0b, y0, x1a, x1b, y1, x, y)
    fun triangulate(out: FTrianglesInt = FTrianglesInt()): FTrianglesInt = Companion.triangulate(x0a, x0b, y0, x1a, x1b, y1, out)
}

fun List<TrapezoidInt>.pointInside(x: Int, y: Int, assumeSorted: Boolean = false): TrapezoidInt? {
    this.fastForEach {
        if (it.inside(x, y)) return it
    }
    return null
}
