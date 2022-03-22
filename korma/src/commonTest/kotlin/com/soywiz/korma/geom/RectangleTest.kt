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

    @Test
    fun containsPointInside() {
        val rect = IRectangle(10, 20, 100, 200)
        val point = IPointInt(11, 21)

        assertTrue(point.float in rect)
        assertTrue(point in rect)
        assertTrue(rect.contains(point.x.toDouble(), point.y.toDouble()))
        assertTrue(rect.contains(point.x.toFloat(), point.y.toFloat()))
        assertTrue(rect.contains(point.x, point.y))
    }

    @Test
    fun doesNotContainPointToTheLeft() {
        val rect = IRectangle(10, 20, 100, 200)
        val point = IPointInt(9, 21)

        assertFalse(point.float in rect)
        assertFalse(point in rect)
        assertFalse(rect.contains(point.x.toDouble(), point.y.toDouble()))
        assertFalse(rect.contains(point.x.toFloat(), point.y.toFloat()))
        assertFalse(rect.contains(point.x, point.y))
    }

    @Test
    fun doesNotContainPointToTheTop() {
        val rect = IRectangle(10, 20, 100, 200)
        val point = IPointInt(11, 19)

        assertFalse(point.float in rect)
        assertFalse(point in rect)
        assertFalse(rect.contains(point.x.toDouble(), point.y.toDouble()))
        assertFalse(rect.contains(point.x.toFloat(), point.y.toFloat()))
        assertFalse(rect.contains(point.x, point.y))
    }

    @Test
    fun doesNotContainPointToTheRight() {
        val rect = IRectangle(10, 20, 100, 200)
        val point = IPointInt(110, 21)

        assertFalse(point.float in rect)
        assertFalse(point in rect)
        assertFalse(rect.contains(point.x.toDouble(), point.y.toDouble()))
        assertFalse(rect.contains(point.x.toFloat(), point.y.toFloat()))
        assertFalse(rect.contains(point.x, point.y))
    }

    @Test
    fun doesNotContainPointToTheBottom() {
        val rect = IRectangle(10, 20, 100, 200)
        val point = IPointInt(11, 220)

        assertFalse(point.float in rect)
        assertFalse(point in rect)
        assertFalse(rect.contains(point.x.toDouble(), point.y.toDouble()))
        assertFalse(rect.contains(point.x.toFloat(), point.y.toFloat()))
        assertFalse(rect.contains(point.x, point.y))
    }

    @Test
    fun testMargin() {
        assertEquals(Rectangle.fromBounds(10, 10, 90, 90), Rectangle(0, 0, 100, 100).without(Margin(10.0)))
        assertEquals(Rectangle.fromBounds(-10, -10, 110, 110), Rectangle(0, 0, 100, 100).with(Margin(10.0)))
    }
    
    @Test
    fun testInt() {
        assertEquals(RectangleInt(1, 2, 3, 4), Rectangle(1.1, 2.1, 3.1, 4.1).toInt())
    }

    @Test
    fun testExpand() {
        assertEquals(Rectangle.fromBounds(-10, -15, 120, 125), Rectangle.fromBounds(0, 0, 100, 100).expand(10, 15, 20, 25))
        assertEquals(Rectangle.fromBounds(-10, -15, 120, 125), Rectangle.fromBounds(0, 0, 100, 100).expand(Margin(left = 10.0, top = 15.0, right = 20.0, bottom = 25.0)))
        assertEquals(Rectangle.fromBounds(-10, -15, 120, 125), Rectangle.fromBounds(0, 0, 100, 100).expand(MarginInt(left = 10, top = 15, right = 20, bottom = 25)))
    }
}
