package com.soywiz.korma.geom

import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.shape.ops.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.triangle.*
import com.soywiz.korma.triangle.pathfind.*
import com.soywiz.korma.triangle.triangulate.*
import kotlin.math.*
import kotlin.test.*

class IntegrationShapeTest {
    @Test
    fun vectorPathToShape2d() {
        val exactArea = Shape2d.Circle(0, 0, 100).area
        val vp = VectorPath().apply { circle(Point(0, 0), 100f) }
        val shape = vp.toShape2d()
        assertEquals(true, shape.closed)
        assertTrue(abs(exactArea - shape.area) / exactArea < 0.01)
        assertEquals(81, shape.paths.totalVertices)
    }

    @Test
    fun triangulate() {
        val shape = MRectangle(0, 0, 100, 100).toShape()
        //println(shape)
        //println(shape.getAllPoints())
        //println(shape.getAllPoints().toPoints())
        assertEquals(
            "[Triangle((0, 100), (100, 0), (100, 100)), Triangle((0, 100), (0, 0), (100, 0))]",
            shape.triangulateFlat().toString()
        )
    }

    @Test
    fun pathFind() {
        assertEquals(
            "[(10, 10), (90, 90)]",
            MRectangle(0, 0, 100, 100).toShape().pathFind(MPoint(10, 10), MPoint(90, 90)).toString()
        )
        assertEquals(
            "[(10, 10), (100, 50), (120, 52)]",
            (MRectangle(0, 0, 100, 100).toShape() union MRectangle(100, 50, 50, 50).toShape()).pathFind(
                MPoint(10, 10),
                MPoint(120, 52)
            ).toString()
        )
    }
}
