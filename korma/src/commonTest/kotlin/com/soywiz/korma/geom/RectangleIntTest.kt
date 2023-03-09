package com.soywiz.korma.geom

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RectangleIntTest {
    @Test
    fun name() {
        assertEquals(MSizeInt(25, 100), MSizeInt(50, 200).fitTo(container = MSizeInt(100, 100)))
        assertEquals(MSizeInt(50, 200), MSizeInt(50, 200).fitTo(container = MSizeInt(100, 200)))
    }

    @Test
    fun corners() {
        val rectangle = MRectangleInt(1, 20, 300, 4000)
        assertEquals(MPointInt(1, 20), rectangle.topLeft)
        assertEquals(MPointInt(301, 20), rectangle.topRight)
        assertEquals(MPointInt(1, 4020), rectangle.bottomLeft)
        assertEquals(MPointInt(301, 4020), rectangle.bottomRight)

        val iRectangle = MRectangleInt(1000, 200, 30, 4)
        assertEquals(MPointInt(1000, 200), iRectangle.topLeft)
        assertEquals(MPointInt(1030, 200), iRectangle.topRight)
        assertEquals(MPointInt(1000, 204), iRectangle.bottomLeft)
        assertEquals(MPointInt(1030, 204), iRectangle.bottomRight)
    }
    
    @Test
    fun containsPointInside() {
        val rect = MRectangleInt(10, 20, 100, 200)
        val point = MPointInt(11, 21)

        assertTrue(point.double in rect)
        assertTrue(point in rect)
        assertTrue(rect.contains(point.x.toDouble(), point.y.toDouble()))
        assertTrue(rect.contains(point.x.toFloat(), point.y.toFloat()))
        assertTrue(rect.contains(point.x, point.y))
    }

    @Test
    fun doesNotContainPointToTheLeft() {
        val rect = MRectangleInt(10, 20, 100, 200)
        val point = MPointInt(9, 21)

        assertFalse(point.double in rect)
        assertFalse(point in rect)
        assertFalse(rect.contains(point.x.toDouble(), point.y.toDouble()))
        assertFalse(rect.contains(point.x.toFloat(), point.y.toFloat()))
        assertFalse(rect.contains(point.x, point.y))
    }

    @Test
    fun doesNotContainPointToTheTop() {
        val rect = MRectangleInt(10, 20, 100, 200)
        val point = MPointInt(11, 19)

        assertFalse(point.double in rect)
        assertFalse(point in rect)
        assertFalse(rect.contains(point.x.toDouble(), point.y.toDouble()))
        assertFalse(rect.contains(point.x.toFloat(), point.y.toFloat()))
        assertFalse(rect.contains(point.x, point.y))
    }

    @Test
    fun doesNotContainPointToTheRight() {
        val rect = MRectangleInt(10, 20, 100, 200)
        val point = MPointInt(110, 21)

        assertFalse(point.double in rect)
        assertFalse(point in rect)
        assertFalse(rect.contains(point.x.toDouble(), point.y.toDouble()))
        assertFalse(rect.contains(point.x.toFloat(), point.y.toFloat()))
        assertFalse(rect.contains(point.x, point.y))
    }

    @Test
    fun doesNotContainPointToTheBottom() {
        val rect = MRectangleInt(10, 20, 100, 200)
        val point = MPointInt(11, 220)

        assertFalse(point.double in rect)
        assertFalse(point in rect)
        assertFalse(rect.contains(point.x.toDouble(), point.y.toDouble()))
        assertFalse(rect.contains(point.x.toFloat(), point.y.toFloat()))
        assertFalse(rect.contains(point.x, point.y))
    }
}
