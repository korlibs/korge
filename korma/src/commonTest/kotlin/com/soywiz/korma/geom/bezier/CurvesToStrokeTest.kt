package com.soywiz.korma.geom.bezier

import com.soywiz.korma.geom.shape.buildVectorPath
import com.soywiz.korma.geom.vector.getCurves
import com.soywiz.korma.geom.vector.line
import com.soywiz.korma.geom.vector.lineTo
import com.soywiz.korma.geom.vector.moveTo
import com.soywiz.korma.geom.vector.star
import kotlin.test.Test
import kotlin.test.assertEquals

class CurvesToStrokeTest {
    @Test
    fun test() {
        val path = buildVectorPath {
            line(0, 0, 100, 100)
        }
        val strokePoints = path.getCurves().toStrokePoints(10.0).vector
        for (n in 0 until strokePoints.size) {
            println(strokePoints.vectorToString(n))
        }
        println(strokePoints)
    }

    @Test
    fun testClosed() {
        val path = buildVectorPath {
            star(6, 10.0, 20.0)
        }
        assertEquals(true, path.getCurves().closed)
    }

    @Test
    fun testSplit() {
        val curves = buildVectorPath {
            moveTo(0, 0)
            lineTo(100, 0)
            lineTo(200, 0)
        }.getCurves()
        assertEquals(Curves(Bezier(100,0, 150,0)), curves.split(0.5, 0.75))
        assertEquals(Curves(Bezier(50,0, 100,0)), curves.split(0.25, 0.5))
        assertEquals(Curves(Bezier(50,0, 100,0), Bezier(100,0, 150,0)), curves.split(0.25, 0.75))
    }
}
