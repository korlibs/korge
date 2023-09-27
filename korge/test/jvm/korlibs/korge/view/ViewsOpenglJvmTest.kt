package korlibs.korge.view

import korlibs.graphics.*
import korlibs.graphics.gl.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.kgl.*
import korlibs.korge.render.*
import korlibs.korge.testing.*
import korlibs.korge.tests.*
import korlibs.korge.view.filter.*
import korlibs.math.geom.*
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
    fun testIdentityFilterFor128x128() = korgeScreenshotTest(Size(200, 200)) {
        views.stage += Image(Bitmap32(102, 102, Colors.RED.premultiplied)).also {
            it.filter = IdentityFilter
        }
        assertScreenshot()
    }

    @Test
    fun testRenderToTextureWithStencil() = korgeScreenshotTest(Size(512, 512)) {
        this += object : View() {
            override fun renderInternal(ctx: RenderContext) {
                ctx.renderToTexture(100, 100, render = {
                   ctx.useCtx2d { ctx2d ->
                       ctx2d.rect(Rectangle(0.0, 0.0, 100.0, 100.0), Colors.RED)
                   }
                }, hasStencil = true) { tex ->
                    ctx.useBatcher { batcher ->
                        batcher.drawQuad(tex)
                    }
                }
                ctx.renderToTexture(100, 100, render = {
                    ctx.useCtx2d { ctx2d ->
                        ctx2d.rect(Rectangle(0.0, 0.0, 100.0, 100.0), Colors.BLUE)
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
