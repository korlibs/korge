package korlibs.korge.view.filter

import korlibs.graphics.*
import korlibs.graphics.shader.Program
import korlibs.korge.render.RenderContext
import korlibs.korge.render.Texture
import korlibs.korge.view.BlendMode
import korlibs.korge.view.View
import korlibs.korge.view.ViewRenderPhase
import korlibs.image.color.Colors

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
                    batcher.temporalTextureUnit2(DefaultShaders.u_Tex, bgrtex?.base?.base, DefaultShaders.u_TexEx, mask.base.base) {
                        batcher.drawQuad(
                            bgrtex!!, x = 0f, y = 0f, m = view.parent!!.globalMatrix, program = DefaultShaders.MERGE_ALPHA_PROGRAM,
                        )
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
