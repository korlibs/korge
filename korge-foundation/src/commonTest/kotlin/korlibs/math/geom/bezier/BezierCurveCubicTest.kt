package korlibs.math.geom.bezier

import korlibs.math.geom.*
import kotlin.test.*

class BezierCurveCubicTest {
    val b = Bezier(Point(0, 0), Point(0, 1), Point(1, 1), Point(1, 0))

    @Test
    fun testSerializesCorrectly() {
        assertEquals("Bezier([(0, 0), (0, 1), (1, 1), (1, 0)])", b.toString())
    }

    @Test
    fun testHastheCorrectApproximateLength() {
        assertEquals(2.0f, b.length, 0.0001f)
    }

    @Test
    fun testHasTheExpectedDerivativePoints() {
        assertEquals(
            listOf(
                pointArrayListOf(Point(0, 3), Point(3, 0), Point(0, -3)),
                pointArrayListOf(Point(6, -6), Point(-6, -6)),
                pointArrayListOf(Point(-12, 0))
            ),
            b.dpoints
        )

        assertEquals(Point(0, 3), b.derivative(0.0f))
        assertEquals(Point(1.5, 0.0), b.derivative(0.5f))
        assertEquals(Point(0, -3), b.derivative(1.0f))
    }

    @Test
    fun testHasTheExpectedNormals() {
        assertEquals(Point(-1, 0), b.normal(0.0f))
        assertEquals(Point(-0.0, 1.0), b.normal(0.5f))
        assertEquals(Point(1.0, 0.0), b.normal(1.0f))
    }

    @Test
    fun testHasTheCorrectInflectionPoint() {
        assertEquals(emptyList(), b.inflections().toList())
    }

    @Test
    fun testHasTheCorrectAxisAlignedBoundingBox() {
        assertEquals(
            Rectangle.fromBounds(0.0, 0.0, 1.0, 0.75),
            b.boundingBox
        )
    }

    @Test
    fun testComplexCubicHasTheCorrectInflectionPoint() {
        val b = Bezier(Point(0.0, 0.0), Point(1.0, 0.25), Point(0.0, 1.0), Point(1.0, 0.0))
        assertEqualsFloat(listOf(0.8f, 0.5f), b.inflections().toList(), 0.01)
    }

    @Test
    fun testFromPointSet() {
        val M = Point(200 / 3.0, 100 / 3.0)
        val pts = listOf(Point(0, 0), M, Point(100, 100))
        run {
            val b: Bezier = Bezier.cubicFromPoints(pts[0], pts[1], pts[2])
            assertEqualsFloat(
                Bezier(Point(0.0, 0.0), Point(55.56, 11.11), Point(88.89, 44.44), Point(100.0, 100.0)),
                b.roundDecimalPlaces(2)
            )
            val midpoint = b[0.5f]
            assertEquals(midpoint.xD, M.xD, 0.001)
            assertEquals(midpoint.yD, M.yD, 0.001)
        }
        run {
            val t = 0.25f
            val b = Bezier.cubicFromPoints(pts[0], pts[1], pts[2], t)
            val quarterpoint = b[t]
            assertEquals(quarterpoint.xD, M.xD, 0.001)
            assertEquals(quarterpoint.yD, M.yD, 0.001)
        }
    }
}
