package com.soywiz.korma.geom.bezier

import com.soywiz.korma.geom.*
import kotlin.test.Test
import kotlin.test.assertEquals

class BezierCurveQuadraticTest {
    val b = Bezier(0.0, 0.0, 0.5, 1.0, 1.0, 0.0);
    @Test
    fun testSerializesCorrectly() {
        assertEquals("Bezier([(0, 0), (0.5, 1), (1, 0)])", b.toString())
    }

    @Test
    fun testHasTheCorrectApproximateLength() {
        assertEquals(1.4789428575453212, b.length)
    }

    @Test
    fun testHasTheExpectedDerivatePoints() {
        assertEquals(
            listOf(
                PointArrayList(1, 2, 1, -2),
                PointArrayList(0, -4),
            ),
            b.dpoints
        )
        assertEquals(MPoint(1, 2), b.derivative(0.0))
        assertEquals(MPoint(1, 0), b.derivative(0.5))
        assertEquals(MPoint(1, -2), b.derivative(1.0))
    }

    @Test
    fun testHasTheExpectedNormals() {
        assertEquals(MPoint(-0.8944271909999159, 0.4472135954999579), b.normal(0.0))
        assertEquals(MPoint(-0.0, 1.0), b.normal(0.5))
        assertEquals(MPoint(0.8944271909999159, 0.4472135954999579), b.normal(1.0))
    }

    @Test
    fun testHasTheCorrectInflectionPoint() {
        // quadratic curves by definition have no inflections
        assertEquals(emptyList(), b.inflections().toList())
    }

    @Test
    fun testHasTheCorrectAxisAlignedBoundingBox() {
        assertEquals(
            MRectangle.fromBounds(0.0, 0.0, 1.0, 0.5),
            b.boundingBox
        )
    }

    @Test
    fun testFromPointSet() {
        val M = MPoint(75, 25)
        val pts = listOf(MPoint(0, 0), M, MPoint(100, 100))

        run {
            val b = Bezier.quadraticFromPoints(pts[0], pts[1], pts[2])
            assertEqualsFloat(Bezier(0, 0, 100, 0, 100, 100), b)
            assertEqualsFloat(M, b.get(0.5))
        }

        run {
            val t = 0.25
            val b = Bezier.quadraticFromPoints(pts[0], pts[1], pts[2], t)
            assertEqualsFloat(Bezier(0.0, 0.0, 183.33, 50.0, 100.0, 100.0), b.roundDecimalPlaces(2))
            assertEqualsFloat(M, b.get(t))
        }
    }

    /*


  describe(`from point set`, () => {

     */
}
