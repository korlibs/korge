package com.soywiz.korma.geom.vector

import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.shape.*

class StrokeToFillTest {
    private inline fun path(stroke: Double = 2.0, crossinline block: VectorPath.() -> Unit) =
        buildPath { block() }.strokeToFill(stroke).getPoints2().toList().map { it.int }.toString()

    /*
    @Test
    fun testSimple() {
        assertEquals(
            "[(0, -1), (10, -1), (10, 1), (0, 1)]",
            path { line(0, 0, 10, 0) }
        )
    }

    @Test
    fun testSimple2() {
        assertEquals(
            "[(1, 0), (1, 10), (-1, 10), (-1, 0)]",
            path { line(0, 0, 0, 10) }
        )
    }
     */

    /*
    @Test
    fun testSimple3() {
        assertEquals(
            "--",
            path {
                line(0, 0, 10, 0)
                lineToV(10)
            }
        )
    }
     */
}
