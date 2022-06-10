package com.soywiz.korma.geom.bezier

import com.soywiz.korma.geom.Point
import kotlin.test.Test
import kotlin.test.assertEquals

class BezierCurveLinearTest {
    val b = BezierCurve(0, 0, 100, 100)

    @Test
    fun testLinearCurvesSerializesCorrectly() {
        assertEquals("BezierCurve([(0, 0), (100, 100)])", b.toString())
    }

    @Test
    fun testMidpointIsIndeedTheMidpoint() {
        assertEquals(Point(50, 50), b.compute(0.5))
    }
}
