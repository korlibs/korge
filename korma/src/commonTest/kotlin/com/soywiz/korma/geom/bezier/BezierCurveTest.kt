package com.soywiz.korma.geom.bezier

import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.clone
import kotlin.test.Test
import kotlin.test.assertEquals

class BezierCurveTest {
    @Test
    fun testBezierCurve() {
        //val curve = BezierCurve(PointArrayList(Point(0, 0), Point(100, 100), Point(150, 150), Point(250, 300)))
        val curve = BezierCurve(Point(0, 0), Point(-50, -200), Point(150, 150), Point(110, 120))
        //val curve = BezierCurve(PointArrayList(Point(0, 0), Point(100, 100), Point(250, 300)))
        println(curve.points)
        println(curve.dpoints)
        println(curve.derivative(0.0))
        println(curve.derivative(0.5))
        println(curve.derivative(1.0))
        println(curve.normal(0.0))
        println(curve.normal(0.5))
        println(curve.normal(1.0))
        println(curve.compute(0.0))
        println(curve.compute(0.5))
        println(curve.compute(1.0))
        println(curve.getLUT())
        println(curve.extrema)
        println(curve.boundingBox)
        println(curve.length)
        println(curve.hull(0.5))
        println(curve.split(0.5))
        println(curve.split(0.25, 0.75))
        println(curve.project(Point(-20, -30)))
        println(curve.reduce())
        println(curve.selfIntersections())
    }

    @Test
    fun testBezierCurveBoundingBox() {
        assertEquals(
            Rectangle(x=-4.044654662829129, y=-62.06241698807055, width=2.6127315550921892, height=0.6955056507112474).clone().roundDecimalPlaces(2),
            BezierCurve(
                Point(-4.044654662829129, -61.366911337359305),
                Point(-3.2722813703417932, -61.83588230138613),
                Point(-2.398578099496581, -62.06241698807055),
                Point(-1.4319231077369396, -62.06241698807055),
            ).boundingBox.clone().roundDecimalPlaces(2)
        )

        assertEquals(
            Rectangle(65.0, 25.0, 37.2, 116.6),
            BezierCurve(100,25 , 10,180 , 170,165 , 65,70).boundingBox.clone().roundDecimalPlaces(1)
        )
    }

    @Test
    fun testSelfIntersections() {
        assertEquals(
            listOf(0.13914, 0.13961),
            BezierCurve(100,25 , 10,180 , 170,165 , 65,70).selfIntersections().toList()
        )
        assertEquals(
            listOf(),
            BezierCurve(Point(0, 0), Point(-50, -200), Point(150, 150), Point(110, 120)).selfIntersections().toList()
        )
    }

    @Test
    fun testInflections() {
        val curve = BezierCurve(100, 25, 10, 90, 110, 100, 150, 195)

        assertEquals(
            listOf(0.6300168840449997),
            curve.inflections().toList()
        )
        println(curve.lut)
        println(curve.length)
        //println(curve.lut.estimateAtLength(10.0))
        println(curve.lut.estimateAtLength(-10.0))
        println(curve.lut.estimateAtLength(10.0))
        println(curve.lut.estimateAtLength(100.0))
        println(curve.lut.estimateAtLength(200.0))
        println(curve.lut.estimateAtLength(10000.0))
    }

    @Test
    fun testBoundingBox() {
        //println(BezierCurve(0,0, 0,-50, 50,-50, 50,0).extrema)
        assertEquals(
            Rectangle(0.0, -37.5, 50.0, 37.5),
            BezierCurve(0,0, 0,-50, 50,-50, 50,0).boundingBox
        )
        assertEquals(
            Rectangle(0.0, -37.5, 50.0, 37.5),
            Bezier.cubicBounds(0.0,0.0, 0.0,-50.0, 50.0,-50.0, 50.0,0.0)
        )
    }
}
