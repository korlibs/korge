package korlibs.math.geom.bezier

import korlibs.math.*
import korlibs.math.geom.*
import korlibs.math.geom.shape.*
import korlibs.math.geom.vector.*
import korlibs.math.interpolation.*
import kotlin.math.*
import kotlin.test.*

class BezierTest {
    @Test
    fun testLength() {
        assertEquals(100.0, Bezier(Point(0, 0), Point(50, 0), Point(100, 0)).length, 0.001)
        val bezier = Bezier(Point(0, 0), Point(50, 0), Point(100, 0))
        assertEquals(100.0, bezier.length, 0.001)
        val bezier2 = Bezier(Point(0, 0), Point(100, 0), Point(100, 100))
        assertEquals(162.32, bezier2.length.roundDecimalPlaces(2), 0.001)
    }

    @Test
    fun testCurves() {
        val path = buildVectorPath {
            moveTo(Point(200.0, 200.0))
            lineTo(Point(400.0, 100.0))
            quadTo(Point(400.0, 400.0), Point(200.0, 200.0))
        }
        val curves = path.getCurves()
        assertEquals(590.0, curves.length, 0.4)
        assertEqualsFloat(Rectangle(200, 100, 200, 180), curves.getBounds(), 0.1)
        assertEquals(
            """
                (200, 200)
                (228, 186)
                (256, 172)
                (283, 158)
                (311, 144)
                (339, 131)
                (367, 117)
                (394, 103)
                (399, 138)
                (395, 180)
                (389, 214)
                (379, 241)
                (367, 261)
                (352, 274)
                (334, 280)
                (313, 278)
                (289, 269)
                (262, 253)
                (232, 230)
                (200, 200)
            """.trimIndent(),
            curves.getPoints(20).map { x, y -> "(${x.roundToInt()}, ${y.roundToInt()})" }.joinToString("\n")
        )
    }

    @Test
    fun testCurveList() {
        val path = buildVectorPath {
            rect(0, 0, 100, 100)
            rect(300, 0, 100, 100)
        }
        val curves = path.getCurvesList()
        assertEquals(2, curves.size)
        assertEquals(Rectangle(0, 0, 100, 100), curves[0].getBounds())
        assertEquals(Rectangle(300, 0, 100, 100), curves[1].getBounds())
        assertEquals(Rectangle(0, 0, 400, 100), curves.toCurves().getBounds())
    }

    @Test
    fun testTangent() {
        val bezier = Bezier(Point(74.58, 36.96), Point(74.58, 36.96), Point(77.04, 27.36), Point(71.76, 32.64))
        assertEquals(Point(0.2482, -0.9687), bezier.tangent(Ratio.ZERO).roundDecimalPlaces(4))
    }
}
