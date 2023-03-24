package korlibs.math.geom

import kotlin.test.Test
import kotlin.test.assertEquals

class OrientationTest {
    @Test
    fun test() {
        assertEquals(Orientation.CLOCK_WISE, Orientation.orient2d(MPoint(0, 0), MPoint(0, 10), MPoint(10, 0)))
        assertEquals(Orientation.COUNTER_CLOCK_WISE, Orientation.orient2d(MPoint(10, 0), MPoint(0, 10), MPoint(0, 0)))
        assertEquals(Orientation.COLLINEAR, Orientation.orient2d(MPoint(0, 0), MPoint(5, 0), MPoint(10, 0)))
    }
}
