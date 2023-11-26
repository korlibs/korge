package korlibs.math.geom

import korlibs.math.*
import korlibs.math.annotations.*
import kotlin.test.*

@OptIn(KormaExperimental::class)
class PointTest {
    @Test
    fun testPointCreation() {
        assertEquals("(0, 0)", Point().toString())
        assertEquals("(0, 0)", Point.ZERO.toString())
        assertEquals("(1, 2)", Point(1, 2).toString())
        assertEquals("(1, 2)", Point(1.0, 2.0).toString())
        assertEquals("(1, 2)", Point(1f, 2f).toString())
        assertEquals("(1, 2)", Point(1, 2).toString())
    }

    @Test
    fun testPointComparison() {
        assertTrue("zero1") { Point() == Point.ZERO }
        assertTrue("zero2") { Point() == Point() }
        assertTrue("nan1") { Point() != Point.NaN }
        assertTrue("comparison1") { Point() != Point(0f, 1f) }
        assertTrue("nan2") { !Point.ZERO.isNaN() }
        assertTrue("nan3") { Point.NaN.isNaN() }
        assertEquals(Point(1f, 3f), Point(0f, 1f) + Point(1f, 2f))
    }

    @Test
    fun testPointArithmetic() {
        //val a = Point(1, 2) + Point(3, 4)
        //println(a.x)
        //println(a.y)
        assertEquals(Point(4, 6), Point(1, 2) + Point(3, 4))
    }

    @Test
    fun testPolarConstruction() {
        assertEqualsFloat(Point(10, 20), Point.polar(Point(10, 10), 90.degrees, 10.0, up = Vector2D.UP))
        assertEqualsFloat(Point(10, 0), Point.polar(Point(10, 10), 90.degrees, 10.0, up = Vector2D.UP_SCREEN))
    }

    private fun assertEquals(a: Point, b: Point, absoluteTolerance: Double = 1e-7) {
        assertTrue("Point $a != $b absoluteTolerance=$absoluteTolerance") {
            a.x.isAlmostEquals(b.x, absoluteTolerance) &&
                a.y.isAlmostEquals(b.y, absoluteTolerance)
        }
    }
}
