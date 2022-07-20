package samples

import com.soywiz.klock.seconds
import com.soywiz.korge.animate.animate
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.tween.get
import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korge.view.mask.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.paint.LinearGradientPaint
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korma.geom.shape.buildPath
import com.soywiz.korma.geom.vector.circle
import com.soywiz.korma.interpolation.Easing

class MainMasks : Scene() {
    override suspend fun SContainer.sceneMain() {
        filter = IdentityFilter
        scale = 0.9
        //y -= 32.0

        solidRect(width, height, Colors.GREEN)

        val fill1 = LinearGradientPaint(0, 0, 200, 200).add(0.0, Colors.RED).add(1.0, Colors.BLUE)
        var maskView = circle(50.0).xy(50, 50).visible(false)
        val circle1 = circle(100.0, fill = fill1)
            //val circle1 = solidRect(200, 200, Colors.PURPLE)
            .filters(DropshadowFilter())
            //.filters(BlurFilter())
            //.filters(ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX))
            .mask(maskView)
        //.mask(solidRect(100, 100, Colors.WHITE).xy(50, 50).visible(false))

        roundRect(100, 100, 16, 16).xy(15, 15)
            .backdropFilters(ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX))
            .backdropFilters(BlurFilter())

        /*
        addChild(BackdropMaskView().apply {
            //roundRect(100, 100, 16, 16).xy(10, 10)
            //roundRect(100, 100, 16, 16).xy(0, 0)
            roundRect(100, 100, 16, 16).xy(50, 50)
            //solidRect(100, 100, Colors.WHITE).xy(10, 10)
            //addChild(Circle(50.0, Colors.GREEN))
            filters(ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX))
            //filters(BlurFilter(radius = 4.0, expandBorder = false))
        })
        */

        val circle3 = circle(100.0, fill = fill1).centered
        launchImmediately {
            val width = this.width
            val height = this.height
            val path = buildPath { this.circle(width * 0.5, height * 0.5, 300.0) }
            animate(looped = true) {
                tween(circle3::pos[path], time = 2.seconds, easing = Easing.LINEAR)
            }
        }


        launchImmediately {
            animate(looped = true) {
                //parallel {
                tween(maskView::radius[150.0], time = 1.seconds)
                tween(maskView::radius[10.0], time = 1.seconds)
                //}
            }
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

    /*
    // @TODO: Mask proper region
    class BackdropMaskView : Container() {
        init {
            addRenderPhase(object : ViewRenderPhase {
                override val priority: Int
                    get() = +100000

                var bgrtex: Texture? = null

                override fun beforeRender(view: View, ctx: RenderContext) {
                    val bgtex = ctx.ag.tempTexturePool.alloc()
                    val width = ctx.ag.currentRenderBufferOrMain.width
                    val height = ctx.ag.currentRenderBufferOrMain.height
                    ctx.ag.readColorTexture(bgtex, 0, 0, width, height)
                    bgrtex = Texture(bgtex, width, height)
                }

                override fun afterRender(view: View, ctx: RenderContext) {
                    bgrtex?.let { ctx.ag.tempTexturePool.free(it.base.base!!) }
                    bgrtex = null
                }

                override fun render(view: View, ctx: RenderContext) {
                    //println(ctx.ag.renderBufferStack)
                    //println("width=$width, height=$height")
                    ctx.useBatcher { batcher ->
                        ctx.renderToTexture(bgrtex!!.width, bgrtex!!.height, {
                            //println(ctx.batch.viewMat2D)
                            batcher.setViewMatrixTemp(Matrix()) {
                                super.render(view, ctx)
                            }
                        }) { mask ->
                            batcher.setTemporalUniform(
                                DefaultShaders.u_Tex2,
                                AG.TextureUnit(mask.base.base),
                                flush = true
                            ) {
                                //batcher.drawQuad(bgrtex, x = 0f, y = 0f, program = MERGE_ALPHA)
                                batcher.drawQuad(bgrtex!!, x = 0f, y = 0f, m = view.globalMatrix, program = MERGE_ALPHA)
                                //batcher.drawQuad(mask, x = 0f, y = 0f, m = view.globalMatrix, program = MERGE_ALPHA)
                            }
                        }
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
    */

}
