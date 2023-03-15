package com.soywiz.korma.geom

import kotlin.test.*

class NewBoundsBuilderTest {
    @Test
    fun test() {
        var bb = NewBoundsBuilder()
        assertEquals(null, bb.rectOrNull())
        bb += Point(10, 10)
        assertEquals(Rectangle.fromBounds(Point(10, 10), Point(10, 10)), bb.rectOrNull())
        bb += Point(30, 30)
        assertEquals(Rectangle.fromBounds(Point(10, 10), Point(30, 30)), bb.rectOrNull())
    }

    @Test
    fun test2() {
        assertEquals(Rectangle.NaN, NewBoundsBuilder().rect)
        assertEquals(null, NewBoundsBuilder().rectOrNull())
        assertEquals(Rectangle.fromBounds(Point(10, 10), Point(10, 10)), NewBoundsBuilder(Point(10, 10)).rect)
        assertEquals(Rectangle.fromBounds(Point(5, 10), Point(15, 20)), NewBoundsBuilder(Point(5, 20), Point(15, 10)).rect)
        assertEquals(Rectangle.fromBounds(Point(-7, 10), Point(15, 23)), NewBoundsBuilder(Point(5, 20), Point(15, 10), Point(-7, 23)).rect)
        assertEquals(Rectangle.fromBounds(Point(-7, -10), Point(30, 23)), NewBoundsBuilder(Point(5, 20), Point(15, 10), Point(-7, 23), Point(30, -10)).rect)
    }
}
