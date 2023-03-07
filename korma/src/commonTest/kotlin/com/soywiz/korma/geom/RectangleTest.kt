package com.soywiz.korma.geom

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RectangleTest {
    @Test
    fun name() {
        val big = MRectangle.fromBounds(0, 0, 50, 50)
        val small = MRectangle.fromBounds(10, 10, 20, 20)
        val out = MRectangle.fromBounds(100, 10, 200, 20)
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
        val r1 = MRectangle(20, 0, 30, 10)
        val r2 = MRectangle(100, 0, 100, 50)
        val ro = r1.copy()
        ro.setToAnchoredRectangle(ro, Anchor.MIDDLE_CENTER, r2)
        //Assert.assertEquals(Rectangle(0, 0, 0, 0), r1)
        assertEquals(MRectangle(135, 20, 30, 10), ro)
    }

    @Test
    fun testPlace() {
        val out =
            MRectangle(0, 0, 100, 100).place(MSize(50, 25), Anchor.MIDDLE_CENTER, ScaleMode.SHOW_ALL)
        assertEquals(MRectangle(0, 25, 100, 50), out)
    }

    @Test
    fun corners() {
        val rectangle = MRectangle(1, 20, 300, 4000)
        assertEquals(IPoint(1, 20), rectangle.topLeft.mutable)
        assertEquals(IPoint(301, 20), rectangle.topRight.mutable)
        assertEquals(IPoint(1, 4020), rectangle.bottomLeft.mutable)
        assertEquals(IPoint(301, 4020), rectangle.bottomRight.mutable)

        val iRectangle = IRectangle(1000, 200, 30, 4)
        assertEquals(IPoint(1000, 200), iRectangle.topLeft.mutable)
        assertEquals(IPoint(1030, 200), iRectangle.topRight.mutable)
        assertEquals(IPoint(1000, 204), iRectangle.bottomLeft.mutable)
        assertEquals(IPoint(1030, 204), iRectangle.bottomRight.mutable)
    }

    @Test
    fun containsPointInside() {
        val rect = IRectangle(10, 20, 100, 200)
        val point = IPointInt(11, 21)

        assertTrue(point.double in rect)
        assertTrue(point in rect)
        assertTrue(rect.contains(point.x.toDouble(), point.y.toDouble()))
        assertTrue(rect.contains(point.x.toFloat(), point.y.toFloat()))
        assertTrue(rect.contains(point.x, point.y))
    }

    @Test
    fun doesNotContainPointToTheLeft() {
        val rect = IRectangle(10, 20, 100, 200)
        val point = IPointInt(9, 21)

        assertFalse(point.double in rect)
        assertFalse(point in rect)
        assertFalse(rect.contains(point.x.toDouble(), point.y.toDouble()))
        assertFalse(rect.contains(point.x.toFloat(), point.y.toFloat()))
        assertFalse(rect.contains(point.x, point.y))
    }

    @Test
    fun doesNotContainPointToTheTop() {
        val rect = IRectangle(10, 20, 100, 200)
        val point = IPointInt(11, 19)

        assertFalse(point.double in rect)
        assertFalse(point in rect)
        assertFalse(rect.contains(point.x.toDouble(), point.y.toDouble()))
        assertFalse(rect.contains(point.x.toFloat(), point.y.toFloat()))
        assertFalse(rect.contains(point.x, point.y))
    }

    @Test
    fun doesNotContainPointToTheRight() {
        val rect = IRectangle(10, 20, 100, 200)
        val point = IPointInt(110, 21)

        assertFalse(point.double in rect)
        assertFalse(point in rect)
        assertFalse(rect.contains(point.x.toDouble(), point.y.toDouble()))
        assertFalse(rect.contains(point.x.toFloat(), point.y.toFloat()))
        assertFalse(rect.contains(point.x, point.y))
    }

    @Test
    fun doesNotContainPointToTheBottom() {
        val rect = IRectangle(10, 20, 100, 200)
        val point = IPointInt(11, 220)

        assertFalse(point.double in rect)
        assertFalse(point in rect)
        assertFalse(rect.contains(point.x.toDouble(), point.y.toDouble()))
        assertFalse(rect.contains(point.x.toFloat(), point.y.toFloat()))
        assertFalse(rect.contains(point.x, point.y))
    }

    @Test
    fun testMargin() {
        assertEquals(
            MRectangle.fromBounds(10, 10, 90, 90),
            MRectangle(0, 0, 100, 100).without(Margin(10f))
        )
        assertEquals(
            MRectangle.fromBounds(-10, -10, 110, 110),
            MRectangle(0, 0, 100, 100).with(Margin(10f))
        )
    }

    @Test
    fun testInt() {
        assertEquals(MRectangleInt(1, 2, 3, 4), MRectangle(1.1, 2.1, 3.1, 4.1).toInt())
    }

    @Test
    fun testExpand() {
        assertEquals(
            MRectangle.fromBounds(-10, -15, 120, 125),
            MRectangle.fromBounds(0, 0, 100, 100).expand(10, 15, 20, 25)
        )
        assertEquals(
            MRectangle.fromBounds(-10, -15, 120, 125),
            MRectangle.fromBounds(0, 0, 100, 100)
                .expand(Margin(left = 10f, top = 15f, right = 20f, bottom = 25f))
        )
        assertEquals(
            MRectangle.fromBounds(-10, -15, 120, 125),
            MRectangle.fromBounds(0, 0, 100, 100)
                .expand(MarginInt(left = 10, top = 15, right = 20, bottom = 25))
        )
    }

    @Test
    fun constructWithPoints() {
        assertEquals(
            IRectangle(MPoint(0, 0), MPoint(100, 100)),
            MRectangle(0, 0, 100, 100)
        )
        assertEquals(
            IRectangle(MPoint(100, 100), MPoint(0, 0)),
            MRectangle(0, 0, 100, 100)
        )
        assertEquals(
            IRectangle(MPoint(0, 100), MPoint(100, 0)),
            MRectangle(0, 0, 100, 100)
        )
        assertEquals(
            IRectangle(MPoint(100, 0), MPoint(0, 100)),
            MRectangle(0, 0, 100, 100)
        )
    }
}
