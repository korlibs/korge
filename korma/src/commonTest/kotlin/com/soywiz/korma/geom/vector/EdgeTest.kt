package com.soywiz.korma.geom.vector

import com.soywiz.korma.geom.MPoint
import com.soywiz.korma.geom.assertEquals
import com.soywiz.korma.geom.degrees
import kotlin.test.Test
import kotlin.test.assertEquals

class EdgeTest {
    @Test
    fun test() {
        // /|
        val a = MEdge(0, 10, 10, 0)
        val b = MEdge(10, 0, 10, 10)
        val c = MEdge().setToHalf(a, b)
        assertEquals(MPoint(10, 0), MEdge.getIntersectXY(a, b))
        assertEquals(MPoint(10, 0), MEdge.getIntersectXY(a, c))
        assertEquals(90.degrees, MEdge.angleBetween(MEdge(0, 0, 10, 0), MEdge(10, 0, 10, 10)))
    }
}
