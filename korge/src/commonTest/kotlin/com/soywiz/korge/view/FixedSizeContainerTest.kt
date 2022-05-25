package com.soywiz.korge.view

import com.soywiz.korag.AG
import com.soywiz.korag.log.LogAG
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.testRenderContext
import com.soywiz.korma.geom.SizeInt
import kotlin.test.Test
import kotlin.test.assertEquals

class FixedSizeContainerTest {
    @Test
    fun test() {
        testRenderContext {
            val windowSize = SizeInt(640, 480)
            val virtualSize = SizeInt(512, 512)
            val ag: LogAG = LogAG(windowSize.width, windowSize.height)
            val bp: BoundsProvider = BoundsProvider.Base()
            bp.setBoundsInfo(virtualSize.width, virtualSize.height, windowSize)
            val container = ClipContainer(30.0, 40.0).xy(110, 120)
            val log = arrayListOf<String>()
            container.addChild(object : View() {
                override fun renderInternal(ctx: RenderContext) {
                    log += "${ctx.batch.scissor}"
                }
            })
            testRenderContext(ag, bp) { ctx ->
                container.render(ctx)
                ctx.renderToTexture(400, 400, {
                    container.render(ctx)
                }) {
                }
            }
            assertEquals(
                """
                    Scissor(x=137.5, y=112.5, width=37.5, height=37.5)
                    Scissor(x=110, y=120, width=30, height=40)
                """.trimIndent(),
                log.joinToString("\n")
            )
        }
    }
}
