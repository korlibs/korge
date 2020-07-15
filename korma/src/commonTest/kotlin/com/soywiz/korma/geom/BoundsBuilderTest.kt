package com.soywiz.korma.geom

import kotlin.test.Test
import kotlin.test.assertEquals

class BoundsBuilderTest {
    @Test
    fun name() {
        val bb = BoundsBuilder()
        bb.add(Rectangle(20, 10, 200, 300))
        bb.add(Rectangle(2000, 70, 400, 50))
        bb.add(Rectangle(10000, 10000, 0, 0))
        assertEquals("Rectangle(x=20, y=10, width=2380, height=300)", bb.getBounds().toString())
        bb.reset()
        assertEquals("null", bb.getBoundsOrNull().toString())
        assertEquals("Rectangle(x=0, y=0, width=0, height=0)", bb.getBounds().toString())
        bb.add(Rectangle.fromBounds(0, 0, 1, 1))
        assertEquals("Rectangle(x=0, y=0, width=1, height=1)", bb.getBoundsOrNull().toString())
        assertEquals("Rectangle(x=0, y=0, width=1, height=1)", bb.getBounds().toString())
    }

    @Test
    fun test2() {
        val bb = BoundsBuilder()
            .add(-100, 100)
            .add(-90, 100)
            .add(-100, 110)
            .add(-90, 110)

        assertEquals(Rectangle(-100, 100, 10, 10), bb.getBounds())
    }
}
