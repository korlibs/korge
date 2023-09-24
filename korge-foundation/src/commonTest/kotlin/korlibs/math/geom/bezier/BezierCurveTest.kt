package korlibs.math.geom.bezier

import korlibs.datastructure.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*
import korlibs.platform.*
import kotlin.test.*

class BezierCurveTest {
    @Test
    fun testBezierSimple() {
        val curve = Bezier(Point(0, 0), Point(-50, -200), Point(150, 150), Point(110, 120))
        assertEqualsFloat(
            Bezier.ProjectedPoint(p=Point(-6.66, -31.89), t=.06f.toRatio(), dSq=181.61),
            curve.project(Point(-20, -30)).roundDecimalPlaces(2)
        )
    }

    @Test
    fun testBezier() {
        //assertEquals("a", "b")
        //val curve = Bezier(PointArrayList(Point(0, 0), Point(100, 100), Point(150, 150), Point(250, 300)))
        val curve = Bezier(Point(0, 0), Point(-50, -200), Point(150, 150), Point(110, 120))
        //val curve = Bezier(PointArrayList(Point(0, 0), Point(100, 100), Point(250, 300)))
        assertEqualsFloat("[(0, 0), (-50, -200), (150, 150), (110, 120)]", curve.points.toString())
        assertEqualsFloat("[[(-150, -600), (600, 1050), (-120, -90)], [(1500, 3300), (-1440, -2280)], [(-2940, -5580)]]", curve.dpoints.toString())
        assertEqualsFloat(Point(-150, -600), curve.derivative(0.0f.toRatio()))
        assertEqualsFloat(Point(232.5, 352.5), curve.derivative(0.5f.toRatio()))
        assertEqualsFloat(Point(-120, -90), curve.derivative(1.0f.toRatio()))

        assertEqualsFloat(Point(0.97, -0.24), curve.normal(0.0f.toRatio()).roundDecimalPlaces(2))
        assertEqualsFloat(Point(-0.83, 0.55), curve.normal(0.5f.toRatio()).roundDecimalPlaces(2))
        assertEqualsFloat(Point(0.6, -0.8), curve.normal(1.0f.toRatio()))

        assertEqualsFloat(Point(0, 0), curve.compute(0.0f.toRatio()))
        assertEqualsFloat(Point(51.25, -3.75), curve.compute(0.5f.toRatio()))
        assertEqualsFloat(Point(110, 120), curve.compute(1.0f.toRatio()))

        assertEqualsFloat(292.8273626504729f, curve.length, 0.1)
        assertEqualsFloat(
            listOf(listOf(0.11f, 0.51f, 0.91f), listOf(0.22f, 0.59f, 0.96f)),
            listOf(
                curve.extrema.xt.map { it.roundDecimalPlaces(2) },
                curve.extrema.yt.map { it.roundDecimalPlaces(2) }
            ),
            0.01
        )
        assertEqualsFloat(doubleArrayListOf(), curve.selfIntersections())

        assertEqualsFloat(101, curve.getLUT().size)
        assertEqualsFloat(
            Rectangle(x=-8.08, y=-62.06, width=123.41, height=183.90),
            curve.boundingBox.roundDecimalPlaces(2)
        )
        assertEqualsFloat(
            pointArrayListOf(Point(0, 0), Point(-50, -200), Point(150, 150), Point(110, 120), Point(-25, -100), Point(50, -25), Point(130, 135), Point(12.5, -62.5), Point(90, 55), Point(51.25, -3.75)),
            curve.hull(0.5f.toRatio())
        )
        assertEqualsFloat(
            Bezier.ProjectedPoint(p=Point(-6.66, -31.89), t=0.06f.toRatio(), dSq=181.61),
            curve.project(Point(-20, -30)).roundDecimalPlaces(2)
        )
        assertEqualsFloat(
            "CurveSplit(base=Bezier([(0, 0), (-50, -200), (150, 150), (110, 120)]), left=SubBezier[0..0.5](Bezier([(0, 0), (-25, -100), (12.5, -62.5), (51.25, -3.75)])), right=SubBezier[0.5..1](Bezier([(51.25, -3.75), (90, 55), (130, 135), (110, 120)])), t=0.5, hull=[(0, 0), (-50, -200), (150, 150), (110, 120), (-25, -100), (50, -25), (130, 135), (12.5, -62.5), (90, 55), (51.25, -3.75)])",
            curve.split(0.5f.toRatio()).roundDecimalPlaces(2).toString()
        )

        assertEqualsFloat(
            Bezier(Point(1.72, -61.41), Point(23.91, -52.97), Point(77.97, 34.84), Point(102.66, 85.78)),
            curve.split(0.25f.toRatio(), 0.75f.toRatio()).curve,
            absoluteTolerance = 0.1
        )
        assertEqualsFloat(
            listOf(
                Bezier(Point(0, 0), Point(-5.6, -22.5), Point(-8.0, -38.0), Point(-8.0, -48.0)),
                Bezier(Point(-8.0, -47.9), Point(-8.0, -55.5), Point(-6.6, -59.8), Point(-4.0, -61.4)),
                Bezier(Point(-4.0, -61.3), Point(-3.3, -61.8), Point(-2.4, -62.0), Point(-1.4, -62.0)),
                Bezier(Point(-1.4, -62.0), Point(9.3, -62.0), Point(31.5, -34.2), Point(53.6, -0.13)),
                Bezier(Point(53.6, -0.1), Point(59.9, 9.5), Point(66.2, 19.7), Point(72.25, 29.9)),
                Bezier(Point(72.25, 29.9), Point(95.8, 69.5), Point(115.3, 109.2), Point(115.3, 119.3)),
                Bezier(Point(115.3, 119.3), Point(115.3, 120.5), Point(115.0, 121.3), Point(114.51, 121.6)),
                Bezier(Point(114.5, 121.6), Point(114.3, 121.7), Point(114.0, 121.8), Point(113.78, 121.84)),
                Bezier(Point(113.8, 121.8), Point(112.9, 121.8), Point(111.6, 121.25), Point(110, 120)),
            ),
            curve.reduce().map { it.curve },
            absoluteTolerance = 0.1
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
            Bezier(Point(100, 25), Point(10, 180), Point(170, 165), Point(65, 70)).boundingBox.clone().roundDecimalPlaces(1)
        )
    }

    @Test
    fun testSelfIntersections() {
        assertEqualsFloat(
            listOf(0.13914f, 0.13961f),
            Bezier(Point(100, 25), Point(10, 180), Point(170, 165), Point(65, 70)).selfIntersections().toList(),
            0.01
        )
        assertEqualsFloat(
            listOf(),
            Bezier(Point(0, 0), Point(-50, -200), Point(150, 150), Point(110, 120)).selfIntersections().toList(),
            0.01
        )
    }

    @Test
    fun testInflections() {
        val curve = Bezier(Point(100, 25), Point(10, 90), Point(110, 100), Point(150, 195))

        assertEqualsFloat(
            listOf(0.6300168840449997f),
            curve.inflections().toList(),
            absoluteTolerance = 0.01
        )
    }

    @Test
    fun testLUT() {
        if (Platform.isWasm) {
            println("!! WASM: SKIPPING FOR NOW BECAUSE toString differs!")
            return
        }

        val curve = Bezier(Point(100, 25), Point(10, 90), Point(110, 100), Point(150, 195))
        assertEquals(101, curve.lut.size)
        assertEquals(213.86206312975315, curve.length, 0.001)

        //println(curve.lut.estimatedLengths.toString())
        assertEquals(
            """
                Estimation(point=(100, 25), ratio=0, length=0)
                Estimation(point=(91.99, 30.99), ratio=0.03, length=10)
                Estimation(point=(81.66, 104.91), ratio=0.54, length=100)
                Estimation(point=(144.16, 182.44), ratio=0.95, length=200)
                Estimation(point=(150, 195), ratio=1, length=213.86)
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
            Bezier(Point(0,0), Point(0,-50), Point(50,-50), Point(50,0)).boundingBox
        )
        assertEquals(
            Rectangle(0.0, -37.5, 50.0, 37.5),
            Bezier(Point(0,0), Point(0,-50), Point(50,-50), Point(50,0)).boundingBox
        )
    }
    @Test
    fun testCurvature() {
        val result = Bezier(Point(0,0), Point(0,-50), Point(50,-50), Point(50,0)).curvature(0.1.toRatio())
        assertEquals(0.019829466587348795, result.k, 0.001)
        assertEquals(50.43000000000001, result.r, 0.001)
        assertEquals(0.00007738330725929297, result.dk, 0.001)
        assertEquals(0.00007738330725929297, result.adk, 0.001)
    }

    @Test
    fun testOffset() {
        val curves = Bezier(Point(0, 0), Point(0, -50), Point(50, -50), Point(50, 0)).offset(10.0)
        assertEquals(
            listOf(
                Bezier(Point(10.0, 0.0), Point(10.0, -13.09), Point(22.67, -13.20), Point(5.03, 8.64)),
                Bezier(Point(5.03, 8.64), Point(29.40, -5.55), Point(28.05, 0.0), Point(0.0, 10.0)),
                Bezier(Point(0.0, 10.0), Point(17.71, 10.0), Point(6.56, 11.08), Point(-8.60, 5.10)),
                Bezier(Point(-8.60, 5.10), Point(-9.08, 4.30), Point(0.0, 0.29), Point(-10.0, 0.0))
            ),
            curves.map { it.roundDecimalPlaces(2) }
        )
    }

    @Test
    fun testReduce() {
        val curves = Bezier(Point(0,0), Point(0,-50), Point(50,-50), Point(50,0)).toSimpleList()

        assertEqualsFloat(
            listOf(
                listOf(Point(0, 0), Point(0.0, -18.25), Point(6.66125, -29.838749), Point(15.1210375, -34.76625)),
                listOf(Point(15.1210375, -34.76625), Point(18.25, -36.58875), Point(21.625, -37.5), Point(25.0, -37.5)),
                listOf(Point(25.0, -37.5), Point(32.125, -37.5), Point(39.25, -33.43875), Point(44.06009, -25.316252)),
                listOf(Point(44.06009, -25.316252), Point(47.68875, -19.188751), Point(50.0, -10.75), Point(50, 0)),
            ),
            curves.map { it.curve.points.toList().map { it } }
        )
    }

    @Test
    fun testExtremaHasCorrectExtrema() {
        val extrema = Bezier(Point(330, 592), Point(330, 557), Point(315, 522), Point(315, 485)).extrema

        assertEquals(
            listOf(
                listOf(0.0, .5, 1.0),
                listOf(0.0, .5, 1.0),
                listOf(0.0),
            ),
            listOf(extrema.allt.toList(), extrema.xt.toList(), extrema.yt.toList())
        )
    }

    @Test
    fun testLutYieldsNP1Points() {
        val b = Bezier(Point(0, 0), Point(0, 1), Point(1, 1), Point(1, 0))
        val lut = b.getLUT(100)
        assertEquals(101, lut.size)
    }

    @Test
    fun testLineCurveIntersection() {
        val b = Bezier(Point(76, 250), Point(77, 150), Point(220, 50))
        val line = Line(13, 140, 213, 140)
        val intersections = b.intersections(line)
        assertEqualsFloat(listOf(0.55f), intersections.toList(), 0.01)
    }

    @Test
    fun testProjectsOntoTheCorrectOnCurvePoint() {
        val b = Bezier(Point(0, 0), Point(100, 0), Point(100, 100))
        assertEqualsFloat(
            Bezier.ProjectedPoint(p = Point(75, 25), t = .5f.toRatio(), dSq = 50.0),
            b.project(Point(80, 20)),
            0.1
        )
    }

    @Test
    fun testToCubicLine() {
        // Line
        assertEquals(
            Bezier(Point(0.0, 0.0), Point(33.33, 33.33), Point(66.67, 66.67), Point(100.0, 100.0)),
            Bezier(Point(0, 0), Point(100, 100)).toCubic().roundDecimalPlaces(2)
        )

    }

    @Test
    fun testToCubicQuad() {
        // Quad
        assertEquals(
            Bezier(Point(0, 0), Point(66.67, 0), Point(100, 33.33), Point(100, 100)),
            Bezier(Point(0, 0), Point(100, 0), Point(100, 100)).toCubic().roundDecimalPlaces(2)
        )
    }

    @Test
    fun testToCubicCubic() {
        // Cubic
        assertEquals(
            Bezier(Point(0, 0), Point(50, 60), Point(90, 30), Point(100, 100)),
            Bezier(Point(0, 0), Point(50, 60), Point(90, 30), Point(100, 100)).toCubic().roundDecimalPlaces(2)
        )
    }

    @Test
    fun testToCubicToQuad() {
        // Quad
        assertEquals(
            Bezier(Point(0, 0), Point(100, 80), Point(100, 100)),
            Bezier(Point(0, 0), Point(100, 80), Point(100, 100)).toCubic().toQuad().roundDecimalPlaces(2)
        )
    }
}
