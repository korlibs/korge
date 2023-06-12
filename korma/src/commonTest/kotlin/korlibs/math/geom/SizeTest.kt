package korlibs.math.geom

import kotlin.test.*

class SizeTest {
    @Test
    fun test() {
        assertEquals(Point(40, 60), Point(10, 20) + Size(30, 40))
        assertEquals(Point(20, 60), Point(10, 20) * Scale(2f, 3f))
    }
}
