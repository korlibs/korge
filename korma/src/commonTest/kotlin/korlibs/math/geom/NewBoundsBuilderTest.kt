package korlibs.math.geom


import kotlin.test.*

class NewBoundsBuilderTest {
    @Test
    fun test() {
        var bb = NewBoundsBuilder()
        assertEquals(null, bb.boundsOrNull())
        bb += Point(10, 10)
        assertEquals(Rectangle.fromBounds(Point(10, 10), Point(10, 10)), bb.boundsOrNull())
        bb += Point(30, 30)
        assertEquals(Rectangle.fromBounds(Point(10, 10), Point(30, 30)), bb.boundsOrNull())
    }

    @Test
    fun test2() {
        assertEquals(Rectangle.NaN, NewBoundsBuilder().bounds)
        assertEquals(null, NewBoundsBuilder().boundsOrNull())
        assertEquals(Rectangle.fromBounds(Point(10, 10), Point(10, 10)), NewBoundsBuilder(Point(10, 10)).bounds)
        assertEquals(Rectangle.fromBounds(Point(5, 10), Point(15, 20)), NewBoundsBuilder(Point(5, 20), Point(15, 10)).bounds)
        assertEquals(Rectangle.fromBounds(Point(-7, 10), Point(15, 23)), NewBoundsBuilder(Point(5, 20), Point(15, 10), Point(-7, 23)).bounds)
        assertEquals(Rectangle.fromBounds(Point(-7, -10), Point(30, 23)), NewBoundsBuilder(Point(5, 20), Point(15, 10), Point(-7, 23), Point(30, -10)).bounds)
    }
}