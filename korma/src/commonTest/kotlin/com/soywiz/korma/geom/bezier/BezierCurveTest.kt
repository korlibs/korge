package com.soywiz.korma.geom.bezier

import com.soywiz.korma.geom.Line
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.clone
import com.soywiz.korma.geom.roundDecimalPlaces
import com.soywiz.korma.math.roundDecimalPlaces
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
    @Test
    fun testCurvature() {
        val result = BezierCurve(0,0, 0,-50, 50,-50, 50,0).curvature(0.1)
        assertEquals(0.019829466587348795, result.k)
        assertEquals(50.43000000000001, result.r)
        assertEquals(0.00007738330725929297, result.dk)
        assertEquals(0.00007738330725929297, result.adk)
    }

    @Test
    fun testOffset() {
        val curves = BezierCurve(0,0, 0,-50, 50,-50, 50,0).offset(10.0)
        assertEquals(
            listOf(
                BezierCurve(10.0, 0.0, 10.0, -13.09, 22.67, -13.20, 5.03, 8.64),
                BezierCurve(5.03, 8.64, 29.40, -5.55, 28.05, 0.0, 0.0, 10.0),
                BezierCurve(0.0, 10.0, 17.71, 10.0, 6.56, 11.08, -8.60, 5.10),
                BezierCurve(-8.60, 5.10, -9.08, 4.30, 0.0, 0.29, -10.0, 0.0)
            ),
            curves.map { it.roundDecimalPlaces(2) }
        )
    }

    @Test
    fun testReduce() {
        val curves = BezierCurve(0,0, 0,-50, 50,-50, 50,0).toSimpleList()
        assertEquals(
            listOf(
                BezierCurve(
                    Point(0.0, 0.0),
                    Point(0.0, -18.25000000000001),
                    Point(6.661250000000008, -29.838750000000015),
                    Point(15.121037500000018, -34.766250000000014),
                ),
                BezierCurve(
                    Point(15.121037500000018, -34.766250000000014),
                    Point(18.250000000000014, -36.588750000000005),
                    Point(21.625000000000007, -37.5),
                    Point(25.0, -37.5),
                ),
                BezierCurve(
                    Point(25.0, -37.5 ),
                    Point(32.125, -37.5 ),
                    Point(39.25, -33.43875 ),
                    Point(44.0600875, -25.31624999999999)
                ),
                BezierCurve(
                    Point(44.0600875, -25.31624999999999),
                    Point(47.68875, -19.188749999999988),
                    Point(50.0, -10.749999999999993),
                    Point(50, 0)
                )
            ),
            curves.map { it.curve }
        )
    }

    @Test
    fun testExtremaHasCorrectExtrema() {
        val extrema = BezierCurve(330, 592, 330, 557, 315, 522, 315, 485).extrema

        assertEquals(
            listOf(
                listOf(0.0, 0.5, 1.0),
                listOf(0.0, 0.5, 1.0),
                listOf(0.0),
            ),
            listOf(extrema.allt.toList(), extrema.xt.toList(), extrema.yt.toList())
        )
    }

    @Test
    fun testLutYieldsNP1Points() {
        val b = BezierCurve(0, 0, 0, 1, 1, 1, 1, 0)
        val lut = b.getLUT(100)
        assertEquals(101, lut.size)
    }

    @Test
    fun testLineCurveIntersection() {
        val b = BezierCurve(76, 250, 77, 150, 220, 50);
        val line = Line(13, 140, 213, 140)
        val intersections = b.intersections(line)
        assertEquals(listOf(0.55), intersections.toList())
    }

    @Test
    fun testProjectsOntoTheCorrectOnCurvePoint() {
        val b = BezierCurve(0, 0, 100, 0, 100, 100)
        assertEquals(
            BezierCurve.ProjectedPoint(p = Point(75, 25), t = 0.5, d = 7.0710678118654755),
            b.project(Point(80, 20))
        )
    }
}
