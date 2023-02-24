package com.soywiz.korge.view

import com.soywiz.kgl.*
import com.soywiz.korag.*
import com.soywiz.korag.gl.*
import com.soywiz.korge.render.*
import com.soywiz.korge.test.*
import com.soywiz.korge.testing.*
import com.soywiz.korge.tests.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import org.junit.*

class ViewsOpenglJvmTest : ViewsForTesting(log = true) {
    val logGl: KmlGlProxyLogToString = object : KmlGlProxyLogToString() {
        override fun getString(name: String, params: List<Any?>, result: Any?): String? = when (name) {
            "shaderSource" -> super.getString(name, params.dropLast(1) + listOf("..."), result)
            else -> super.getString(name, params, result)
        }
    }

    override fun createAg(): AG = AGOpengl(logGl)

    // This checks that the texture is generated with the default size (dirty=true fix)
    @Test
    fun testIdentityFilterFor128x128() = korgeScreenshotTest(200, 200) {
        views.stage += Image(Bitmap32(102, 102, Colors.RED.premultiplied)).also {
            it.filter = IdentityFilter
        }
        assertScreenshot()
    }

    @Test
    fun testRenderToTextureWithStencil() = korgeScreenshotTest(512, 512) {
        this += object : View() {
            override fun renderInternal(ctx: RenderContext) {
                ctx.renderToTexture(100, 100, render = {
                   ctx.useCtx2d { ctx2d ->
                       ctx2d.rect(0.0, 0.0, 100.0, 100.0, Colors.RED)
                   }
                }, hasStencil = true) { tex ->
                    ctx.useBatcher { batcher ->
                        batcher.drawQuad(tex)
                    }
                }
                ctx.renderToTexture(100, 100, render = {
                    ctx.useCtx2d { ctx2d ->
                        ctx2d.rect(0.0, 0.0, 100.0, 100.0, Colors.BLUE)
                    }
                }, hasDepth = true, hasStencil = true) { tex ->
                    ctx.useBatcher { batcher ->
                        batcher.drawQuad(tex, 100f, 0f)
                    }
                }
            }
        }
        assertScreenshot()
    }

}
