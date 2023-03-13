package com.soywiz.korma.geom.triangle

import com.soywiz.korma.geom.*
import kotlin.test.Test
import kotlin.test.assertEquals

class EdgeTest {
    @Test
    fun string() {
        assertEquals("Edge((1, 2), (3, 4))", Edge(Point(1, 2), Point(3, 4)).toString())
    }

    @Test
    fun equality() {
        val e1 = Edge(Point(1, 1), Point(2, 2))
        val e2 = Edge(Point(1, 1), Point(2, 2))
        val e3 = Edge(Point(2, 2), Point(1, 1))
        assertEquals(e1, e2)
        assertEquals(e1, e3)
    }

    @Test
    fun unique() {
        val e1 = Edge(Point(1, 1), Point(2, 2))
        val e2 = Edge(Point(2, 2), Point(3, 3))
        val e3 = Edge(Point(3, 3), Point(1, 1))
        assertEquals(pointArrayListOf(Point(1, 1), Point(2, 2), Point(3, 3)), Edge.getUniquePointsFromEdges(listOf(e1, e2, e3)))
    }
}
