package com.soywiz.korma.geom.bezier

import com.soywiz.korma.geom.Point

object SegmentEmitter {
    inline fun emit(
        segments: Int,
        crossinline curveGen: (p: Point, t: Double) -> Point,
        crossinline gen: (p0: Point, p1: Point) -> Unit,
        p1: Point = Point(),
        p2: Point = Point()
    ) {
        val dt = 1.0 / segments
        for (n in 0 until segments) {
            val pp1 = p2
            val pp2 = curveGen(p2, dt * n)
            if (n > 1) {
                gen(pp1, pp2)
            }
        }
    }
}
