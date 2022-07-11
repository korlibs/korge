package com.soywiz.korge.view.mask

import com.soywiz.kds.extraProperty
import com.soywiz.korag.AG
import com.soywiz.korag.DefaultShaders
import com.soywiz.korag.FragmentShaderDefault
import com.soywiz.korag.annotation.KoragExperimental
import com.soywiz.korag.shader.Program
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.Texture
import com.soywiz.korge.view.View
import com.soywiz.korge.view.ViewRenderPhase
import com.soywiz.korim.color.Colors
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
private var View.__mask: View? by extraProperty { null }

fun <T : View> T.mask(mask: View?): T {
    this.mask = mask
    return this
}

var View.mask: View?
    get() = __mask
    set(value) {
        removeRenderPhaseOfType<ViewRenderPhaseMask>()
        if (value != null) {
            addRenderPhase(ViewRenderPhaseMask(value))
        }
        __mask = value
    }

@OptIn(KoragExperimental::class)
class ViewRenderPhaseMask(var mask: View) : ViewRenderPhase {
    companion object {
        const val PRIORITY = -100

        // @TODO: Review edge alpha issues
        val MERGE_ALPHA_PROGRAM = Program(DefaultShaders.VERTEX_DEFAULT, FragmentShaderDefault {
            val coords = v_Tex["xy"]
            SET(t_Temp0, texture2D(u_Tex, coords))
            SET(t_Temp1["a"], texture2D(u_Tex2, coords)["a"])
            SET(t_Temp1["x"], t_Temp0["a"] * t_Temp1["a"])
            SET(out, vec4(t_Temp0["rgb"] * t_Temp1["a"], t_Temp1["x"]))
        })
    }

    override val priority: Int get() = PRIORITY
    override fun render(view: View, ctx: RenderContext) {
        ctx.useBatcher { batcher ->
            val maskBounds = mask.getLocalBoundsOptimized()
            val boundsWidth = maskBounds.width.toInt()
            val boundsHeight = maskBounds.height.toInt()
            ctx.ag.tempAllocateFrameBuffers2(boundsWidth, boundsHeight) { maskFB, viewFB ->
                batcher.setViewMatrixTemp(mask.globalMatrixInv) {
                    ctx.renderToFrameBuffer(maskFB) {
                        ctx.ag.clear(color = Colors.TRANSPARENT_BLACK)
                        val oldVisible = mask.visible
                        try {
                            mask.visible = true
                            mask.renderFirstPhase(ctx)
                        } finally {
                            mask.visible = oldVisible
                        }
                    }
                    ctx.renderToFrameBuffer(viewFB) {
                        ctx.ag.clear(color = Colors.TRANSPARENT_BLACK)
                        view.renderNextPhase(ctx)
                    }
                }
                //batcher.drawQuad(Texture(maskFB), 100f, 200f, m = view.parent!!.globalMatrix)
                //batcher.drawQuad(Texture(viewFB), 300f, 200f, m = view.parent!!.globalMatrix)
                batcher.setTemporalUniform(DefaultShaders.u_Tex2, AG.TextureUnit(maskFB.tex), flush = true) {
                    batcher.drawQuad(Texture(viewFB), m = mask.globalMatrix, program = MERGE_ALPHA_PROGRAM)
                }
            }
        }
        view.renderNextPhase(ctx)
    }
}
