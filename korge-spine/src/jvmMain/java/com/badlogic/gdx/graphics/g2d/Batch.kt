package com.badlogic.gdx.graphics.g2d

import com.badlogic.gdx.graphics.*

interface Batch {
    @JvmDefault
    fun begin() {}
    @JvmDefault
    fun end() {}
    @JvmDefault
    var blendSrcFunc: Int
    @JvmDefault
    var blendDstFunc: Int
    @JvmDefault
    var blendSrcFuncAlpha: Int
    @JvmDefault
    var blendDstFuncAlpha: Int

    @JvmDefault
    fun setBlendFunctionSeparate(blendSrc: Int, blendDst: Int, blendSrcAlpha: Int, blendDstAlpha: Int) {}
    @JvmDefault
    fun setBlendFunction(source: Int, dest: Int) {}
    @JvmDefault
    fun draw(texture: Texture?, vertices: FloatArray?, index0: Int, index1: Int) {}
}
