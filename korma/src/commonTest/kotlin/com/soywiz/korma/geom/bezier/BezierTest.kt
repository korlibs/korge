package com.soywiz.korma.geom.bezier

import com.soywiz.korma.geom.Point
import kotlin.test.Test
import kotlin.test.assertEquals

class BezierTest {
    @Test
    fun testLength() {
        assertEquals(100.0, Bezier(Point(0, 0), Point(50, 0), Point(100, 0)).length(steps = 100))
    }
}
