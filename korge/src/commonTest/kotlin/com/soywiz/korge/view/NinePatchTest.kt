package com.soywiz.korge.view

import com.soywiz.korag.log.LogAG
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.VertexInfo
import com.soywiz.korge.render.testRenderContext
import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.NinePatchBitmap32
import com.soywiz.korim.bitmap.NinePatchBmpSlice
import com.soywiz.korim.bitmap.readNinePatch
import com.soywiz.korio.file.std.resourcesVfs
import kotlin.test.Test
import kotlin.test.assertEquals

class NinePatchTest : ViewsForTesting() {
    @Test
    fun testNinePatch() = viewsTest {
        val vertices = arrayListOf<List<VertexInfo>>()
        val ninePatch = resourcesVfs["npatch/9patch.9.png"].readNinePatch()

        ninePatch(ninePatch, 450.0, 600.0) {
            position(0, 0)
        }
        val log = testRenderContext { ctx ->
            ctx.batch.beforeFlush { vertices.add(it.readVertices()) }
            render(ctx)
        }
        assertEquals(1, vertices.size) // 1 batch
        val batch = vertices[0]
        assertEquals(36, batch.size) // 36 vertices
        assertEquals(
            """
                [(0,0), (1, 1)]
                [(49,0), (50, 1)]
                [(49,47), (50, 48)]
                [(0,47), (1, 48)]
                [(49,0), (50, 1)]
                [(400,0), (78, 1)]
                [(400,47), (78, 48)]
                [(49,47), (50, 48)]
                [(400,0), (78, 1)]
                [(450,0), (128, 1)]
                [(450,47), (128, 48)]
                [(400,47), (78, 48)]
                [(0,47), (1, 48)]
                [(49,47), (50, 48)]
                [(49,553), (50, 57)]
                [(0,553), (1, 57)]
                [(49,47), (50, 48)]
                [(400,47), (78, 48)]
                [(400,553), (78, 57)]
                [(49,553), (50, 57)]
                [(400,47), (78, 48)]
                [(450,47), (128, 48)]
                [(450,553), (128, 57)]
                [(400,553), (78, 57)]
                [(0,553), (1, 57)]
                [(49,553), (50, 57)]
                [(49,600), (50, 104)]
                [(0,600), (1, 104)]
                [(49,553), (50, 57)]
                [(400,553), (78, 57)]
                [(400,600), (78, 104)]
                [(49,600), (50, 104)]
                [(400,553), (78, 57)]
                [(450,553), (128, 57)]
                [(450,600), (128, 104)]
                [(400,600), (78, 104)]
            """.trimIndent(),
            batch.joinToString("\n") { it.toStringXYUV() }
        )
        //assertEquals("--", log.getLogAsString())
    }

    @Test
    fun testNinePatchSmaller() = viewsTest {
        val ninePatch = resourcesVfs["npatch/9patch.9.png"].readNinePatch()

        fun computeVertices(ninePatch: NinePatchBitmap32, width: Double, height: Double): List<List<VertexInfo>> {
            val vertices = arrayListOf<List<VertexInfo>>()
            stage.removeChildren()
            testRenderContext { ctx ->
                val view = ninePatch(ninePatch, width, height) {
                    position(0, 0)
                }
                ctx.batch.beforeFlush { vertices.add(it.readVertices()) }
                view.render(ctx)
            }
            return vertices
        }

        fun computeInterestingPoints(width: Number, height: Number): String {
            val batch = computeVertices(ninePatch, width.toDouble(), height.toDouble())[0]
            return listOf(batch[0], batch[2], batch[32], batch[34]).joinToString(", ") { it.toStringXY() }
        }

        assertEquals(
            """
                [0,0], [49,47], [400,553], [450,600]
                [0,0], [15,14], [435,18], [450,32]
                [0,0], [12,11], [20,589], [32,600]
                [0,0], [12,11], [20,21], [32,32]
            """.trimIndent(),
            listOf(
                computeInterestingPoints(450, 600),
                computeInterestingPoints(450, 32),
                computeInterestingPoints(32, 600),
                computeInterestingPoints(32, 32)
            ).joinToString("\n")
        )
    }

    @Test
    fun testNinePatchColorInvalidation() {
        val ctx = RenderContext(LogAG())
        val container = Container()
        val ninePatch = container.ninePatch(NinePatchBmpSlice(Bitmap32(32, 32, premultiplied = true)), 16.0, 16.0)
        ninePatch.render(ctx)
        assertEquals(1, ninePatch.renderedVersion)
        container.alpha = 0.5
        ninePatch.render(ctx)
        assertEquals(2, ninePatch.renderedVersion)
        ninePatch.render(ctx)
        assertEquals(2, ninePatch.renderedVersion)
    }
}

//
// +-+--------+-+
// +-+--------+-+
// | |        | |
// | |        | |
// +-+--------+-+
// +-+--------+-+
