package korlibs.math.geom

import korlibs.math.annotations.*
import korlibs.math.math.*
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
        assertEquals("(1, 2)", Point(Point(1, 2)).toString())
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
    fun testIPointCreation() {
        assertEquals("(0, 0)", MPoint().toString())
        assertEquals("(0, 0)", MPoint(0, 0).toString())
        assertEquals("(1, 2)", MPoint(1, 2).toString())
        assertEquals("(1, 2)", MPoint(1.0, 2.0).toString())
        assertEquals("(1, 2)", MPoint(1f, 2f).toString())
        assertEquals("(1, 2)", MPoint(MPoint(1, 2)).toString())
    }

    @Test
    fun testMPointCreation() {
        assertEquals("(0, 0)", MPoint().toString())
        assertEquals("(0, 0)", MPoint.Zero.toString())
        assertEquals("(1, 1)", MPoint.One.toString())
        assertEquals("(0, -1)", MPoint.Up.toString())
        assertEquals("(-1, 0)", MPoint.Left.toString())
        assertEquals("(1, 0)", MPoint.Right.toString())
        assertEquals("(0, 1)", MPoint.Down.toString())
        assertEquals("(1, 2)", MPoint(1, 2).toString())
        assertEquals("(1, 2)", MPoint(1.0, 2.0).toString())
        assertEquals("(1, 2)", MPoint(1f, 2f).toString())
        assertEquals("(1, 2)", MPoint(MPoint(1, 2)).toString())
    }

    @Test
    fun testPolar() {
        assertEquals("(10, 0)", MPoint.fromPolar(0.degrees, 10.0).toString())
        assertEquals("(0, 10)", MPoint.fromPolar(90.degrees, 10.0).toString())
        assertEquals("(-10, 0)", MPoint.fromPolar(180.degrees, 10.0).toString())
        assertEquals("(0, -10)", MPoint.fromPolar(270.degrees, 10.0).toString())
        assertEquals("(10, 0)", MPoint.fromPolar(360.degrees, 10.0).toString())

        assertEquals("(0, 5)", MPoint.fromPolar(0.degrees, 10.0).setToPolar(90.degrees, 5.0).toString())
    }

    @Test
    fun testPointArithmetic() {
        //val a = Point(1, 2) + Point(3, 4)
        //println(a.x)
        //println(a.y)
        assertEquals(Point(4, 6), Point(1, 2) + Point(3, 4))
    }

    private fun assertEquals(a: Point, b: Point, absoluteTolerance: Float = 1e-7f) {
        assertTrue("Point $a != $b absoluteTolerance=$absoluteTolerance") {
            a.x.isAlmostEquals(b.x, absoluteTolerance) &&
                a.y.isAlmostEquals(b.y, absoluteTolerance)
        }
    }
}