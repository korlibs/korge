package korlibs.math.geom.shape

import korlibs.math.geom.*
import kotlin.test.*

class NShape2DTest {
    @Test
    fun test() {
        val circle = Circle(Point(100, 100), radius = 10f)
        assertEqualsFloat(314.15927f, circle.area, absoluteTolerance = 0.1)
        assertEqualsFloat(62.831852f, circle.perimeter, absoluteTolerance = 0.1)
        assertEqualsFloat(Point(100, 90), circle.projectedPoint(Point(100, 0)))
        assertEqualsFloat(-10f, circle.distance(Point(100, 100)))
        assertEqualsFloat(0f, circle.distance(Point(100, 90)))
        assertEqualsFloat(+10f, circle.distance(Point(100, 80)))
    }
}
