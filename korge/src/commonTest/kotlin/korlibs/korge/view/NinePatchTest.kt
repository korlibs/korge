package korlibs.korge.view

import korlibs.graphics.log.*
import korlibs.image.bitmap.*
import korlibs.io.file.std.*
import korlibs.korge.render.*
import korlibs.korge.tests.*
import korlibs.math.geom.*
import kotlin.test.*

class NinePatchTest : ViewsForTesting() {
    @Test
    fun testNinePatch() = viewsTest {
        val vertices = arrayListOf<List<VertexInfo>>()
        val ninePatch = resourcesVfs["npatch/9patch.9.png"].readNinePatch()

        ninePatch(ninePatch, Size(450f, 600f)) {
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

        fun computeVertices(ninePatch: NinePatchBitmap32, width: Float, height: Float): List<List<VertexInfo>> {
            val vertices = arrayListOf<List<VertexInfo>>()
            stage.removeChildren()
            testRenderContext { ctx ->
                val view = ninePatch(ninePatch, Size(width, height)) {
                    position(0, 0)
                }
                ctx.batch.beforeFlush { vertices.add(it.readVertices()) }
                view.render(ctx)
            }
            return vertices
        }

        fun computeInterestingPoints(width: Number, height: Number): String {
            val batch = computeVertices(ninePatch, width.toFloat(), height.toFloat())[0]
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
        val ctx = RenderContext(AGLog())
        val container = Container()
        val ninePatch = container.ninePatch(NinePatchBmpSlice(Bitmap32(32, 32, premultiplied = true)), Size(16f, 16f))
        ninePatch.render(ctx)
        assertEquals(1, ninePatch.renderedVersion)
        container.alphaF = 0.5f
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
