package com.soywiz.korge.render

import com.soywiz.korag.*

object MaskStates {
    class RenderState(val stencilOpFunc: AGStencilOpFunc, val stencilRef: AGStencilReference, val colorMask: AGColorMask) {
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
        AGStencilOpFunc.DEFAULT,
        AGStencilReference.DEFAULT,
        AGColorMask.ALL_ENABLED,
    )
    val STATE_SHAPE = RenderState(
        AGStencilOpFunc.DEFAULT.withEnabled(true).withCompareMode(AGCompareMode.ALWAYS).withAction(AGStencilOp.SET),
        AGStencilReference.DEFAULT.withReferenceValue(0).withReadMask(0x00).withWriteMask(0xFF),
        AGColorMask.ALL_DISABLED
    )
    val STATE_CONTENT = RenderState(
        AGStencilOpFunc.DEFAULT.withEnabled(true).withCompareMode(AGCompareMode.EQUAL).withAction(AGStencilOp.KEEP),
        AGStencilReference.DEFAULT.withReferenceValue(0).withReadMask(0xFF).withWriteMask(0x00),
        AGColorMask.ALL_ENABLED
    )
}
