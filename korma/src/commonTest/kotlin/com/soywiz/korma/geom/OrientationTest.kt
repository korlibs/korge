package com.soywiz.korma.geom

import kotlin.test.*

class OrientationTest {
    @Test
    fun test() {
        assertEquals(Orientation.CLOCK_WISE, Orientation.orient2d(Point(0, 0), Point(0, 10), Point(10, 0)))
        assertEquals(Orientation.COUNTER_CLOCK_WISE, Orientation.orient2d(Point(10, 0), Point(0, 10), Point(0, 0)))
        assertEquals(Orientation.COLLINEAR, Orientation.orient2d(Point(0, 0), Point(5, 0), Point(10, 0)))
    }
}
