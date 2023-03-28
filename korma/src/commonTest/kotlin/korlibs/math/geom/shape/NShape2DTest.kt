package korlibs.math.geom.shape

import korlibs.math.geom.*
import kotlin.test.*

class NShape2DTest {
    @Test
    fun test() {
        val circle = Circle(Point(100, 100), radius = 10f)
        assertEquals(314.15927f, circle.area, absoluteTolerance = 0.1f)
        assertEquals(62.831852f, circle.perimeter, absoluteTolerance = 0.1f)
        assertEquals(Point(100, 90), circle.projectedPoint(Point(100, 0)))
        assertEquals(-10f, circle.distance(Point(100, 100)))
        assertEquals(0f, circle.distance(Point(100, 90)))
        assertEquals(+10f, circle.distance(Point(100, 80)))
    }
}
