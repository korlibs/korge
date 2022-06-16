package com.soywiz.korma.geom.bezier

import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.map
import com.soywiz.korma.geom.shape.buildVectorPath
import com.soywiz.korma.geom.vector.getCurves
import com.soywiz.korma.geom.vector.getCurvesList
import com.soywiz.korma.geom.vector.rect
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals

class BezierTest {
    @Test
    fun testLength() {
        assertEquals(100.0, Bezier(Point(0, 0), Point(50, 0), Point(100, 0)).length)
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

    @Test
    fun testCurveList() {
        val path = buildVectorPath {
            rect(0, 0, 100, 100)
            rect(300, 0, 100, 100)
        }
        val curves = path.getCurvesList()
        assertEquals(2, curves.size)
        assertEquals(Rectangle(0, 0, 100, 100), curves[0].getBounds())
        assertEquals(Rectangle(300, 0, 100, 100), curves[1].getBounds())
        assertEquals(Rectangle(0, 0, 400, 100), curves.toCurves().getBounds())
    }

    @Test
    fun testTangent() {
        val bezier = Bezier(Point(74.58, 36.96), Point(74.58, 36.96), Point(77.04, 27.36), Point(71.76, 32.64))
        assertEquals(Point(0.2482, -0.9687), bezier.tangent(0.0).setToRoundDecimalPlaces(4))
    }
}
