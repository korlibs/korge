package com.soywiz.korma.geom.triangle.triangulate

import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.triangle.triangulate.*
import kotlin.test.*

class TriangulateTest {
    @Test
    fun test() {
        assertEquals(
            "[[Triangle((0, 100), (100, 0), (100, 100)), Triangle((0, 100), (0, 0), (100, 0))], [Triangle((300, 100), (400, 0), (400, 100)), Triangle((300, 100), (300, 0), (400, 0))]]",
            VectorPath {
                rect(0, 0, 100, 100)
                rect(300, 0, 100, 100)
            }.triangulate().toString()
        )
    }

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

    @Test
    fun test3() {
        val points = listOf(
            Point(-371.182, 307.381),
            Point(-365.909, 310.721),
            Point(-369.425, 318.455),
            Point(-373.468, 317.048),
            Point(-375.401, 315.818),
            Point(-376.28, 314.588),
            Point(-376.28, 313.357),
            Point(-375.753, 311.599),
            Point(-373.643, 307.733),
            Point(-372.764, 307.029),
            Point(-372.061, 307.029)
        )

        assertEquals(
            "[Triangle((-375.401, 315.818), (-376.28, 314.588), (-376.28, 313.357)), Triangle((-375.401, 315.818), (-376.28, 313.357), (-375.753, 311.599)), Triangle((-373.468, 317.048), (-375.401, 315.818), (-375.753, 311.599)), Triangle((-371.182, 307.381), (-373.468, 317.048), (-375.753, 311.599)), Triangle((-373.643, 307.733), (-371.182, 307.381), (-375.753, 311.599)), Triangle((-373.643, 307.733), (-372.061, 307.029), (-371.182, 307.381)), Triangle((-373.643, 307.733), (-372.764, 307.029), (-372.061, 307.029)), Triangle((-365.909, 310.721), (-373.468, 317.048), (-371.182, 307.381)), Triangle((-365.909, 310.721), (-369.425, 318.455), (-373.468, 317.048))]",
            points.triangulate().toString()
        )
    }
}
