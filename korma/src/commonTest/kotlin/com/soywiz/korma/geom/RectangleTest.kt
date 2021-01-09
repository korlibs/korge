package com.soywiz.korma.geom

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RectangleTest {
    @Test
    fun name() {
        val big = Rectangle.fromBounds(0, 0, 50, 50)
        val small = Rectangle.fromBounds(10, 10, 20, 20)
        val out = Rectangle.fromBounds(100, 10, 200, 20)
        assertTrue(small in big)
        assertTrue(big !in small)
        assertTrue(small == (small intersection big))
        assertTrue(small == (big intersection small))
        assertTrue(null == (big intersection out))
        assertTrue(small intersects big)
        assertTrue(big intersects small)
        assertFalse(big intersects out)
    }

    @Test
    fun name2() {
        val r1 = Rectangle(20, 0, 30, 10)
        val r2 = Rectangle(100, 0, 100, 50)
        val ro = r1.copy()
        ro.setToAnchoredRectangle(ro, Anchor.MIDDLE_CENTER, r2)
        //Assert.assertEquals(Rectangle(0, 0, 0, 0), r1)
        assertEquals(Rectangle(135, 20, 30, 10), ro)
    }

    @Test
    fun testPlace() {
        val out = Rectangle(0, 0, 100, 100).place(Size(50, 25), Anchor.MIDDLE_CENTER, ScaleMode.SHOW_ALL)
        assertEquals(Rectangle(0, 25, 100, 50), out)
    }

    @Test
    fun corners() {
        val rectangle = Rectangle(1, 20, 300, 4000)
        assertEquals(IPoint(1, 20), rectangle.topLeft)
        assertEquals(IPoint(301, 20), rectangle.topRight)
        assertEquals(IPoint(1, 4020), rectangle.bottomLeft)
        assertEquals(IPoint(301, 4020), rectangle.bottomRight)

        val iRectangle = IRectangle(1000, 200, 30, 4)
        assertEquals(IPoint(1000, 200), iRectangle.topLeft)
        assertEquals(IPoint(1030, 200), iRectangle.topRight)
        assertEquals(IPoint(1000, 204), iRectangle.bottomLeft)
        assertEquals(IPoint(1030, 204), iRectangle.bottomRight)
    }
}
