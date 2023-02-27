package com.soywiz.korge.view

import com.soywiz.korag.*
import com.soywiz.korag.log.AGBaseLog
import com.soywiz.korge.render.MaskStates
import com.soywiz.korge.render.VertexInfo
import com.soywiz.korge.tests.ViewsForTesting
import kotlin.test.Test
import kotlin.test.assertEquals

class MaskedViewTest : ViewsForTesting() {
    @Test
    fun testMaskBatches() {
        val vertices = arrayListOf<List<VertexInfo>>()
        val stencilsOpFunc = arrayListOf<AGStencilOpFunc>()
        val stencilsRef = arrayListOf<AGStencilReference>()

        val masked = MaskedView()
        masked.mask = SolidRect(32.0, 32.0)
        masked.solidRect(64, 64)

        testRenderContext(object : AGBaseLog() {

            override fun execute(command: AGCommand) {
                when (command) {
                    is AGBatch -> {
                        stencilsOpFunc += command.stencilOpFunc
                        stencilsRef += command.stencilRef
                    }
                    else -> Unit
                }
            }
        }) { ctx ->
            @Suppress("EXPERIMENTAL_API_USAGE")
            ctx.batch.beforeFlush { vertices.add(it.readVertices()) }
            masked.render(ctx)
        }

        assertEquals(2, vertices.size)
        assertEquals(listOf(
            MaskStates.STATE_SHAPE.stencilOpFunc,
            MaskStates.STATE_CONTENT.stencilOpFunc
        ), stencilsOpFunc)
        assertEquals(listOf(
            MaskStates.STATE_SHAPE.stencilRef.withReferenceValue(referenceValue = 1),
            MaskStates.STATE_CONTENT.stencilRef.withReferenceValue(referenceValue = 1)
        ), stencilsRef)
    }
}
