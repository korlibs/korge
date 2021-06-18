package com.soywiz.korma.geom.triangle.triangulate

import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.triangle.triangulate.*
import kotlin.test.*

class TriangulateTest {
    //@Test
    //fun test() {
    //    assertEquals(
    //        "[Triangle((0, 100), (100, 0), (100, 100)), Triangle((0, 100), (0, 0), (100, 0)), Triangle((300, 100), (400, 0), (400, 100)), Triangle((300, 100), (300, 0), (400, 0))]",
    //        VectorPath {
    //            rect(0, 0, 100, 100)
    //            rect(300, 0, 100, 100)
    //        }.triangulate().toString()
    //    )
    //}

    @Test
    fun test2() {
        val points = listOf(
            Point(3, 10),
            Point(1, 5),
            Point(3, 1),
            Point(4, 0),
            Point(6, 0)
        )

        assertEquals(
            "[Triangle((3, 10), (1, 5), (6, 0)), Triangle((3, 1), (6, 0), (1, 5)), Triangle((6, 0), (3, 1), (4, 0))]",
            points.triangulate().toString()
        )
    }
}
