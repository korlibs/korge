package com.soywiz.korma.geom.bezier

import com.soywiz.kds.*
import com.soywiz.korma.geom.Line
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.clone
import com.soywiz.korma.geom.mutable
import com.soywiz.korma.geom.pointArrayListOf
import com.soywiz.korma.math.*
import kotlin.test.*
import kotlin.test.assertEquals

class BezierCurveTest {
    @Test
    fun testBezierSimple() {
        val curve = Bezier(Point(0, 0), Point(-50, -200), Point(150, 150), Point(110, 120))
        assertEquals(
            Bezier.ProjectedPoint(p=Point(-6.66, -31.89), t=0.06, dSq=181.61),
            curve.project(Point(-20, -30)).roundDecimalPlaces(2)
        )
    }

    @Test
    fun testBezier() {
        //val curve = Bezier(PointArrayList(Point(0, 0), Point(100, 100), Point(150, 150), Point(250, 300)))
        val curve = Bezier(Point(0, 0), Point(-50, -200), Point(150, 150), Point(110, 120))
        //val curve = Bezier(PointArrayList(Point(0, 0), Point(100, 100), Point(250, 300)))
        assertEquals("[(0, 0), (-50, -200), (150, 150), (110, 120)]", curve.points.toString())
        assertEquals("[[(-150, -600), (600, 1050), (-120, -90)], [(1500, 3300), (-1440, -2280)], [(-2940, -5580)]]", curve.dpoints.toString())
        assertEquals(Point(-150, -600), curve.derivative(0.0))
        assertEquals(Point(232.5, 352.5), curve.derivative(0.5))
        assertEquals(Point(-120, -90), curve.derivative(1.0))

        assertEquals(Point(0.97, -0.24), curve.normal(0.0).mutable.setToRoundDecimalPlaces(2))
        assertEquals(Point(-0.83, 0.55), curve.normal(0.5).mutable.setToRoundDecimalPlaces(2))
        assertEquals(Point(0.6, -0.8), curve.normal(1.0))

        assertEquals(Point(0, 0), curve.compute(0.0))
        assertEquals(Point(51.25, -3.75), curve.compute(0.5))
        assertEquals(Point(110, 120), curve.compute(1.0))

        assertEquals(292.8273626504729, curve.length, 0.00001)
        assertEquals(
            listOf(listOf(0.11, 0.51, 0.91), listOf(0.22, 0.59, 0.96)),
            listOf(
                curve.extrema.xt.map { it.roundDecimalPlaces(2) },
                curve.extrema.yt.map { it.roundDecimalPlaces(2) }
            )
        )
        assertEquals(doubleArrayListOf(), curve.selfIntersections())

        assertEquals(101, curve.getLUT().size)
        assertEquals(
            Rectangle(x=-8.08, y=-62.06, width=123.41, height=183.90),
            curve.boundingBox.mutable.roundDecimalPlaces(2)
        )
        assertEquals(
            pointArrayListOf(Point(0, 0), Point(-50, -200), Point(150, 150), Point(110, 120), Point(-25, -100), Point(50, -25), Point(130, 135), Point(12.5, -62.5), Point(90, 55), Point(51.25, -3.75)),
            curve.hull(0.5)
        )
        assertEquals(
            Bezier.ProjectedPoint(p=Point(-6.66, -31.89), t=0.06, dSq=181.61),
            curve.project(Point(-20, -30)).roundDecimalPlaces(2)
        )
        assertEquals(
            "CurveSplit(base=Bezier([(0, 0), (-50, -200), (150, 150), (110, 120)]), left=SubBezier[0..0.5](Bezier([(0, 0), (-25, -100), (12.5, -62.5), (51.25, -3.75)])), right=SubBezier[0.5..1](Bezier([(51.25, -3.75), (90, 55), (130, 135), (110, 120)])), t=0.5, hull=[(0, 0), (-50, -200), (150, 150), (110, 120), (-25, -100), (50, -25), (130, 135), (12.5, -62.5), (90, 55), (51.25, -3.75)])",
            curve.split(0.5).roundDecimalPlaces(2).toString()
        )
        assertEquals(
            "SubBezier[0.25..0.75](Bezier([(1.72, -61.41), (23.91, -52.97), (77.97, 34.84), (102.66, 85.78)]))",
            curve.split(0.25, 0.75).roundDecimalPlaces(2).toString()
        )
        assertEquals(
            "[Bezier([(0, 0), (-5.62, -22.48), (-8.08, -38), (-8.08, -47.91)]), Bezier([(-8.08, -47.91), (-8.08, -55.51), (-6.63, -59.8), (-4.04, -61.37)]), Bezier([(-4.04, -61.37), (-3.27, -61.84), (-2.4, -62.06), (-1.43, -62.06)]), Bezier([(-1.43, -62.06), (9.29, -62.06), (31.46, -34.18), (53.62, -0.13)]), Bezier([(53.62, -0.13), (59.92, 9.55), (66.22, 19.72), (72.25, 29.89)]), Bezier([(72.25, 29.89), (95.78, 69.55), (115.33, 109.22), (115.33, 119.36)]), Bezier([(115.33, 119.36), (115.33, 120.54), (115.06, 121.32), (114.51, 121.65)]), Bezier([(114.51, 121.65), (114.31, 121.77), (114.06, 121.84), (113.78, 121.84)]), Bezier([(113.78, 121.84), (112.91, 121.84), (111.66, 121.25), (110, 120)])]",
            curve.reduce().map { it.curve.roundDecimalPlaces(2) }.toString()
        )
    }

    @Test
    fun testBezierBoundingBox() {
        assertEquals(
            Rectangle(x=-4.044654662829129, y=-62.06241698807055, width=2.6127315550921892, height=0.6955056507112474).clone().roundDecimalPlaces(2),
            Bezier(
                Point(-4.044654662829129, -61.366911337359305),
                Point(-3.2722813703417932, -61.83588230138613),
                Point(-2.398578099496581, -62.06241698807055),
                Point(-1.4319231077369396, -62.06241698807055),
            ).boundingBox.clone().roundDecimalPlaces(2)
        )

        assertEquals(
            Rectangle(65.0, 25.0, 37.2, 116.6),
            Bezier(100,25 , 10,180 , 170,165 , 65,70).boundingBox.clone().roundDecimalPlaces(1)
        )
    }

    @Test
    fun testSelfIntersections() {
        assertEquals(
            listOf(0.13914, 0.13961),
            Bezier(100,25 , 10,180 , 170,165 , 65,70).selfIntersections().toList()
        )
        assertEquals(
            listOf(),
            Bezier(Point(0, 0), Point(-50, -200), Point(150, 150), Point(110, 120)).selfIntersections().toList()
        )
    }

    @Test
    fun testInflections() {
        val curve = Bezier(100, 25, 10, 90, 110, 100, 150, 195)

        assertEquals(
            listOf(0.6300168840449997),
            curve.inflections().toList()
        )
    }

    @Test
    fun testLUT() {
        val curve = Bezier(100, 25, 10, 90, 110, 100, 150, 195)
        assertEquals(101, curve.lut.size)
        assertEquals(213.86206312975315, curve.length, 0.00001)

        println(curve.lut.estimatedLengths.toString())
        assertEquals(
            """
                Estimation(point=(100, 25), ratio=0.0, length=0.0)
                Estimation(point=(91.99, 30.99), ratio=0.03, length=10.0)
                Estimation(point=(81.66, 104.91), ratio=0.54, length=100.0)
                Estimation(point=(144.16, 182.44), ratio=0.95, length=200.0)
                Estimation(point=(150, 195), ratio=1.0, length=213.86)
            """.trimIndent(),
            //listOf(
            //    CurveLUT.Estimation(point=Point(100, 25), ratio=0.0, length=0.0),
            //    CurveLUT.Estimation(point=Point(91.9902598047208, 30.988058641418167), ratio=0.03139852512330205, length=10.0),
            //    CurveLUT.Estimation(point=Point(81.66306609423552, 104.91335975767849), ratio=0.5449255066963016, length=100.0),
            //    CurveLUT.Estimation(point=Point(144.15932033939768, 182.4364734733693), ratio=0.954162296103267, length=200.0),
            //    CurveLUT.Estimation(point=Point(150, 195), ratio=1.0, length=213.8574065019019),
            //),
            listOf(
                curve.lut.estimateAtLength(-10.0),
                curve.lut.estimateAtLength(10.0),
                curve.lut.estimateAtLength(100.0),
                curve.lut.estimateAtLength(200.0),
                curve.lut.estimateAtLength(10000.0),
            ).map { it.roundDecimalDigits(2) }.joinToString("\n")
        )
    }

    @Test
    fun testBoundingBox() {
        assertEquals(
            Rectangle(0.0, -37.5, 50.0, 37.5),
            Bezier(0,0, 0,-50, 50,-50, 50,0).boundingBox
        )
        assertEquals(
            Rectangle(0.0, -37.5, 50.0, 37.5),
            Bezier(0.0,0.0, 0.0,-50.0, 50.0,-50.0, 50.0,0.0).boundingBox
        )
    }
    @Test
    fun testCurvature() {
        val result = Bezier(0,0, 0,-50, 50,-50, 50,0).curvature(0.1)
        assertEquals(0.019829466587348795, result.k)
        assertEquals(50.43000000000001, result.r)
        assertEquals(0.00007738330725929297, result.dk)
        assertEquals(0.00007738330725929297, result.adk)
    }

    @Test
    fun testOffset() {
        val curves = Bezier(0,0, 0,-50, 50,-50, 50,0).offset(10.0)
        assertEquals(
            listOf(
                Bezier(10.0, 0.0, 10.0, -13.09, 22.67, -13.20, 5.03, 8.64),
                Bezier(5.03, 8.64, 29.40, -5.55, 28.05, 0.0, 0.0, 10.0),
                Bezier(0.0, 10.0, 17.71, 10.0, 6.56, 11.08, -8.60, 5.10),
                Bezier(-8.60, 5.10, -9.08, 4.30, 0.0, 0.29, -10.0, 0.0)
            ),
            curves.map { it.roundDecimalPlaces(2) }
        )
    }

    @Test
    fun testReduce() {
        val curves = Bezier(0,0, 0,-50, 50,-50, 50,0).toSimpleList()
        assertEquals(
            listOf(
                Bezier(
                    Point(0.0, 0.0),
                    Point(0.0, -18.25000000000001),
                    Point(6.661250000000008, -29.838750000000015),
                    Point(15.121037500000018, -34.766250000000014),
                ),
                Bezier(
                    Point(15.121037500000018, -34.766250000000014),
                    Point(18.250000000000014, -36.588750000000005),
                    Point(21.625000000000007, -37.5),
                    Point(25.0, -37.5),
                ),
                Bezier(
                    Point(25.0, -37.5 ),
                    Point(32.125, -37.5 ),
                    Point(39.25, -33.43875 ),
                    Point(44.0600875, -25.31624999999999)
                ),
                Bezier(
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
        val extrema = Bezier(330, 592, 330, 557, 315, 522, 315, 485).extrema

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
        val b = Bezier(0, 0, 0, 1, 1, 1, 1, 0)
        val lut = b.getLUT(100)
        assertEquals(101, lut.size)
    }

    @Test
    fun testLineCurveIntersection() {
        val b = Bezier(76, 250, 77, 150, 220, 50);
        val line = Line(13, 140, 213, 140)
        val intersections = b.intersections(line)
        assertEquals(listOf(0.55), intersections.toList())
    }

    @Test
    fun testProjectsOntoTheCorrectOnCurvePoint() {
        val b = Bezier(0, 0, 100, 0, 100, 100)
        assertEquals(
            Bezier.ProjectedPoint(p = Point(75, 25), t = 0.5, dSq = 50.0),
            b.project(Point(80, 20))
        )
    }

    @Test
    fun testToCubic() {
        // Line
        assertEquals(
            Bezier(0.0, 0.0, 33.33, 33.33, 66.67, 66.67, 100.0, 100.0),
            Bezier(0, 0, 100, 100).toCubic().roundDecimalPlaces(2)
        )

        // Quad
        assertEquals(
            Bezier(0.0, 0.0, 66.67, 0.0, 100.0, 33.33, 100.0, 100.0),
            Bezier(0, 0, 100, 0, 100, 100).toCubic().roundDecimalPlaces(2)
        )

        // Cubic
        assertEquals(
            Bezier(0, 0, 50, 60, 90, 30, 100, 100),
            Bezier(0, 0, 50, 60, 90, 30, 100, 100).toCubic().roundDecimalPlaces(2)
        )
    }

    @Test
    fun testToCubicToQuad() {
        // Quad
        assertEquals(
            Bezier(0, 0, 100, 80, 100, 100),
            Bezier(0, 0, 100, 80, 100, 100).toCubic().toQuad().roundDecimalPlaces(2)
        )
    }
}
