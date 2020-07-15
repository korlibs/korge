package com.soywiz.korma.geom

import kotlin.test.Test
import kotlin.test.assertEquals

class PointTest {
    @Test
    fun testPolar() {
        assertEquals("(10, 0)", Point.fromPolar(0.degrees, 10.0).toString())
        assertEquals("(0, 10)", Point.fromPolar(90.degrees, 10.0).toString())
        assertEquals("(-10, 0)", Point.fromPolar(180.degrees, 10.0).toString())
        assertEquals("(0, -10)", Point.fromPolar(270.degrees, 10.0).toString())
        assertEquals("(10, 0)", Point.fromPolar(360.degrees, 10.0).toString())

        assertEquals("(0, 5)", Point.fromPolar(0.degrees, 10.0).setToPolar(90.degrees, 5.0).toString())
    }
}
