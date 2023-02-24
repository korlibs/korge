package com.soywiz.korge.view.filter

import com.soywiz.korag.log.AGLog
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.view.View
import com.soywiz.korio.async.suspendTest
import com.soywiz.korma.geom.MRectangle
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposedFilterTest {
    val log = arrayListOf<String>()

    @Test
    fun test() = suspendTest {
        val ctx = RenderContext(AGLog())
        val rect = object : View() {
            override fun renderInternal(ctx: RenderContext) {
                log += "image.frameBuffers=${ctx.frameBuffers.totalItemsInUse}"
            }

            override fun getLocalBoundsInternal(out: MRectangle) {
                out.setTo(0, 0, 10, 10)
            }
        }
        rect.addFilters(SwizzleColorsFilter("rrra"), ColorMatrixFilter(ColorMatrixFilter.SEPIA_MATRIX), BlurFilter(), WaveFilter())
        rect.filter = object : ComposedFilter(rect.filter?.allFilters ?: emptyList()) {
            override fun stepBefore() {
                log += "filter.frameBuffers=${ctx.frameBuffers.totalItemsInUse}"
            }
        }
        rect.render(ctx)
        log += "end.frameBuffers=${ctx.frameBuffers.totalItemsInUse}"
        assertEquals(
            """
                image.frameBuffers=1
                filter.frameBuffers=1
                filter.frameBuffers=2
                filter.frameBuffers=2
                filter.frameBuffers=2
                filter.frameBuffers=2
                end.frameBuffers=0
            """.trimIndent(),
            log.joinToString("\n")
        )
    }
}
