package com.soywiz.korma.geom

import com.soywiz.korma.geom.convex.Convex
import com.soywiz.korma.geom.shape.buildVectorPath
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.korma.geom.vector.circle
import com.soywiz.korma.geom.vector.rect
import com.soywiz.korma.geom.vector.regularPolygon
import com.soywiz.korma.geom.vector.roundRect
import com.soywiz.korma.geom.vector.star
import kotlin.test.Test
import kotlin.test.assertEquals

class ConvexTest {
    @Test
    fun testRect() {
        assertEquals(true, isConvexPath { rect(0, 0, 100, 100) })
    }
    @Test
    fun testCircle() {
        assertEquals(true, isConvexPath { circle(0, 0, 100) })
    }
    @Test
    fun testRoundRect() {
        assertEquals(true, isConvexPath { roundRect(0, 0, 100, 100, 10, 10) })
    }
    @Test
    fun testRegularPolygon() {
        assertEquals(true, isConvexPath { regularPolygon(6, 100.0) })
    }
    @Test
    fun testStar() {
        assertEquals(false, isConvexPath { star(6, 50.0, 100.0) })
    }

    private fun isConvexPath(block: VectorPath.() -> Unit): Boolean {
        return Convex.isConvex(buildVectorPath { block() })
    }
}
