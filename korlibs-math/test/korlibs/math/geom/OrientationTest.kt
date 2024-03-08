package korlibs.math.geom

import kotlin.test.*

class OrientationTest {
    @Test
    fun test() {
        assertEquals(Orientation.CLOCK_WISE, Orientation.orient2d(Point(0, 0), Point(0, 10), Point(10, 0)))
        assertEquals(Orientation.COUNTER_CLOCK_WISE, Orientation.orient2d(Point(10, 0), Point(0, 10), Point(0, 0)))
        assertEquals(Orientation.COLLINEAR, Orientation.orient2d(Point(0, 0), Point(5, 0), Point(10, 0)))
    }

    @Test
    fun testOrientation() {
        assertEquals(Orientation.CLOCK_WISE, Orientation.orient2d(Point(0, 0), Point(0, 100), Point(100, 0)))
        assertEquals(Orientation.COUNTER_CLOCK_WISE, Orientation.orient2d(Point(0, 0), Point(0, 100), Point(100, 0), up = Vector2D.UP_SCREEN))
    }


    //@Test
    //fun test3D() {
    //    assertEquals(Orientation.COUNTER_CLOCK_WISE, Orientation.orient3d(Vector3.ZERO, Vector3.UP, Vector3.RIGHT))
    //    assertEquals(Orientation.CLOCK_WISE, Orientation.orient3d(Vector3.ZERO, Vector3.UP, Vector3.LEFT))
    //}
}
