package com.soywiz.korag

import com.soywiz.kgl.*
import com.soywiz.korio.annotations.*

@OptIn(KorIncomplete::class, KorInternal::class)
class AGQueueProcessorOpenGL(val gl: KmlGl) : AGQueueProcessor {
    override fun enableDisable(kind: AGEnable, enable: Boolean) {
        gl.enableDisable(when (kind) {
            AGEnable.BLEND -> gl.BLEND
            AGEnable.CULL_FACE -> gl.CULL_FACE
            AGEnable.DEPTH -> gl.DEPTH_TEST
            AGEnable.SCISSOR -> gl.SCISSOR_TEST
            AGEnable.STENCIL -> gl.STENCIL_TEST
        }, enable)
    }

    override fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) {
        gl.colorMask(red, green, blue, alpha)
    }

    override fun blendEquation(rgb: AGBlendEquation, a: AGBlendEquation) {
        gl.blendEquationSeparate(rgb.toGl(), a.toGl())
    }

    override fun blendFunction(srcRgb: AGBlendFactor, dstRgb: AGBlendFactor, srcA: AGBlendFactor, dstA: AGBlendFactor) {
        gl.blendFuncSeparate(srcRgb.toGl(), dstRgb.toGl(), srcA.toGl(), dstA.toGl())
    }

    override fun cullFace(face: AGCullFace) {
        gl.cullFace(when (face) {
            AG.CullFace.BOTH -> gl.FRONT_AND_BACK
            AG.CullFace.FRONT -> gl.FRONT
            AG.CullFace.BACK -> gl.BACK
        })
    }

    override fun depthFunction(depthTest: AGCompareMode) {
        gl.depthFunc(when (depthTest) {
            AG.CompareMode.ALWAYS -> gl.ALWAYS
            AG.CompareMode.EQUAL -> gl.EQUAL
            AG.CompareMode.GREATER -> gl.GREATER
            AG.CompareMode.GREATER_EQUAL -> gl.GEQUAL
            AG.CompareMode.LESS -> gl.LESS
            AG.CompareMode.LESS_EQUAL -> gl.LEQUAL
            AG.CompareMode.NEVER -> gl.NEVER
            AG.CompareMode.NOT_EQUAL -> gl.NOTEQUAL
        })
    }

    private fun AGBlendEquation.toGl(): Int = when (this) {
        AG.BlendEquation.ADD -> gl.FUNC_ADD
        AG.BlendEquation.SUBTRACT -> gl.FUNC_SUBTRACT
        AG.BlendEquation.REVERSE_SUBTRACT -> gl.FUNC_REVERSE_SUBTRACT
    }

    private fun AGBlendFactor.toGl(): Int = when (this) {
        AG.BlendFactor.DESTINATION_ALPHA -> gl.DST_ALPHA
        AG.BlendFactor.DESTINATION_COLOR -> gl.DST_COLOR
        AG.BlendFactor.ONE -> gl.ONE
        AG.BlendFactor.ONE_MINUS_DESTINATION_ALPHA -> gl.ONE_MINUS_DST_ALPHA
        AG.BlendFactor.ONE_MINUS_DESTINATION_COLOR -> gl.ONE_MINUS_DST_COLOR
        AG.BlendFactor.ONE_MINUS_SOURCE_ALPHA -> gl.ONE_MINUS_SRC_ALPHA
        AG.BlendFactor.ONE_MINUS_SOURCE_COLOR -> gl.ONE_MINUS_SRC_COLOR
        AG.BlendFactor.SOURCE_ALPHA -> gl.SRC_ALPHA
        AG.BlendFactor.SOURCE_COLOR -> gl.SRC_COLOR
        AG.BlendFactor.ZERO -> gl.ZERO
    }
}
