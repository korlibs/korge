package com.soywiz.korma.geom.bezier

import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.map
import com.soywiz.korma.geom.shape.buildVectorPath
import com.soywiz.korma.geom.vector.getCurves
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals

class BezierTest {
    @Test
    fun testLength() {
        assertEquals(100.0, Bezier(Point(0, 0), Point(50, 0), Point(100, 0)).length(steps = 100))
    }

    @Test
    fun testCurves() {
        val path = buildVectorPath {
            moveTo(200.0, 200.0)
            lineTo(400.0, 100.0)
            quadTo(400.0, 400.0, 200.0, 200.0)
        }
        val curves = path.getCurves()
        assertEquals(590.0, curves.length, 0.4)
        assertEquals(Rectangle(200, 100, 200, 180), curves.getBounds())
        assertEquals(
            """
                (200, 200)
                (228, 186)
                (256, 172)
                (283, 158)
                (311, 144)
                (339, 131)
                (367, 117)
                (394, 103)
                (399, 138)
                (395, 180)
                (389, 214)
                (379, 241)
                (367, 261)
                (352, 274)
                (334, 280)
                (313, 278)
                (289, 269)
                (262, 253)
                (232, 230)
                (200, 200)
            """.trimIndent(),
            curves.getPoints(20).map { x, y -> "(${x.roundToInt()}, ${y.roundToInt()})" }.joinToString("\n")
        )
    }
}
