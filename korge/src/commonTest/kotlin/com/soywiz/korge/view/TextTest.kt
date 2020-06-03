package com.soywiz.korge.view

import com.soywiz.korge.render.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class TextTest {
	@Test
	fun testRender() {
        testRenderContext { ctx ->
            val text = Text("1").apply {
                textSize = 32.0
            }
            text.render(ctx)
            assertEquals(Point(0, 0), ctx.batch.readVertex(0).xy)
            assertEquals(Point(32, 0), ctx.batch.readVertex(1).xy)
            assertEquals(Point(32, 32), ctx.batch.readVertex(2).xy)
            assertEquals(Point(0, 32), ctx.batch.readVertex(3).xy)
        }
        //println(ag.log)
	}
}
