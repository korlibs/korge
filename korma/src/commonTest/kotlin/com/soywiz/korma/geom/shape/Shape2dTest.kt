package com.soywiz.korma.geom.shape

import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.PointArrayList
import com.soywiz.korma.geom.map
import com.soywiz.korma.geom.shape.ops.extend
import com.soywiz.korma.geom.shape.ops.intersection
import com.soywiz.korma.geom.shape.ops.union
import com.soywiz.korma.geom.shape.ops.xor
import com.soywiz.korma.geom.toPoints
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.korma.geom.vector.circle
import com.soywiz.korma.geom.vector.lineTo
import com.soywiz.korma.geom.vector.moveTo
import com.soywiz.korma.geom.vector.rect
import com.soywiz.korma.internal.niceStr
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class Shape2dTest {
    @Test
    fun test() {
        assertEquals(
            "Rectangle(x=0, y=0, width=100, height=100)",
            VectorPath { rect(0, 0, 100, 100) }.toShape2d(closed = true).toString()
        )

        assertEquals(
            "Complex(items=[Rectangle(x=0, y=0, width=100, height=100), Rectangle(x=300, y=0, width=100, height=100)])",
            VectorPath {
                rect(0, 0, 100, 100)
                rect(300, 0, 100, 100)
            }.toShape2d(closed = true).toString()
        )

        assertEquals(
            "Polygon(points=[(0, 0), (100, 0), (100, 100)])",
            VectorPath {
                moveTo(0, 0)
                lineTo(100, 0)
                lineTo(100, 100)
                close()
            }.toShape2d(closed = true).toString()
        )
    }

    @Test
    fun test_ToRectangleOrNull() {
        val a = Point(1.0, 1.0)
        val b = Point(1.0, 2.0)
        val c = Point(2.0, 2.0)
        val d = Point(2.0, 1.0)

        assertNotNull(PointArrayList(a, b, c, d).toRectangleOrNull())
        assertNotNull(PointArrayList(d, a, b, c).toRectangleOrNull())
        assertNotNull(PointArrayList(c, d, a, b).toRectangleOrNull())
        assertNotNull(PointArrayList(b, c, d, a).toRectangleOrNull())
        assertNotNull(PointArrayList(b, a, c, d).toRectangleOrNull())
        assertNotNull(PointArrayList(a, c, b, d).toRectangleOrNull())
        assertNotNull(PointArrayList(a, b, d, c).toRectangleOrNull())

        assertNull(PointArrayList(a).toRectangleOrNull())
        assertNull(PointArrayList(a, b).toRectangleOrNull())
        assertNull(PointArrayList(a, b, c).toRectangleOrNull())
        assertNull(PointArrayList(a, b, c, d, a).toRectangleOrNull())

        assertNull(PointArrayList(a, a, b, c).toRectangleOrNull())
        assertNull(PointArrayList(a, b, a, c).toRectangleOrNull())
        assertNull(PointArrayList(a, b, c, a).toRectangleOrNull())
        assertNull(PointArrayList(a, b, b, c).toRectangleOrNull())
        assertNull(PointArrayList(a, a, a, a).toRectangleOrNull())


        assertNull(PointArrayList(Point(0.0, 1.0), Point(1.0, 2.0), Point(2.0, 2.0), Point(2.0, 1.0)).toRectangleOrNull())
        assertNull(PointArrayList(Point(1.0, 1.0), Point(0.0, 2.0), Point(2.0, 2.0), Point(2.0, 1.0)).toRectangleOrNull())
        assertNull(PointArrayList(Point(1.0, 1.0), Point(1.0, 2.0), Point(0.0, 2.0), Point(2.0, 1.0)).toRectangleOrNull())
        assertNull(PointArrayList(Point(1.0, 1.0), Point(1.0, 2.0), Point(2.0, 2.0), Point(0.0, 1.0)).toRectangleOrNull())
        assertNull(PointArrayList(Point(1.0, 0.0), Point(1.0, 2.0), Point(2.0, 2.0), Point(2.0, 1.0)).toRectangleOrNull())
        assertNull(PointArrayList(Point(1.0, 1.0), Point(1.0, 0.0), Point(2.0, 2.0), Point(2.0, 1.0)).toRectangleOrNull())
        assertNull(PointArrayList(Point(1.0, 1.0), Point(1.0, 2.0), Point(2.0, 0.0), Point(2.0, 1.0)).toRectangleOrNull())
        assertNull(PointArrayList(Point(1.0, 1.0), Point(1.0, 2.0), Point(2.0, 2.0), Point(2.0, 0.0)).toRectangleOrNull())
    }

    @Test
    fun name() {
        assertEquals(
            "Rectangle(x=5, y=0, width=5, height=10)",
            (Shape2d.Rectangle(0, 0, 10, 10) intersection Shape2d.Rectangle(5, 0, 10, 10)).toString()
        )

        assertEquals(
            "Polygon(points=[(10, 5), (15, 5), (15, 15), (5, 15), (5, 10), (0, 10), (0, 0), (10, 0)])",
            (Shape2d.Rectangle(0, 0, 10, 10) union Shape2d.Rectangle(5, 5, 10, 10)).toString()
        )

        assertEquals(
            "Complex(items=[Rectangle(x=10, y=0, width=5, height=10), Rectangle(x=0, y=0, width=5, height=10)])",
            (Shape2d.Rectangle(0, 0, 10, 10) xor Shape2d.Rectangle(5, 0, 10, 10)).toString()
        )
    }

    @Test
    fun extend() {
        assertEquals(
            "Rectangle(x=-10, y=-10, width=30, height=30)",
            (Shape2d.Rectangle(0, 0, 10, 10).extend(10.0)).toString()
        )
    }

    @Test
    fun testIntersects() {
        assertEquals(false, Shape2d.Circle(0, 0, 20).intersectsWith(Shape2d.Circle(40, 0, 20)))
        assertEquals(true, Shape2d.Circle(0, 0, 20).intersectsWith(Shape2d.Circle(38, 0, 20)))
    }

    @Test
    fun testToPaths() {
        val points = buildVectorPath {
            moveTo(100, 100)
            lineTo(400, 400)
            lineTo(200, 500)
            lineTo(500, 500)
            lineTo(200, 700)
            close()

            moveTo(800, 600)
            lineTo(900, 600)
            lineTo(900, 400)
            close()

            moveTo(800, 100)
            lineTo(800, 110)

            moveTo(750, 100)
            lineTo(750, 110)
        }.toPathList()

        assertEquals("""
            closed : (100,100),(400,400),(200,500),(500,500),(200,700)
            closed : (800,600),(900,600),(900,400)
            opened : (800,100),(800,110)
            opened : (750,100),(750,110)
        """.trimIndent(),
            points.joinToString("\n") {
                val kind = if (it.closed) "closed" else "opened"
                "$kind : " + it.toPoints().joinToString(",") { "(${it.x.niceStr},${it.y.niceStr})" }
            }
        )
    }

    @Test
    fun testApproximateCurve() {
        fun approx(start: Boolean, end: Boolean) = arrayListOf<String>().also { out ->
            approximateCurve(
                10,
                { ratio, get -> get(ratio * 100, -ratio * 100) },
                { x, y -> out.add("(${x.toInt()},${y.toInt()})") },
                start, end
            )
        }.joinToString(" ")
        assertEquals(
            "(5,-5) (10,-10) (15,-15) (20,-20) (25,-25) (30,-30) (35,-35) (40,-40) (45,-45) (50,-50) (55,-55) (60,-60) (65,-65) (70,-70) (75,-75) (80,-80) (85,-85) (90,-90) (95,-95)",
            approx(start = false, end = false)
        )
        assertEquals(
            "(5,-5) (10,-10) (15,-15) (20,-20) (25,-25) (30,-30) (35,-35) (40,-40) (45,-45) (50,-50) (55,-55) (60,-60) (65,-65) (70,-70) (75,-75) (80,-80) (85,-85) (90,-90) (95,-95) (100,-100)",
            approx(start = false, end = true)
        )
        assertEquals(
            "(0,0) (5,-5) (10,-10) (15,-15) (20,-20) (25,-25) (30,-30) (35,-35) (40,-40) (45,-45) (50,-50) (55,-55) (60,-60) (65,-65) (70,-70) (75,-75) (80,-80) (85,-85) (90,-90) (95,-95)",
            approx(start = true, end = false)
        )
        assertEquals(
            "(0,0) (5,-5) (10,-10) (15,-15) (20,-20) (25,-25) (30,-30) (35,-35) (40,-40) (45,-45) (50,-50) (55,-55) (60,-60) (65,-65) (70,-70) (75,-75) (80,-80) (85,-85) (90,-90) (95,-95) (100,-100)",
            approx(start = true, end = true)
        )
    }

    @Test
    fun testApproximateCurve2() {
        val path = buildVectorPath { circle(0, 0, 10) }
        val pointsStr = path.getPoints2().map { x, y -> "(${(x * 100).toInt()},${(y * 100).toInt()})" }.joinToString(" ")
        assertEquals(
            "(1000,0) (996,-82) (986,-162) (970,-240) (949,-316) (921,-389) (888,-459) (850,-526) (807,-590) (759,-650) (707,-707) (650,-759) (590,-807) (526,-850) (459,-888) (389,-921) (316,-949) (240,-970) (162,-986) (82,-996) (0,-1000) (-82,-996) (-162,-986) (-240,-970) (-316,-949) (-389,-921) (-459,-888) (-526,-850) (-590,-807) (-650,-759) (-707,-707) (-759,-650) (-807,-590) (-850,-526) (-888,-459) (-921,-389) (-949,-316) (-970,-240) (-986,-162) (-996,-82) (-1000,0) (-996,82) (-986,162) (-970,240) (-949,316) (-921,389) (-888,459) (-850,526) (-807,590) (-759,650) (-707,707) (-650,759) (-590,807) (-526,850) (-459,888) (-389,921) (-316,949) (-240,970) (-162,986) (-82,996) (0,1000) (82,996) (162,986) (240,970) (316,949) (389,921) (459,888) (526,850) (590,807) (650,759) (707,707) (759,650) (807,590) (850,526) (888,459) (921,389) (949,316) (970,240) (986,162) (996,82) (1000,0) (1000,0)",
            pointsStr
        )
    }
}
