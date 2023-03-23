package com.soywiz.korge.view.filter

import com.soywiz.korag.*
import com.soywiz.korag.shader.Program
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.Texture
import com.soywiz.korge.view.BlendMode
import com.soywiz.korge.view.View
import com.soywiz.korge.view.ViewRenderPhase
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
        val bgtex = ctx.tempTexturePool.alloc()
        val width = ctx.currentFrameBufferOrMain.width
        val height = ctx.currentFrameBufferOrMain.height
        ctx.ag.readToTexture(ctx.currentFrameBufferOrMain, bgtex, 0, 0, width, height)
        bgrtex = Texture(bgtex, width, height)
    }

    override fun afterRender(view: View, ctx: RenderContext) {
        bgrtex?.let { ctx.tempTexturePool.free(it.base.base!!) }
        bgrtex = null
    }

    override fun render(view: View, ctx: RenderContext) {
        //println(ctx.ag.frameBufferStack)
        //println("width=$width, height=$height")
        ctx.renderToTexture(bgrtex!!.width, bgrtex!!.height, {
            ctx.useBatcher { batcher ->
                ctx.renderToTexture(bgrtex!!.width, bgrtex!!.height, {
                    batcher.setViewMatrixTemp(view.parent!!.globalMatrixInv) {
                        super.render(view, ctx)
                    }
                }) { mask ->
                    //batcher.flush {
                    batcher.keepTextureUnit(
                        DefaultShaders.TexExUB.SAMPLER_INDEX,
                        flush = true
                    ) {
                        //    ctx[DefaultShaders.TexExUB].push {
                        //        it.set(u_TexEx, mask.base.base)
                        //    }
                        ctx[DefaultShaders.TexExUB].push {
                            it[u_TexEx] = DefaultShaders.TexExUB.SAMPLER_INDEX
                        }
                        ctx.textureUnits.set(DefaultShaders.TexExUB.SAMPLER_INDEX, mask.base.base)

                        //batcher.drawQuad(bgrtex, x = 0f, y = 0f, program = MERGE_ALPHA)
                        batcher.drawQuad(
                            bgrtex!!, x = 0f, y = 0f, m = view.parent!!.globalMatrix, program = DefaultShaders.MERGE_ALPHA_PROGRAM,
                        )
                        //batcher.drawQuad(mask, x = 0f, y = 0f, m = view.globalMatrix, program = MERGE_ALPHA)
                    }
                }
            }
        }) {
            filter.render(
                ctx, view.parent!!.globalMatrix, it, it.width, it.height, Colors.WHITE, BlendMode.NORMAL,
                filter.recommendedFilterScale
            )
        }
    }
}
