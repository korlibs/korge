package com.soywiz.korge.view

import com.soywiz.korge.render.*

class MaskedView : Container() {
    var mask: View? = null
        set(value) {
            if (field != null) {
                removeChild(value)
            }
            field = value
            if (value != null) {
                addChild(value)
            }
        }

    private val tempLocalRenderState = MaskStates.LocalRenderState()
    override fun renderInternal(ctx: RenderContext) {
        if (!visible) return
        val useMask = mask != null
        try {
            if (useMask) {
                ctx.flush()
                ctx.stencilIndex++
                setMaskState(ctx, MaskStates.STATE_SHAPE)
                mask?.render(ctx)
                ctx.flush()
                setMaskState(ctx, MaskStates.STATE_CONTENT)
            }

            forEachChildren { child ->
                if (child != mask) {
                    child.render(ctx)
                }
            }
        } finally {
            if (useMask) {
                ctx.flush()
                ctx.stencilIndex--

                if (ctx.stencilIndex <= 0) {
                    setMaskState(ctx, MaskStates.STATE_NONE)
                    //println("ctx.stencilIndex: ${ctx.stencilIndex}")
                    ctx.stencilIndex = 0
                    ctx.ag.clear(clearColor = false, clearDepth = false, clearStencil = true, stencil = ctx.stencilIndex)
                }
            }
        }
    }

    private fun setMaskState(ctx: RenderContext, state: MaskStates.RenderState) {
        state.set(ctx, ctx.stencilIndex, tempLocalRenderState)
    }
}
