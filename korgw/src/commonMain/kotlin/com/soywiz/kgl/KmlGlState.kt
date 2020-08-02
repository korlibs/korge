package com.soywiz.kgl

import com.soywiz.kmem.*

class KmlGlState(val gl: KmlGl) {
    val enabledList = listOf(
        gl.BLEND, gl.CULL_FACE, gl.DEPTH_TEST, gl.DITHER,
        gl.POLYGON_OFFSET_FILL, gl.SAMPLE_ALPHA_TO_COVERAGE, gl.SAMPLE_COVERAGE,
        gl.SCISSOR_TEST, gl.STENCIL_TEST,
    )
    val enabledArray = BooleanArray(enabledList.size)
    val scissor = FBuffer(4 * 4)
    val viewport = FBuffer(4 * 4)
    val clearColor = FBuffer(4 * 4)
    val clearDepth = FBuffer(4)
    val clearStencil = FBuffer(4)
    val stencilFrontWriteMask = FBuffer(4)
    val stencilBackWriteMask = FBuffer(4)
    val textureBinding2D = FBuffer(4)
    val currentProgram = FBuffer(4)

    fun save() {
        saveEnable()
        gl.getIntegerv(gl.SCISSOR_BOX, scissor)
        gl.getIntegerv(gl.VIEWPORT, viewport)
        gl.getFloatv(gl.COLOR_CLEAR_VALUE, clearColor)
        gl.getFloatv(gl.DEPTH_CLEAR_VALUE, clearDepth)
        gl.getIntegerv(gl.STENCIL_CLEAR_VALUE, clearStencil)
        gl.getIntegerv(gl.STENCIL_WRITEMASK, stencilFrontWriteMask)
        gl.getIntegerv(gl.STENCIL_BACK_WRITEMASK, stencilBackWriteMask)
        gl.getIntegerv(gl.TEXTURE_BINDING_2D, textureBinding2D)
        gl.getIntegerv(gl.CURRENT_PROGRAM, currentProgram)
    }

    fun saveEnable() = run { for (n in enabledList.indices) enabledArray[n] = gl.isEnabled(enabledList[n]) }

    fun restore() {
        restoreEnable()
        gl.scissor(scissor.i32[0], scissor.i32[1], scissor.i32[2], scissor.i32[3])
        gl.viewport(viewport.i32[0], viewport.i32[1], viewport.i32[2], viewport.i32[3])
        //gl.disable(gl.SCISSOR_TEST)
        gl.clearColor(clearColor.f32[0], clearColor.f32[1], clearColor.f32[2], clearColor.f32[3])
        gl.clearDepthf(clearDepth.f32[0])
        gl.stencilMaskSeparate(gl.FRONT, stencilFrontWriteMask.i32[0])
        gl.stencilMaskSeparate(gl.BACK, stencilFrontWriteMask.i32[0])
        gl.clearStencil(clearStencil.i32[0])
        gl.bindTexture(gl.TEXTURE_2D, textureBinding2D.i32[0])
        gl.useProgram(currentProgram.i32[0])

        //println("restored!")
    }

    fun restoreEnable() = run { for (n in enabledList.indices) gl.enableDisable(enabledList[n], enabledArray[n]) }

    inline fun keep(block: () -> Unit): Unit {
        save()
        try {
            block()
        } finally {
            restore()
        }
    }

    /*
    inline fun <T> keep(block: () -> T): T {
        save()
        try {
            return block()
        } finally {
            restore()
        }
    }

     */
}
