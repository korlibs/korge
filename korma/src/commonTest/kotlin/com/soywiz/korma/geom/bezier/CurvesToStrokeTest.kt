package com.soywiz.korma.geom.bezier

import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.vector.*
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
            }.toString()
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
            }.toString()
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
        assertEquals(Curves(Bezier(100,0, 150,0)), curves.split(0.5, 0.75))
        assertEquals(Curves(Bezier(50,0, 100,0)), curves.split(0.25, 0.5))
        assertEquals(Curves(Bezier(50,0, 100,0), Bezier(100,0, 150,0)), curves.split(0.25, 0.75))
    }

    @Test
    fun testCircleJoins() {
        val curves = Arc.createCircle(Point(0, 0), 100f)
        val builder = StrokePointsBuilder(10.0, mode = StrokePointsMode.SCALABLE_POS_NORMAL_WIDTH)
        builder.addJoin(curves.beziers[0], curves.beziers[1], LineJoin.MITER, 5.0)
        assertEquals(
            """
                VectorArrayList[2](
                   [0, -100, 0, -1, 10, 10], 
                   [0, -100, 0, -1, -10, 10]
                )
            """.trimIndent(),
            builder.vector.roundDecimalPlaces(2).toString()
        )
    }

    fun pathPoints(join: LineJoin, block: VectorBuilder.() -> Unit): VectorArrayList =
        buildVectorPath { block() }
            .toStrokePointsList(StrokeInfo(thickness = 10.0, join = join), mode = StrokePointsMode.SCALABLE_POS_NORMAL_WIDTH)
            .first().vector.clone()
            .roundDecimalPlaces(2)
}
