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
        assertEqualsFloat(0, t1.index(MPoint(0, 0)))
        assertEqualsFloat(1, t1.index(MPoint(10, 0)))
        assertEqualsFloat(2, t1.index(MPoint(0, 10)))

        assertEquals(2, t1.edgeIndex(MPoint(0, 0), MPoint(10, 0)))
        assertEquals(0, t1.edgeIndex(MPoint(10, 0), MPoint(0, 10)))
        assertEquals(1, t1.edgeIndex(MPoint(0, 10), MPoint(0, 0)))

        assertEquals(true, t1.containsPoint(MPoint(0, 0)))
        assertEquals(true, t1.containsPoint(MPoint(10, 0)))
        assertEquals(true, t1.containsPoint(MPoint(0, 10)))
        assertEquals(false, t1.containsPoint(MPoint(10, 10)))

        assertEqualsFloat(MPoint(0, 0), t1.point(0))
        assertEqualsFloat(MPoint(10, 0), t1.point(1))
        assertEqualsFloat(MPoint(0, 10), t1.point(2))

        assertEqualsFloat(MPoint(0, 0), t1.p0)
        assertEqualsFloat(MPoint(10, 0), t1.p1)
        assertEqualsFloat(MPoint(0, 10), t1.p2)


        assertEquals("[(0, 0), (10, 0), (0, 10), (10, 10)]", Triangle.getUniquePointsFromTriangles(listOf(t1, t2)).toString())

        assertEquals(true, t1.containsEdge(Edge(MPoint(0, 0), MPoint(10, 0))))
        assertEquals(true, t1.containsEdge(Edge(MPoint(10, 0), MPoint(0, 10))))
        assertEquals(true, t1.containsEdge(Edge(MPoint(0, 0), MPoint(0, 10))))
        assertEquals(false, t2.containsEdge(Edge(MPoint(0, 0), MPoint(0, 10))))
    }

    @Test
    fun inside() {
        // Edges
        assertEquals(true, t1.pointInsideTriangle(MPoint(0, 0)))
        assertEquals(true, t1.pointInsideTriangle(MPoint(10, 0)))
        assertEquals(true, t1.pointInsideTriangle(MPoint(0, 10)))

        assertEquals(true, t1.pointInsideTriangle(MPoint(2, 2)))

        assertEquals(false, t1.pointInsideTriangle(MPoint(-2, -2)))
        assertEquals(false, t1.pointInsideTriangle(MPoint(2, -2)))
        assertEquals(false, t1.pointInsideTriangle(MPoint(-2, 2)))
    }

    @Test
    fun extra() {
        assertEquals(MPoint(0, 10), t1.pointCW(MPoint(0, 0)))
        assertEquals(MPoint(10, 0), t1.pointCCW(MPoint(0, 0)))

        assertEquals(MPoint(0, 0), t1.pointCW(MPoint(10, 0)))
        assertEquals(MPoint(0, 10), t1.pointCCW(MPoint(10, 0)))

        assertEquals(MPoint(10, 0), t1.pointCW(MPoint(0, 10)))
        assertEquals(MPoint(0, 0), t1.pointCCW(MPoint(0, 10)))
    }
}
