package korlibs.math.geom

import kotlin.test.Test

class CircleTest {
    @Test
    fun testProjectedPoint() {
        val circle = Circle(Point(100, 100), radius = 100f)
        // Simple inner positions
        assertEqualsFloat(Point(200, 100), circle.projectedPoint(Point(110, 100)))
        assertEqualsFloat(Point(0, 100), circle.projectedPoint(Point(90, 100)))
        assertEqualsFloat(Point(100, 0), circle.projectedPoint(Point(100, 90)))
        assertEqualsFloat(Point(100, 200), circle.projectedPoint(Point(100, 110)))

        // Simple outer positions
        assertEqualsFloat(Point(100, 200), circle.projectedPoint(Point(100, 500)))

        // @TODO: What to do here?
        assertEqualsFloat(Point(200, 100), circle.projectedPoint(Point(100, 100))) // Arbitrary angle
        //assertEquals(Point(100, 100), circle.projectedPoint(Point(100, 100))) // Uses center as point
    }
}
