package com.soywiz.korma.geom.bezier

import com.soywiz.korma.geom.*
import kotlin.test.*

class BezierTest {
    @Test
    fun testLength() {
        assertEquals(100.0, Bezier(Point(0, 0), Point(50, 0), Point(100, 0)).length(steps = 100))
    }
}
