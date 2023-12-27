package korlibs.math.geom


import kotlin.test.*

class NewBoundsBuilderTest {
    @Test
    fun test() {
        var bb = BoundsBuilder()
        assertEquals(null, bb.boundsOrNull())
        bb += Point(10, 10)
        assertEquals(Rectangle.fromBounds(Point(10, 10), Point(10, 10)), bb.boundsOrNull())
        bb += Point(30, 30)
        assertEquals(Rectangle.fromBounds(Point(10, 10), Point(30, 30)), bb.boundsOrNull())
    }

    @Test
    fun test2() {
        assertEquals(Rectangle.NaN, BoundsBuilder().bounds)
        assertEquals(null, BoundsBuilder().boundsOrNull())
        assertEquals(Rectangle.fromBounds(Point(10, 10), Point(10, 10)), BoundsBuilder(Point(10, 10)).bounds)
        assertEquals(Rectangle.fromBounds(Point(5, 10), Point(15, 20)), BoundsBuilder(Point(5, 20), Point(15, 10)).bounds)
        assertEquals(Rectangle.fromBounds(Point(-7, 10), Point(15, 23)), BoundsBuilder(Point(5, 20), Point(15, 10), Point(-7, 23)).bounds)
        assertEquals(Rectangle.fromBounds(Point(-7, -10), Point(30, 23)), BoundsBuilder(Point(5, 20), Point(15, 10), Point(-7, 23), Point(30, -10)).bounds)
    }
}
