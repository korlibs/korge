package com.soywiz.korge.render

import com.soywiz.korag.*

object MaskStates {
    class RenderState(val stencilOpFunc: AGStencilOpFuncState, val stencilRef: AGStencilReferenceState, val colorMask: AGColorMaskState) {
        val stencilFull get() = AGStencilFullState(stencilOpFunc, stencilRef)

        @Suppress("DEPRECATION")
        fun set(ctx: RenderContext, referenceValue: Int) {
            ctx.flush()
            if (ctx.masksEnabled) {
                ctx.batch.stencilOpFunc = stencilOpFunc
                ctx.batch.stencilRef = stencilRef.withReferenceValue(referenceValue)
                ctx.batch.colorMask = colorMask
            } else {
                ctx.batch.stencilOpFunc = STATE_NONE.stencilOpFunc
                ctx.batch.stencilRef = STATE_NONE.stencilRef
                ctx.batch.colorMask = STATE_NONE.colorMask
            }
        }
    }

    val STATE_NONE = RenderState(
        AGStencilOpFuncState.DEFAULT,
        AGStencilReferenceState.DEFAULT,
        AGColorMaskState.ALL_ENABLED,
    )
    val STATE_SHAPE = RenderState(
        AGStencilOpFuncState.DEFAULT.withEnabled(true).withCompareMode(AGCompareMode.ALWAYS).withAction(AGStencilOp.SET),
        AGStencilReferenceState.DEFAULT.withReferenceValue(0).withReadMask(0x00).withWriteMask(0xFF),
        AGColorMaskState.ALL_DISABLED
    )
    val STATE_CONTENT = RenderState(
        AGStencilOpFuncState.DEFAULT.withEnabled(true).withCompareMode(AGCompareMode.EQUAL).withAction(AGStencilOp.KEEP),
        AGStencilReferenceState.DEFAULT.withReferenceValue(0).withReadMask(0xFF).withWriteMask(0x00),
        AGColorMaskState.ALL_ENABLED
    )
}
