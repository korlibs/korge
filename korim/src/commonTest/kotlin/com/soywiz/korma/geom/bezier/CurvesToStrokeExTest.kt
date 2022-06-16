package com.soywiz.korma.geom.bezier

import com.soywiz.korim.vector.format.pathSvg
import com.soywiz.korma.geom.VectorArrayList
import com.soywiz.korma.geom.shape.buildVectorPath
import com.soywiz.korma.geom.vector.LineJoin
import com.soywiz.korma.geom.vector.StrokeInfo
import com.soywiz.korma.geom.vector.VectorBuilder
import com.soywiz.korma.geom.vector.toCurvesList
import kotlin.test.Test
import kotlin.test.assertEquals

class CurvesToStrokeExTest {
    val path = buildVectorPath {
        pathSvg("m262.15-119.2s2.05-8-2.35-3.6c0,0-6.4,5.2-13.2,5.2,0,0-13.2,2-17.2,14,0,0-3.6,24.4,3.6,29.6,0,0,4.4,6.8,10.8,0.8s20.35-33.6,18.35-46z")
    }
    val curvesList = path.toCurvesList()
    val curves = curvesList.first()

    @Test
    fun testStroke() {
        val stroke = path.toStrokePointsList(StrokeInfo(thickness = 10.0))
        println(stroke)
    }

    @Test
    fun testShape() {
        assertEquals(1, curvesList.size)
        assertEquals(6, curves.beziers.size)
        assertEquals(true, curves.contiguous)
        assertEquals(true, curves.closed)
        assertEquals(
            """
                Bezier([(262.15, -119.2), (262.15, -119.2), (264.2, -127.2), (259.8, -122.8)])
                Bezier([(259.8, -122.8), (259.8, -122.8), (253.4, -117.6), (246.6, -117.6)])
                Bezier([(246.6, -117.6), (246.6, -117.6), (233.4, -115.6), (229.4, -103.6)])
                Bezier([(229.4, -103.6), (229.4, -103.6), (225.8, -79.2), (233, -74)])
                Bezier([(233, -74), (233, -74), (237.4, -67.2), (243.8, -73.2)])
                Bezier([(243.8, -73.2), (250.2, -79.2), (264.15, -106.8), (262.15, -119.2)])
            """.trimIndent(),
            curves.roundDecimalPlaces(2).beziers.joinToString("\n")
        )
    }

    fun pathPoints(join: LineJoin, block: VectorBuilder.() -> Unit): VectorArrayList =
        buildVectorPath { block() }.toStrokePointsList(StrokeInfo(thickness = 10.0, join = join), mode = StrokePointsMode.SCALABLE_POS_NORMAL_WIDTH).first().vector.clone().roundDecimalPlaces(2)

}
