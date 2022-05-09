import com.soywiz.klock.seconds
import com.soywiz.korag.AG
import com.soywiz.korag.DefaultShaders
import com.soywiz.korag.FragmentShaderDefault
import com.soywiz.korag.shader.Program
import com.soywiz.korge.animate.animate
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.Texture
import com.soywiz.korge.tween.get
import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.paint.LinearGradientPaint
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korma.geom.shape.buildPath
import com.soywiz.korma.geom.vector.circle
import com.soywiz.korma.interpolation.Easing

suspend fun Stage.mainMasks() {
    filter = IdentityFilter

    solidRect(width, height, Colors.GREEN)

    val fill1 = LinearGradientPaint(0, 0, 200, 200).add(0.0, Colors.RED).add(1.0, Colors.BLUE)
    var maskView = circle(50.0).xy(50, 50).visible(false)
    val circle1 = circle(100.0, fill = fill1)
        //val circle1 = solidRect(200, 200, Colors.PURPLE)
        .filters(DropshadowFilter())
        //r.filters(BlurFilter())
        //.filters(ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX))
        .mask(maskView)
    //.mask(solidRect(100, 100, Colors.WHITE).xy(50, 50).visible(false))

    val circle3 = circle(100.0, fill = fill1).centered
    launchImmediately {
        val path = buildPath { this.circle(width * 0.5, height * 0.5, 300.0) }
        animate(looped = true) {
            tween(circle3::pos[path], time = 2.seconds, easing = Easing.LINEAR)
        }
    }

    animate(looped = true) {
        //parallel {
            tween(maskView::radius[150.0], time = 1.seconds)
            tween(maskView::radius[10.0], time = 1.seconds)
        //}
    }


    //.center()
    //.filters(ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX))
    //.filters(BlurFilter(10.0, expandBorder = false))
    //.filters(IdentityFilter)

    //val bmp = NativeImage(800, 800).context2d {
    //    this.fill(fill1) {
    //        this.circle(50 * 2, 50 * 2, 100 * 2)
    //    }
    //}

    /*
    addChild(BackgroundMaskView().apply {
        //roundRect(200, 100, 16, 16).xy(100, 100)
        solidRect(100, 100, Colors.WHITE).xy(80, 80)
        //addChild(Circle(50.0, Colors.GREEN))
    }
        //.filters(ColorMatrixFilter(ColorMatrixFilter.SEPIA_MATRIX))
        //.filters(BlurFilter(radius = 4.0, expandBorder = false))
        //.filters(BlurFilter(radius = 10.0, expandBorder = true))
        //.filters(WaveFilter())
    )
    */


    /*
    gpuShapeView({
        fill(GpuBackgroundPaint) {
        //fill(BitmapPaint(bmp)) {
        //fill(Colors.WHITE) {
            rect(50, 50, 100, 100)
        }
        //fill(Colors.WHITE) {
        //    rect(50, 250, 400, 200)
        //}
    })
        //.filters(ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX))
        //.filters(BlurFilter(10.0))
        .filters(WaveFilter())

     */
}

// @TODO: Mask proper region
class BackgroundMaskView : Container() {
    init {
        addRenderPhase(object : ViewRenderPhase {
            override fun render(view: View, ctx: RenderContext) {
                ctx.flush()
                val bgtex = ctx.ag.tempTexturePool.alloc()
                ctx.ag.readColorTexture(bgtex, ctx.ag.currentWidth, ctx.ag.currentHeight)
                val bgrtex = Texture(bgtex, ctx.ag.currentWidth, ctx.ag.currentHeight)
                try {
                    ctx.renderToTexture(bgrtex.width, bgrtex.height, {
                        super.render(view, ctx)
                    }) { mask ->
                        ctx.useBatcher { batcher ->
                            batcher.setTemporalUniform(
                                DefaultShaders.u_Tex2,
                                AG.TextureUnit(mask.base.base),
                                flush = true
                            ) {
                                batcher.drawQuad(bgrtex, x = 0f, y = 0f, program = MERGE_ALPHA)
                            }
                        }
                    }
                } finally {
                    ctx.ag.tempTexturePool.free(bgrtex.base.base!!)
                }
            }
        })
    }

    companion object {
        val MERGE_ALPHA = Program(DefaultShaders.VERTEX_DEFAULT, FragmentShaderDefault {
            val coords = v_Tex["xy"]
            SET(t_Temp0, texture2D(u_Tex, coords))
            SET(t_Temp1, texture2D(u_Tex2, coords))
            SET(out, vec4(t_Temp0["rgb"], t_Temp0["a"] * t_Temp1["a"]))
        })
    }
}
