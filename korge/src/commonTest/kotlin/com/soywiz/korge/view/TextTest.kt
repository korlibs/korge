package com.soywiz.korge.view

import com.soywiz.korge.render.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class TextTest {
	@Test
	fun testRender() {
        val vertices = arrayListOf<List<VertexInfo>>()

        testRenderContext { ctx ->
            ctx.batch.beforeFlush {
                vertices.add(it.readVertices())
            }
            val text = Text("1").apply {
                textSize = 32.0
            }
            text.render(ctx)
        }
        assertEquals(
            listOf(listOf(
                Point(0, 0),
                Point(32, 0),
                Point(32, 32),
                Point(0, 32)
            )),
            vertices.map { it.map { it.xy } }
        )
        //println(ag.log)
	}
}
