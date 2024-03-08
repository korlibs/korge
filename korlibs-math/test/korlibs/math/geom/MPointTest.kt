package korlibs.math.geom

import kotlin.test.*

class MPointTest {
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
}
