package com.soywiz.korge.view

import com.soywiz.korag.AG
import com.soywiz.korag.log.LogBaseAG
import com.soywiz.korge.render.MaskStates
import com.soywiz.korge.render.VertexInfo
import com.soywiz.korge.tests.ViewsForTesting
import kotlin.test.Test
import kotlin.test.assertEquals

class MaskedViewTest : ViewsForTesting() {
    @Test
    fun testMaskBatches() {
        val vertices = arrayListOf<List<VertexInfo>>()
        val stencils = arrayListOf<AG.StencilState>()

        val masked = MaskedView()
        masked.mask = SolidRect(32.0, 32.0)
        masked.solidRect(64, 64)

        testRenderContext(object : LogBaseAG() {
            override fun draw(batch: Batch) {
                stencils += batch.stencil.copy()
            }
        }) { ctx ->
            @Suppress("EXPERIMENTAL_API_USAGE")
            ctx.batch.beforeFlush { vertices.add(it.readVertices()) }
            masked.render(ctx)
        }

        assertEquals(2, vertices.size)
        assertEquals(listOf(
            MaskStates.STATE_SHAPE.stencil.copy(referenceValue = 1),
            MaskStates.STATE_CONTENT.stencil.copy(referenceValue = 1)
        ), stencils)
    }
}
