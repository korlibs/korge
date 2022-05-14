package com.soywiz.korma.geom

import kotlin.test.Test

class CircleTest {
    @Test
    fun testProjectedPoint() {
        val circle = Circle(Point(100, 100), radius = 100.0)
        // Simple inner positions
        assertEquals(Point(200, 100), circle.projectedPoint(Point(110, 100)))
        assertEquals(Point(0, 100), circle.projectedPoint(Point(90, 100)))
        assertEquals(Point(100, 0), circle.projectedPoint(Point(100, 90)))
        assertEquals(Point(100, 200), circle.projectedPoint(Point(100, 110)))

        // Simple outer positions
        assertEquals(Point(100, 200), circle.projectedPoint(Point(100, 500)))

        // @TODO: What to do here?
        assertEquals(Point(200, 100), circle.projectedPoint(Point(100, 100))) // Arbitrary angle
        //assertEquals(Point(100, 100), circle.projectedPoint(Point(100, 100))) // Uses center as point
    }
}
