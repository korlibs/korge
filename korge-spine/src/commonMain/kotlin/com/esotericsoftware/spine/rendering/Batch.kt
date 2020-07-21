package com.esotericsoftware.spine.rendering

import com.esotericsoftware.spine.graphics.*

interface Batch {
    fun begin() {}
    fun end() {}
    var blendSrcFunc: Int
        set(value) = Unit
        get() = 0
    var blendDstFunc: Int
        set(value) = Unit
        get() = 0
    var blendSrcFuncAlpha: Int
        set(value) = Unit
        get() = 0


    var blendDstFuncAlpha: Int
        set(value) = Unit
        get() = 0


    fun setBlendFunctionSeparate(blendSrc: Int, blendDst: Int, blendSrcAlpha: Int, blendDstAlpha: Int) {}

    fun setBlendFunction(source: Int, dest: Int) {}

    fun draw(texture: Texture?, vertices: FloatArray?, index0: Int, index1: Int) {}
}
