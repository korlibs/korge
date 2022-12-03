package com.soywiz.korge.view.filter

import com.soywiz.korag.*
import com.soywiz.korag.log.LogAG
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.view.View
import com.soywiz.korio.async.suspendTest
import com.soywiz.korma.geom.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposedFilterTest {
    val log = arrayListOf<String>()

    @Test
    fun test() = suspendTest {
        val ctx = RenderContext(NAGLog())
        val rect = object : View() {
            override fun renderInternal(ctx: RenderContext) {
                log += "image.renderBuffers=${ctx.nag.totalRenderBuffers}"
            }

            override fun getLocalBoundsInternal(out: Rectangle) {
                out.setTo(0, 0, 10, 10)
            }
        }
        rect.addFilters(SwizzleColorsFilter("rrra"), ColorMatrixFilter(ColorMatrixFilter.SEPIA_MATRIX), BlurFilter(), WaveFilter())
        rect.filter = object : ComposedFilter(rect.filter?.allFilters ?: emptyList()) {
            override fun stepBefore() {
                log += "filter.renderBuffers=${ctx.nag.totalRenderBuffers}"
            }
        }
        rect.render(ctx)
        log += "end.renderBuffers=${ctx.nag.totalRenderBuffers}"
        assertEquals(
            """
                image.renderBuffers=1
                filter.renderBuffers=1
                filter.renderBuffers=2
                filter.renderBuffers=2
                filter.renderBuffers=2
                filter.renderBuffers=2
                end.renderBuffers=0
            """.trimIndent(),
            log.joinToString("\n")
        )
    }
}
