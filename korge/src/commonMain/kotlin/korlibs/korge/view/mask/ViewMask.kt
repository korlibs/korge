package korlibs.korge.view.mask

import korlibs.datastructure.*
import korlibs.graphics.*
import korlibs.graphics.annotation.*
import korlibs.image.color.*
import korlibs.korge.render.*
import korlibs.korge.view.*

private var View.__mask: View? by extraProperty { null }
private var View.__maskFiltering: Boolean by extraProperty { true }

fun <T : View> T.mask(mask: View?, filtering: Boolean = true): T {
    this.maskFiltering = filtering
    this.mask = mask
    return this
}

var View.maskFiltering: Boolean
    get() = __maskFiltering
    set(value) {
        __maskFiltering = value
        updatedMask()
    }
var View.mask: View?
    get() = __mask
    set(value) {
        __mask = value
        updatedMask()
    }

private fun View.updatedMask() {
    val value = __mask
    removeRenderPhaseOfType<ViewRenderPhaseMask>()
    if (value != null) {
        addRenderPhase(ViewRenderPhaseMask(value, __maskFiltering))
    }
}

@OptIn(KoragExperimental::class)
class ViewRenderPhaseMask(var mask: View, var maskFiltering: Boolean) : ViewRenderPhase {
    companion object {
        const val PRIORITY = -100
    }

    override val priority: Int get() = PRIORITY
    override fun render(view: View, ctx: RenderContext) {
        ctx.useBatcher { batcher ->
            val maskBounds = mask.getLocalBounds()
            val boundsWidth = maskBounds.width.toInt()
            val boundsHeight = maskBounds.height.toInt()
            ctx.tempAllocateFrameBuffers2(boundsWidth, boundsHeight) { maskFB, viewFB ->
                batcher.setViewMatrixTemp(mask.globalMatrixInv) {
                    ctx.renderToFrameBuffer(maskFB) {
                        ctx.clear(color = Colors.TRANSPARENT)
                        val oldVisible = mask.visible
                        try {
                            mask.visible = true
                            mask.renderFirstPhase(ctx)
                        } finally {
                            mask.visible = oldVisible
                        }
                    }
                    ctx.renderToFrameBuffer(viewFB) {
                        ctx.clear(color = Colors.TRANSPARENT)
                        view.renderNextPhase(ctx)
                    }
                }
                //batcher.drawQuad(Texture(maskFB), 100f, 200f, m = view.parent!!.globalMatrix)
                //batcher.drawQuad(Texture(viewFB), 300f, 200f, m = view.parent!!.globalMatrix)
                batcher.temporalTextureUnit(DefaultShaders.u_Tex, viewFB.tex, DefaultShaders.u_TexEx, maskFB.tex) {
                    batcher.drawQuad(
                        Texture(viewFB), m = mask.globalMatrix, program = DefaultShaders.MERGE_ALPHA_PROGRAM, filtering = maskFiltering
                    )
                    //batcher.createBatchIfRequired()
                }
            }
        }
        view.renderNextPhase(ctx)
    }
}
