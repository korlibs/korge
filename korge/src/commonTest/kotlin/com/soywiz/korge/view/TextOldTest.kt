package com.soywiz.korge.view

import com.soywiz.korge.render.VertexInfo
import com.soywiz.korge.render.testRenderContext
import com.soywiz.korge.scene.debugBmpFontSync
import com.soywiz.korma.geom.MPoint
import com.soywiz.korma.geom.MRectangle
import com.soywiz.korma.geom.MSize
import kotlin.test.Test
import kotlin.test.assertEquals

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
                    MPoint(0, 0),
                    MPoint(32, 0),
                    MPoint(32, 32),
                    MPoint(0, 32)
                )
            ),
            vertices.map { it.map { it.xy } }
        )
        //println(ag.log)
    }

    @Test
    fun testDebugFontSize() {
        assertEquals(8.0, debugBmpFontSync.fontSize)
        assertEquals(MSize(192, 192), debugBmpFontSync.baseBmp.size)
    }

    @Test
    fun testBounds() {
        val text = TextOld("1", textSize = 32.0)
        assertEquals(MRectangle(0, 0, 28, 32), text.getLocalBoundsOptimizedAnchored())
    }

    @Test
    fun testHitTest() {
        val text = TextOld("1", textSize = 32.0)
        assertEquals(text, text.hitTest(10, 5))
        assertEquals(null, text.hitTest(30, 5))
        text.setTextBounds(MRectangle(0, 0, 32, 32))
        assertEquals(text, text.hitTest(10, 5))
        assertEquals(text, text.hitTest(30, 5))
        assertEquals(null, text.hitTest(33, 5))
    }
}
