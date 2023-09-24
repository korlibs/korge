package korlibs.math.geom.bezier

import korlibs.image.vector.format.*
import korlibs.logger.*
import korlibs.math.geom.*
import korlibs.math.geom.shape.*
import korlibs.math.geom.vector.*
import kotlin.test.*

class CurvesToStrokeExTest {
    val logger = Logger("CurvesToStrokeExTest")
    val path = buildVectorPath {
        pathSvg("m262.15-119.2s2.05-8-2.35-3.6c0,0-6.4,5.2-13.2,5.2,0,0-13.2,2-17.2,14,0,0-3.6,24.4,3.6,29.6,0,0,4.4,6.8,10.8,0.8s20.35-33.6,18.35-46z")
    }.roundDecimalPlaces(2)
    val curvesList = path.toCurvesList()
    val curves = curvesList.first()

    @Test
    fun testStroke() {
        val stroke = path.toStrokePointsList(StrokeInfo(thickness = 10.0))
        logger.debug { stroke }
    }

    @Test
    fun testShape() {
        assertEquals(1, curvesList.size)
        assertEquals(true, curves.contiguous)
        assertEquals(true, curves.closed)
        assertEqualsFloat(
            listOf(
                Bezier(Point(262.15, -119.2), Point(262.15, -119.2), Point(264.2, -127.2), Point(259.8, -122.8)),
                Bezier(Point(259.8, -122.8), Point(259.8, -122.8), Point(253.4, -117.6), Point(246.6, -117.6)),
                Bezier(Point(246.6, -117.6), Point(246.6, -117.6), Point(233.4, -115.6), Point(229.4, -103.6)),
                Bezier(Point(229.4, -103.6), Point(229.4, -103.6), Point(225.8, -79.2), Point(233, -74)),
                Bezier(Point(233, -74), Point(233, -74), Point(237.4, -67.2), Point(243.8, -73.2)),
                Bezier(Point(243.8, -73.2), Point(250.2, -79.2), Point(264.15, -106.8), Point(262.15, -119.2)),
            ),
            curves.beziers
        )
    }

    fun pathPoints(join: LineJoin, block: VectorBuilder.() -> Unit): DoubleVectorArrayList =
        buildVectorPath { block() }.toStrokePointsList(StrokeInfo(thickness = 10.0, join = join), mode = StrokePointsMode.SCALABLE_POS_NORMAL_WIDTH).first().vector.clone().roundDecimalPlaces(2)

}
