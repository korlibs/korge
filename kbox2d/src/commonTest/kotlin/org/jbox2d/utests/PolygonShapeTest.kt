package org.jbox2d.utests

import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Vec2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class PolygonShapeTest {
    @Test
    fun test() {
        val shape = PolygonShape()
        val vertexArray = arrayOf(Vec2(1f, 1f), Vec2(1f, 0f), Vec2(0f, 0f))
        shape.set(vertexArray, vertexArray.size)

        assertEquals(shape.count, vertexArray.size)
        (0 until shape.count).forEach {
            assertTrue(vertexArray.contains(shape.getVertex(it)))
        }
    }
}
