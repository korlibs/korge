package com.soywiz.korma.geom.parallelogram

import com.soywiz.kmem.*

data class SegmentInt(
    val x0: Int, val y0: Int, val x1: Int, val y1: Int
) {
    val dx: Int get() = x1 - x0 // run
    val dy: Int get() = y1 - y0 // rise
    val slope: Double get() = dy.toDouble() / dx.toDouble()
    val islope: Double get() = dx.toDouble() / dy.toDouble()
    val xMin: Int get() = minOf(x0, x1)
    val yMin: Int get() = minOf(y0, y1)
    val xMax: Int get() = maxOf(x0, x1)
    val yMax: Int get() = maxOf(y0, y1)
    fun x(y: Int): Int = x0 + ((y - y0) * islope).toIntRound()
    fun y(x: Int): Int = y0 + (slope * (x - x0)).toIntRound()
    fun containsX(x: Int): Boolean = x in xMin..xMax
    fun containsY(y: Int): Boolean = y in yMin..yMax

    companion object {
        inline fun getIntersectXY(Ax: Int, Ay: Int, Bx: Int, By: Int, Cx: Int, Cy: Int, Dx: Int, Dy: Int, out: (x: Int, y: Int) -> Unit): Boolean {
            val a1 = By - Ay
            val b1 = Ax - Bx
            val c1 = a1 * (Ax) + b1 * (Ay)
            val a2 = Dy - Cy
            val b2 = Cx - Dx
            val c2 = a2 * (Cx) + b2 * (Cy)
            val determinant = a1 * b2 - a2 * b1
            if (determinant == 0) return false
            val x = (b2 * c1 - b1 * c2) / determinant
            val y = (a1 * c2 - a2 * c1) / determinant
            out(x, y)
            return true
        }

        inline fun getIntersectXY(a: SegmentInt, b: SegmentInt, crossinline out: (x: Int, y: Int) -> Unit): Boolean =
            getIntersectXY(a.x0, a.y0, a.x1, a.y1, b.x0, b.y0, b.x1, b.y1, out)

        fun getIntersectY(a: SegmentInt, b: SegmentInt): Int {
            var outY = Int.MIN_VALUE
            getIntersectXY(a, b) { x, y -> outY = y }
            return outY
        }
    }

    fun getIntersectY(other: SegmentInt): Int = SegmentInt.getIntersectY(this, other)
}
