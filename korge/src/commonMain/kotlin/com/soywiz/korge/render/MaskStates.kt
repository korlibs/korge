package com.soywiz.korge.render

import com.soywiz.korag.*

object MaskStates {
    class LocalRenderState {
        val stencil = AG.StencilState()
    }

    class RenderState(val stencil: AG.StencilState, val colorMask: AG.ColorMaskState) {
        fun set(ctx: RenderContext, referenceValue: Int, temp: LocalRenderState) {
            ctx.flush()
            if (ctx.masksEnabled) {
                temp.stencil.copyFrom(stencil)
                temp.stencil.referenceValue = referenceValue
                ctx.batch.stencil = temp.stencil
                ctx.batch.colorMask = colorMask
            } else {
                ctx.batch.stencil = STATE_NONE.stencil
                ctx.batch.colorMask = STATE_NONE.colorMask
            }
        }
    }

    val STATE_NONE = RenderState(
        AG.StencilState(enabled = false),
        AG.ColorMaskState(true, true, true, true)
    )
    val STATE_SHAPE = RenderState(
        AG.StencilState(
            enabled = true,
            compareMode = AG.CompareMode.ALWAYS,
            actionOnBothPass = AG.StencilOp.SET,
            actionOnDepthFail = AG.StencilOp.SET,
            actionOnDepthPassStencilFail = AG.StencilOp.SET,
            referenceValue = 0,
            readMask = 0x00,
            writeMask = 0xFF
        ),
        AG.ColorMaskState(false, false, false, false)
    )
    val STATE_CONTENT = RenderState(
        AG.StencilState(
            enabled = true,
            compareMode = AG.CompareMode.EQUAL,
            actionOnBothPass = AG.StencilOp.KEEP,
            actionOnDepthFail = AG.StencilOp.KEEP,
            actionOnDepthPassStencilFail = AG.StencilOp.KEEP,
            referenceValue = 0,
            readMask = 0xFF,
            writeMask = 0x00
        ),
        AG.ColorMaskState(true, true, true, true)
    )
}
