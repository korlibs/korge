package korlibs.math.geom

import kotlin.test.*

class PointPoolTest {
    @Test
    fun test() {
        var called = false
        val area = PointPool(7)
        area {
            called = true
            assertEquals(Point(30, 30), Point(10, 10) + Point(20, 20))
            assertEquals(Point(30, 30), Point(10, 10) * 3)
        }
        assertEquals(true, called)
    }
}