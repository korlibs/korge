package com.soywiz.korma.geom

import com.soywiz.korma.annotations.*
import com.soywiz.korma.math.*
import kotlin.test.*

@OptIn(KormaExperimental::class)
class PointTest {
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

    private fun assertEquals(a: Point, b: Point, absoluteTolerance: Double = 1e-7) {
        assertTrue("Point $a != $b absoluteTolerance=$absoluteTolerance") {
            a.x.isAlmostEquals(b.x, absoluteTolerance) &&
                a.y.isAlmostEquals(b.y, absoluteTolerance)
        }
    }
}
