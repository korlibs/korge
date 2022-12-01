package com.soywiz.korge.render

import com.soywiz.korag.AG

object MaskStates {
    class RenderState(val stencilOpFunc: AG.StencilOpFuncState, val stencilRef: AG.StencilReferenceState, val colorMask: AG.ColorMaskState) {
        val stencilFull get() = AG.StencilFullState(stencilOpFunc, stencilRef)

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
        AG.StencilOpFuncState.DEFAULT,
        AG.StencilReferenceState.DEFAULT,
        AG.ColorMaskState.ALL_ENABLED,
    )
    val STATE_SHAPE = RenderState(
        AG.StencilOpFuncState.DEFAULT.withEnabled(true).withCompareMode(AG.CompareMode.ALWAYS).withAction(AG.StencilOp.SET),
        AG.StencilReferenceState.DEFAULT.withReferenceValue(0).withReadMask(0x00).withWriteMask(0xFF),
        AG.ColorMaskState.ALL_DISABLED
    )
    val STATE_CONTENT = RenderState(
        AG.StencilOpFuncState.DEFAULT.withEnabled(true).withCompareMode(AG.CompareMode.EQUAL).withAction(AG.StencilOp.KEEP),
        AG.StencilReferenceState.DEFAULT.withReferenceValue(0).withReadMask(0xFF).withWriteMask(0x00),
        AG.ColorMaskState.ALL_ENABLED
    )
}
