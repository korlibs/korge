package com.soywiz.korma.geom.bezier

import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.PointArrayList
import com.soywiz.korma.geom.Rectangle
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class BezierCurveQuadraticTest {
    val b = BezierCurve(0.0, 0.0, 0.5, 1.0, 1.0, 0.0);
    @Test
    fun testSerializesCorrectly() {
        assertEquals("BezierCurve([(0, 0), (0.5, 1), (1, 0)])", b.toString())
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
        assertEquals(Point(1, 2), b.derivative(0.0))
        assertEquals(Point(1, 0), b.derivative(0.5))
        assertEquals(Point(1, -2), b.derivative(1.0))
    }

    @Test
    fun testHasTheExpectedNormals() {
        assertEquals(Point(-0.8944271909999159, 0.4472135954999579), b.normal(0.0))
        assertEquals(Point(-0.0, 1.0), b.normal(0.5))
        assertEquals(Point(0.8944271909999159, 0.4472135954999579), b.normal(1.0))
    }

    @Test
    fun testHasTheCorrectInflectionPoint() {
        // quadratic curves by definition have no inflections
        assertEquals(emptyList(), b.inflections().toList())
    }

    @Test
    fun testHasTheCorrectAxisAlignedBoundingBox() {
        assertEquals(
            Rectangle.fromBounds(0.0, 0.0, 1.0, 0.5),
            b.boundingBox
        )
    }

    @Test
    @Ignore
    fun testFromPointSet() {
        val M = Point(75, 25)
        val pts = listOf(Point(0, 0), M, Point(100, 100))

        run {
            val b = BezierCurve.quadraticFromPoints(pts[0], pts[1], pts[2]);
            assertEquals(M, b.get(0.5))
        }

        run {
            val t = 0.25;
            val b = BezierCurve.quadraticFromPoints(pts[0], pts[1], pts[2], t);
            val quarterpoint = b.get(t)
            assertEquals(M, quarterpoint)
        }
    }

    /*


  describe(`from point set`, () => {

     */
}
