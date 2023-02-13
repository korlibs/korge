package com.soywiz.korma.geom.triangle

import com.soywiz.korma.geom.MPoint
import kotlin.test.Test
import kotlin.test.assertEquals

class EdgeTest {
    @Test
    fun string() {
        assertEquals("Edge((1, 2), (3, 4))", Edge(MPoint(1, 2), MPoint(3, 4)).toString())
    }

    @Test
    fun equality() {
        val e1 = Edge(MPoint(1, 1), MPoint(2, 2))
        val e2 = Edge(MPoint(1, 1), MPoint(2, 2))
        val e3 = Edge(MPoint(2, 2), MPoint(1, 1))
        assertEquals(e1, e2)
        assertEquals(e1, e3)
    }

    @Test
    fun unique() {
        val e1 = Edge(MPoint(1, 1), MPoint(2, 2))
        val e2 = Edge(MPoint(2, 2), MPoint(3, 3))
        val e3 = Edge(MPoint(3, 3), MPoint(1, 1))
        assertEquals(listOf(MPoint(1, 1), MPoint(2, 2), MPoint(3, 3)), Edge.getUniquePointsFromEdges(listOf(e1, e2, e3)))
    }
}
