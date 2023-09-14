package korlibs.math.geom

import korlibs.memory.*
import kotlin.test.*

class PointFixedTest {
    @Test
    fun testSimple() {
        val point = PointFixed(1.fixed, 2.fixed)
        assertEquals("(1.00, 2.00)", point.toString())
        assertEquals("(-2.00, -4.00)", (point * (-2).fixed).toString())
    }
}
