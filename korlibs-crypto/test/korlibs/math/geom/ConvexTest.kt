package korlibs.math.geom

import korlibs.math.geom.convex.*
import korlibs.math.geom.shape.*
import korlibs.math.geom.vector.*
import kotlin.test.*

class ConvexTest {
    @Test
    fun testRect() {
        assertEquals(true, isConvexPath { rect(0, 0, 100, 100) })
    }
    @Test
    fun testCircle() {
        assertEquals(true, isConvexPath { circle(Point(0, 0), 100f) })
    }
    @Test
    fun testRoundRect() {
        assertEquals(true, isConvexPath { roundRect(0, 0, 100, 100, 10, 10) })
    }
    @Test
    fun testRegularPolygon() {
        assertEquals(true, isConvexPath { regularPolygon(6, 100.0) })
    }
    @Test
    fun testStar() {
        assertEquals(false, isConvexPath { star(6, 50.0, 100.0) })
    }

    private fun isConvexPath(block: VectorPath.() -> Unit): Boolean {
        return Convex.isConvex(buildVectorPath { block() })
    }
}
