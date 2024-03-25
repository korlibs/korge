package korlibs.math.geom.bezier

import korlibs.math.geom.*
import korlibs.math.interpolation.*
import kotlin.test.*

class BezierCurveQuadraticTest {
    val b = Bezier(Point(0.0, 0.0), Point(0.5, 1.0), Point(1.0, 0.0))
    @Test
    fun testSerializesCorrectly() {
        assertEquals("Bezier([(0, 0), (0.5, 1), (1, 0)])", b.toString())
    }

    @Test
    fun testHasTheCorrectApproximateLength() {
        assertEquals(1.4789428575453212, b.length, 0.001)
    }

    @Test
    fun testHasTheExpectedDerivatePoints() {
        assertEquals(
            listOf(
                pointArrayListOf(Point(1, 2), Point(1, -2)),
                pointArrayListOf(Point(0, -4)),
            ),
            b.dpoints
        )
        assertEquals(Point(1, 2), b.derivative(Ratio.ZERO))
        assertEquals(Point(1, 0), b.derivative(Ratio.HALF))
        assertEquals(Point(1, -2), b.derivative(Ratio.ONE))
    }

    @Test
    fun testHasTheExpectedNormals() {
        assertEqualsFloat(Point(-0.8944271909999159, 0.4472135954999579), b.normal(Ratio.ZERO))
        assertEqualsFloat(Point(-0.0, 1.0), b.normal(Ratio.HALF))
        assertEqualsFloat(Point(0.8944271909999159, 0.4472135954999579), b.normal(Ratio.ONE))
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
    fun testFromPointSet() {
        val M = Point(75, 25)
        val pts = listOf(Point(0, 0), M, Point(100, 100))

        run {
            val b = Bezier.quadraticFromPoints(pts[0], pts[1], pts[2])
            assertEqualsFloat(Bezier(Point(0, 0), Point(100, 0), Point(100, 100)), b)
            assertEqualsFloat(M, b[Ratio.HALF])
        }

        run {
            val t = 0.25.toRatio()
            val b = Bezier.quadraticFromPoints(pts[0], pts[1], pts[2], t)
            assertEqualsFloat(Bezier(Point(0.0, 0.0), Point(183.33, 50.0), Point(100.0, 100.0)), b.roundDecimalPlaces(2))
            assertEqualsFloat(M, b[t])
        }
    }

    /*


  describe(`from point set`, () => {

     */
}
