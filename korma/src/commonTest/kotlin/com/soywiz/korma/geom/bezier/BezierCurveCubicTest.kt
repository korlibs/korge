package com.soywiz.korma.geom.bezier

import com.soywiz.korma.geom.MPoint
import com.soywiz.korma.geom.PointArrayList
import com.soywiz.korma.geom.MRectangle
import kotlin.test.Test
import kotlin.test.assertEquals

class BezierCurveCubicTest {
    val b = Bezier(0, 0, 0, 1, 1, 1, 1, 0)

    @Test
    fun testSerializesCorrectly() {
        assertEquals("Bezier([(0, 0), (0, 1), (1, 1), (1, 0)])", b.toString())
    }

    @Test
    fun testHastheCorrectApproximateLength() {
        assertEquals(2.0, b.length, 0.0001)
    }

    @Test
    fun testHasTheExpectedDerivativePoints() {
        assertEquals(
            listOf(
                PointArrayList(0, 3, 3, 0, 0, -3),
                PointArrayList(6, -6, -6, -6),
                PointArrayList(-12, 0)
            ),
            b.dpoints
        )

        assertEquals(MPoint(0, 3), b.derivative(0.0))
        assertEquals(MPoint(1.5, 0.0), b.derivative(0.5))
        assertEquals(MPoint(0, -3), b.derivative(1.0))
    }

    @Test
    fun testHasTheExpectedNormals() {
        assertEquals(MPoint(-1, 0), b.normal(0.0))
        assertEquals(MPoint(-0.0, 1.0), b.normal(0.5))
        assertEquals(MPoint(1.0, 0.0), b.normal(1.0))
    }

    @Test
    fun testHasTheCorrectInflectionPoint() {
        assertEquals(emptyList(), b.inflections().toList())
    }

    @Test
    fun testHasTheCorrectAxisAlignedBoundingBox() {
        assertEquals(
            MRectangle.fromBounds(0.0, 0.0, 1.0, 0.75),
            b.boundingBox
        )
    }

    @Test
    fun testComplexCubicHasTheCorrectInflectionPoint() {
        val b = Bezier(0.0, 0.0, 1.0, 0.25, 0.0, 1.0, 1.0, 0.0)
        assertEquals(listOf(0.8, 0.5), b.inflections().toList())
    }

    @Test
    fun testFromPointSet() {
        val M = MPoint(200 / 3.0, 100 / 3.0)
        val pts = listOf(MPoint(0, 0), M, MPoint(100, 100))
        run {
            val b: Bezier = Bezier.cubicFromPoints(pts[0], pts[1], pts[2])
            assertEquals(
                Bezier(0.0, 0.0, 55.56, 11.11, 88.89, 44.44, 100.0, 100.0),
                b.roundDecimalPlaces(2)
            )
            val midpoint = b.get(0.5)
            assertEquals(midpoint.x, M.x, 0.00001)
            assertEquals(midpoint.y, M.y, 0.00001)
        }
        run {
            val t = 0.25
            val b = Bezier.cubicFromPoints(pts[0], pts[1], pts[2], t)
            val quarterpoint = b.get(t)
            assertEquals(quarterpoint.x, M.x, 0.00001)
            assertEquals(quarterpoint.y, M.y, 0.00001)
        }
    }
}
