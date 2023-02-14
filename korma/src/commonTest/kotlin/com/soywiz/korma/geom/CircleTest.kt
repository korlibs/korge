package com.soywiz.korma.geom

import kotlin.test.Test

class CircleTest {
    @Test
    fun testProjectedPoint() {
        val circle = Circle(MPoint(100, 100), radius = 100.0)
        // Simple inner positions
        assertEquals(MPoint(200, 100), circle.projectedPoint(MPoint(110, 100)))
        assertEquals(MPoint(0, 100), circle.projectedPoint(MPoint(90, 100)))
        assertEquals(MPoint(100, 0), circle.projectedPoint(MPoint(100, 90)))
        assertEquals(MPoint(100, 200), circle.projectedPoint(MPoint(100, 110)))

        // Simple outer positions
        assertEquals(MPoint(100, 200), circle.projectedPoint(MPoint(100, 500)))

        // @TODO: What to do here?
        assertEquals(MPoint(200, 100), circle.projectedPoint(MPoint(100, 100))) // Arbitrary angle
        //assertEquals(Point(100, 100), circle.projectedPoint(Point(100, 100))) // Uses center as point
    }
}
