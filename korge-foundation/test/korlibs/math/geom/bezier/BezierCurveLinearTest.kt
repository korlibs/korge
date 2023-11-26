package korlibs.math.geom.bezier

import korlibs.math.geom.*
import korlibs.math.interpolation.*
import kotlin.test.*

class BezierCurveLinearTest {
    val b = Bezier(Point(0, 0), Point(100, 100))

    @Test
    fun testLinearCurvesSerializesCorrectly() {
        assertEquals("Bezier([(0, 0), (100, 100)])", b.toString())
    }

    @Test
    fun testMidpointIsIndeedTheMidpoint() {
        assertEquals(Point(50, 50), b.compute(0.5.toRatio()))
    }
}
