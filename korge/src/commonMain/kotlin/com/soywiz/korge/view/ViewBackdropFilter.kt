package com.soywiz.korge.view

import com.soywiz.korag.AG
import com.soywiz.korag.DefaultShaders
import com.soywiz.korag.FragmentShaderDefault
import com.soywiz.korag.shader.Program
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.Texture
import com.soywiz.korge.view.filter.ComposedFilter
import com.soywiz.korge.view.filter.Filter
import com.soywiz.korim.color.ColorAdd
import com.soywiz.korim.color.Colors

// @TODO: WIP. We should only read/render/clip the masked AABB area
var View.backdropFilter: Filter?
    get() = getRenderPhaseOfTypeOrNull<ViewRenderPhaseBackdropFilter>()?.filter
    set(value) {
        if (value != null) {
            getOrCreateAndAddRenderPhase { ViewRenderPhaseBackdropFilter(value) }.filter = value
        } else {
            removeRenderPhaseOfType<ViewRenderPhaseBackdropFilter>()
        }
    }

fun <T : View> T.backdropFilters(vararg filter: Filter?): T {
    this.backdropFilter = ComposedFilter.combine(this.backdropFilter, ComposedFilter(filter.filterNotNull()))
    return this
}

// @TODO: Only render the bounds of the view
class ViewRenderPhaseBackdropFilter(var filter: Filter) : ViewRenderPhase {
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
        ctx.renderToTexture(bgrtex!!.width, bgrtex!!.height, {
            ctx.useBatcher { batcher ->
                ctx.renderToTexture(bgrtex!!.width, bgrtex!!.height, {
                    batcher.setViewMatrixTemp(view.parent!!.globalMatrixInv) {
                        super.render(view, ctx)
                    }
                }) { mask ->
                    batcher.setTemporalUniform(
                        DefaultShaders.u_Tex2,
                        AG.TextureUnit(mask.base.base),
                        flush = true
                    ) {
                        //batcher.drawQuad(bgrtex, x = 0f, y = 0f, program = MERGE_ALPHA)
                        batcher.drawQuad(bgrtex!!, x = 0f, y = 0f, m = view.parent!!.globalMatrix, program = MERGE_ALPHA)
                        //batcher.drawQuad(mask, x = 0f, y = 0f, m = view.globalMatrix, program = MERGE_ALPHA)
                    }
                }
            }
        }) {
            filter.render(ctx, view.parent!!.globalMatrix, it, it.width, it.height, ColorAdd.NEUTRAL, Colors.WHITE, BlendMode.NORMAL, filter.recommendedFilterScale)
        }
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
