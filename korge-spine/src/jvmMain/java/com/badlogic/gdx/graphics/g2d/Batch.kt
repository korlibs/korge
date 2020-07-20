package com.badlogic.gdx.graphics.g2d

import com.badlogic.gdx.graphics.*

interface Batch {
    @JvmDefault
    fun begin() {}
    @JvmDefault
    fun end() {}
    @JvmDefault
    val blendSrcFunc: Int
        get() = 0
    @JvmDefault
    val blendDstFunc: Int
        get() = 0
    @JvmDefault
    val blendSrcFuncAlpha: Int
        get() = 0
    @JvmDefault
    val blendDstFuncAlpha: Int
        get() = 0

    @JvmDefault
    fun setBlendFunctionSeparate(blendSrc: Int, blendDst: Int, blendSrcAlpha: Int, blendDstAlpha: Int) {}
    @JvmDefault
    fun setBlendFunction(source: Int, dest: Int) {}
    @JvmDefault
    fun draw(texture: Texture?, vertices: FloatArray?, index0: Int, index1: Int) {}
}
