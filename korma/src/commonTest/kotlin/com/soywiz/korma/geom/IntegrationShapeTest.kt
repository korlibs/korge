package com.soywiz.korma.geom

import com.soywiz.korma.geom.shape.Shape2d
import com.soywiz.korma.geom.shape.ops.plus
import com.soywiz.korma.geom.shape.toShape
import com.soywiz.korma.geom.shape.toShape2d
import com.soywiz.korma.geom.shape.totalVertices
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.korma.geom.vector.circle
import com.soywiz.korma.triangle.area
import com.soywiz.korma.triangle.pathfind.pathFind
import com.soywiz.korma.triangle.triangulate.triangulateFlat
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntegrationShapeTest {
    @Test
    fun vectorPathToShape2d() {
        val exactArea = Shape2d.Circle(0, 0, 100).area
        val vp = VectorPath().apply { circle(0, 0, 100) }
        val shape = vp.toShape2d()
        assertEquals(true, shape.closed)
        assertTrue(abs(exactArea - shape.area) / exactArea < 0.01)
        assertEquals(81, shape.paths.totalVertices)
    }

    @Test
    fun triangulate() {
        val shape = Rectangle(0, 0, 100, 100).toShape()
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
            Rectangle(0, 0, 100, 100).toShape().pathFind(IPoint(10, 10), IPoint(90, 90)).toString()
        )
        assertEquals(
            "[(10, 10), (100, 50), (120, 52)]",
            (Rectangle(0, 0, 100, 100).toShape() + Rectangle(100, 50, 50, 50).toShape()).pathFind(
                IPoint(10, 10),
                IPoint(120, 52)
            ).toString()
        )
    }
}
