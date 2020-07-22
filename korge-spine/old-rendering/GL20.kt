package com.esotericsoftware.spine.rendering

interface GL20 {
    fun glEnable(glBlend: Int)
    fun glBlendFunc(srcFunc: Int, glOneMinusSrcAlpha: Int)
    fun glDepthMask(b: Boolean)
    fun glDisable(glBlend: Int)
    fun glBlendFuncSeparate(blendSrcFunc: Int, blendDstFunc: Int, blendSrcFuncAlpha: Int, blendDstFuncAlpha: Int)

    companion object {
        const val GL_SRC_ALPHA = 0
        const val GL_ONE = 1
        const val GL_ONE_MINUS_SRC_ALPHA = 2
        const val GL_DST_COLOR = 3
        const val GL_ONE_MINUS_SRC_COLOR = 4
        const val GL_POINTS = 0
        const val GL_LINES = 1
        const val GL_TRIANGLES = 2
        const val GL_BLEND = 0
    }
}
