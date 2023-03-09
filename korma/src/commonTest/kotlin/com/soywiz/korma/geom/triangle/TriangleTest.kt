package com.soywiz.korma.geom.triangle

import com.soywiz.korma.geom.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TriangleTest {
    val t1 = Triangle(MPoint(0, 0), MPoint(10, 0), MPoint(0, 10))
    val t2 = Triangle(MPoint(0, 10), MPoint(10, 0), MPoint(10, 10))

    @Test
    fun test() {
        assertEqualsFloat(50.0, t1.area)
        assertEqualsFloat(0, t1.index(Point(0, 0)))
        assertEqualsFloat(1, t1.index(Point(10, 0)))
        assertEqualsFloat(2, t1.index(Point(0, 10)))

        assertEquals(2, t1.edgeIndex(Point(0, 0), Point(10, 0)))
        assertEquals(0, t1.edgeIndex(Point(10, 0), Point(0, 10)))
        assertEquals(1, t1.edgeIndex(Point(0, 10), Point(0, 0)))

        assertEquals(true, t1.containsPoint(Point(0, 0)))
        assertEquals(true, t1.containsPoint(Point(10, 0)))
        assertEquals(true, t1.containsPoint(Point(0, 10)))
        assertEquals(false, t1.containsPoint(Point(10, 10)))

        assertEqualsFloat(Point(0, 0), t1.point(0).point)
        assertEqualsFloat(Point(10, 0), t1.point(1).point)
        assertEqualsFloat(Point(0, 10), t1.point(2).point)

        assertEqualsFloat(Point(0, 0), t1.p0.point)
        assertEqualsFloat(Point(10, 0), t1.p1.point)
        assertEqualsFloat(Point(0, 10), t1.p2.point)


        assertEquals("[(0, 0), (10, 0), (0, 10), (10, 10)]", Triangle.getUniquePointsFromTriangles(listOf(t1, t2)).toString())

        assertEquals(true, t1.containsEdge(Edge(Point(0, 0), Point(10, 0))))
        assertEquals(true, t1.containsEdge(Edge(Point(10, 0), Point(0, 10))))
        assertEquals(true, t1.containsEdge(Edge(Point(0, 0), Point(0, 10))))
        assertEquals(false, t2.containsEdge(Edge(Point(0, 0), Point(0, 10))))
    }

    @Test
    fun inside() {
        // Edges
        assertEquals(true, t1.pointInsideTriangle(Point(0, 0)))
        assertEquals(true, t1.pointInsideTriangle(Point(10, 0)))
        assertEquals(true, t1.pointInsideTriangle(Point(0, 10)))
        assertEquals(true, t1.pointInsideTriangle(Point(2, 2)))
        assertEquals(false, t1.pointInsideTriangle(Point(-2, -2)))
        assertEquals(false, t1.pointInsideTriangle(Point(2, -2)))
        assertEquals(false, t1.pointInsideTriangle(Point(-2, 2)))
    }

    @Test
    fun extra() {
        assertEquals(Point(0, 10), t1.pointCW(Point(0, 0)))
        assertEquals(Point(10, 0), t1.pointCCW(Point(0, 0)))
        assertEquals(Point(0, 0), t1.pointCW(Point(10, 0)))
        assertEquals(Point(0, 10), t1.pointCCW(Point(10, 0)))
        assertEquals(Point(10, 0), t1.pointCW(Point(0, 10)))
        assertEquals(Point(0, 0), t1.pointCCW(Point(0, 10)))
    }
}
