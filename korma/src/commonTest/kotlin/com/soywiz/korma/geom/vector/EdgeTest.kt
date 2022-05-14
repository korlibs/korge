package com.soywiz.korma.geom.vector

import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.assertEquals
import com.soywiz.korma.geom.degrees
import kotlin.test.Test
import kotlin.test.assertEquals

class EdgeTest {
    @Test
    fun test() {
        // /|
        val a = Edge(0, 10, 10, 0)
        val b = Edge(10, 0, 10, 10)
        val c = Edge().setToHalf(a, b)
        assertEquals(Point(10, 0), Edge.getIntersectXY(a, b))
        assertEquals(Point(10, 0), Edge.getIntersectXY(a, c))
        assertEquals(90.degrees, Edge.angleBetween(Edge(0, 0, 10, 0), Edge(10, 0, 10, 10)))
    }
}
