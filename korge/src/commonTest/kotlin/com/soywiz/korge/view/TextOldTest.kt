package com.soywiz.korge.view

import com.soywiz.korge.render.*
import com.soywiz.korge.scene.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class TextOldTest {
    @Test
    fun testRender() {
        val vertices = arrayListOf<List<VertexInfo>>()

        testRenderContext { ctx ->
            ctx.batch.beforeFlush {
                vertices.add(it.readVertices())
            }
            val text = TextOld("1").apply {
                textSize = 32.0
            }
            text.render(ctx)
        }
        assertEquals(
            listOf(
                listOf(
                    Point(0, 0),
                    Point(32, 0),
                    Point(32, 32),
                    Point(0, 32)
                )
            ),
            vertices.map { it.map { it.xy } }
        )
        //println(ag.log)
    }

    @Test
    fun testDebugFontSize() {
        assertEquals(8.0, debugBmpFontSync.fontSize)
        assertEquals(Size(192, 192), debugBmpFontSync.baseBmp.size)
    }

    @Test
    fun testBounds() {
        val text = TextOld("1", textSize = 32.0)
        assertEquals(Rectangle(0, 0, 28, 32), text.getLocalBoundsOptimizedAnchored())
    }

    @Test
    fun testHitTest() {
        val text = TextOld("1", textSize = 32.0)
        assertEquals(text, text.hitTest(10, 5))
        assertEquals(null, text.hitTest(30, 5))
        text.setTextBounds(Rectangle(0, 0, 32, 32))
        assertEquals(text, text.hitTest(10, 5))
        assertEquals(text, text.hitTest(30, 5))
        assertEquals(null, text.hitTest(33, 5))
    }
}
