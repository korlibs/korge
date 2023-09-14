package korlibs.math.geom.vector

import korlibs.math.geom.*
import kotlin.test.Test
import kotlin.test.assertEquals

class EdgeTest {
    @Test
    fun test() {
        // /|
        val a = MEdge(0, 10, 10, 0)
        val b = MEdge(10, 0, 10, 10)
        val c = MEdge().setToHalf(a, b)
        assertEqualsFloat(Point(10, 0), MEdge.getIntersectXY(a, b))
        assertEqualsFloat(Point(10, 0), MEdge.getIntersectXY(a, c))
        assertEquals(90.degrees, MEdge.angleBetween(MEdge(0, 0, 10, 0), MEdge(10, 0, 10, 10)))
    }
}
