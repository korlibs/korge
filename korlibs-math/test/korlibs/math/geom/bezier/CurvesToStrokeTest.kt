package korlibs.math.geom.bezier

import korlibs.math.geom.*
import korlibs.math.geom.shape.*
import korlibs.math.geom.vector.*
import korlibs.math.interpolation.*
import kotlin.test.*

class CurvesToStrokeTest {
    @Test
    fun testMiterClosedRect() {
        assertEquals(
            """
                VectorArrayList[10](
                   [0, 0, 0.71, 0.71, 7.07, 7.07], 
                   [0, 0, 0.71, 0.71, -7.07, 7.07], 
                   [100, 0, -0.71, 0.71, 7.07, 7.07], 
                   [100, 0, -0.71, 0.71, -7.07, 7.07], 
                   [100, 100, -0.71, -0.71, 7.07, 7.07], 
                   [100, 100, -0.71, -0.71, -7.07, 7.07], 
                   [0, 100, 0.71, -0.71, 7.07, 7.07], 
                   [0, 100, 0.71, -0.71, -7.07, 7.07], 
                   [0, 0, 0.71, 0.71, 7.07, 7.07], 
                   [0, 0, 0.71, 0.71, -7.07, 7.07]
                )
            """.trimIndent(),
            pathPoints(LineJoin.MITER) {
                rect(0, 0, 100, 100)
            }.toString(roundDecimalPlaces = 2)
        )
    }

    @Test
    fun testBevelAngleCW() {
        assertEquals(
            """
                VectorArrayList[8](
                   [0, 0, 0, 1, 5, 5], 
                   [0, 0, 0, 1, -5, 5], 
                   [100, 0, -0.71, 0.71, 7.07, 7.07], 
                   [100, 0, 0, 1, -5, 5], 
                   [100, 0, -0.71, 0.71, 7.07, 7.07], 
                   [100, 0, -1, 0, -5, 5], 
                   [100, 100, -1, 0, 5, 5], 
                   [100, 100, -1, 0, -5, 5]
                )
            """.trimIndent(),
            pathPoints(LineJoin.BEVEL) {
                moveTo(Point(0, 0))
                lineTo(Point(100, 0))
                lineTo(Point(100, 100))
            }.toString(roundDecimalPlaces = 2)
        )
    }

    @Test
    fun testClosed() {
        val path = buildVectorPath {
            star(6, 10.0, 20.0)
        }
        assertEquals(true, path.getCurves().closed)
    }

    @Test
    fun testSplit() {
        val curves = buildVectorPath {
            moveTo(Point(0, 0))
            lineTo(Point(100, 0))
            lineTo(Point(200, 0))
        }.getCurves()
        assertEquals(Curves(Bezier(Point(100,0), Point(150,0))), curves.split(0.5f.toRatio(), 0.75f.toRatio()))
        assertEquals(Curves(Bezier(Point(50,0), Point(100,0))), curves.split(0.25f.toRatio(), 0.5f.toRatio()))
        assertEquals(Curves(Bezier(Point(50,0), Point(100,0)), Bezier(Point(100,0), Point(150,0))), curves.split(0.25f.toRatio(), 0.75f.toRatio()))
    }

    @Test
    fun testCircleJoins() {
        val curves = Arc.createCircle(Point(0, 0), 100.0)
        val builder = StrokePointsBuilder(10.0, mode = StrokePointsMode.SCALABLE_POS_NORMAL_WIDTH)
        builder.addJoin(curves.beziers[0], curves.beziers[1], LineJoin.MITER, 5.0)
        assertEquals(
            """
                VectorArrayList[2](
                   [0, -100, 0, -1, 10, 10], 
                   [0, -100, 0, -1, -10, 10]
                )
            """.trimIndent(),
            builder.vector.toString(roundDecimalPlaces = 2)
        )
    }

    fun pathPoints(join: LineJoin, block: VectorBuilder.() -> Unit): DoubleVectorArrayList =
        buildVectorPath { block() }
            .toStrokePointsList(StrokeInfo(thickness = 10f, join = join), mode = StrokePointsMode.SCALABLE_POS_NORMAL_WIDTH)
            .first().vector.clone()
}
