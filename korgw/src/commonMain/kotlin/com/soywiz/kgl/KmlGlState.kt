package com.soywiz.kgl

import com.soywiz.kmem.*
import com.soywiz.korag.*

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
    val activeTexture = FBuffer(4)
    val currentProgram = FBuffer(4)

    val MAX_ATTRIB by lazy { gl.getIntegerv(gl.MAX_VERTEX_ATTRIBS) }
    val vertexAttribEnabled by lazy { BooleanArray(MAX_ATTRIB) }
    val vertexAttribSize by lazy { Array(MAX_ATTRIB) { FBuffer(8) } }
    val vertexAttribType by lazy { Array(MAX_ATTRIB) { FBuffer(8) } }
    val vertexAttribNormal by lazy { Array(MAX_ATTRIB) { FBuffer(8) } }
    val vertexAttribStride by lazy { Array(MAX_ATTRIB) { FBuffer(8) } }
    val vertexAttribPointer by lazy { Array(MAX_ATTRIB) { FBuffer(8) } }

    val MAX_TEX_UNITS by lazy { gl.getIntegerv(gl.MAX_TEXTURE_IMAGE_UNITS) }
    val textureBinding2DList by lazy { Array(MAX_TEX_UNITS) { FBuffer(8) } }

    private val temp = FBuffer(64)
    private var arrayBufferBinding: Int = 0
    private var elementArrayBufferBinding: Int = 0

    fun save() {
        saveEnable()
        gl.getIntegerv(gl.SCISSOR_BOX, scissor)
        gl.getIntegerv(gl.VIEWPORT, viewport)
        gl.getFloatv(gl.COLOR_CLEAR_VALUE, clearColor)
        gl.getFloatv(gl.DEPTH_CLEAR_VALUE, clearDepth)
        gl.getIntegerv(gl.STENCIL_CLEAR_VALUE, clearStencil)
        gl.getIntegerv(gl.STENCIL_WRITEMASK, stencilFrontWriteMask)
        gl.getIntegerv(gl.STENCIL_BACK_WRITEMASK, stencilBackWriteMask)

        // Textures
        run {
            gl.getIntegerv(gl.ACTIVE_TEXTURE, activeTexture)
            for (n in 0 until MAX_TEX_UNITS) {
                gl.activeTexture(gl.TEXTURE0 + n)
                gl.getIntegerv(gl.TEXTURE_BINDING_2D, textureBinding2DList[n])
            }
        }

        gl.getIntegerv(gl.CURRENT_PROGRAM, currentProgram)
        //if (currentProgram.getAlignedInt32(0) > 0) {
        run {
            //println("maxAttribs: $maxAttribs")
            for (n in 0 until MAX_ATTRIB) {
                //println(gl.getVertexAttribiv(n, gl.VERTEX_ATTRIB_ARRAY_ENABLED))
                vertexAttribEnabled[n] = gl.getVertexAttribiv(n, gl.VERTEX_ATTRIB_ARRAY_ENABLED) != 0
                if (vertexAttribEnabled[n]) {
                    //println("$n: ${vertexAttribEnabled[n]}")
                    gl.getVertexAttribiv(n, gl.VERTEX_ATTRIB_ARRAY_SIZE, vertexAttribSize[n])
                    gl.getVertexAttribiv(n, gl.VERTEX_ATTRIB_ARRAY_TYPE, vertexAttribType[n])
                    gl.getVertexAttribiv(n, gl.VERTEX_ATTRIB_ARRAY_NORMALIZED, vertexAttribNormal[n])
                    gl.getVertexAttribiv(n, gl.VERTEX_ATTRIB_ARRAY_STRIDE, vertexAttribStride[n])
                    gl.getVertexAttribPointerv(n, gl.VERTEX_ATTRIB_ARRAY_POINTER, vertexAttribPointer[n])
                }
            }
            arrayBufferBinding = gl.getIntegerv(gl.ARRAY_BUFFER_BINDING)
            elementArrayBufferBinding = gl.getIntegerv(gl.ELEMENT_ARRAY_BUFFER_BINDING)
        }
        gl.getError() // Clears the error
    }

    fun saveEnable() { for (n in enabledList.indices) enabledArray[n] = gl.isEnabled(enabledList[n]) }

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

        // Textures
        run {
            for (n in 0 until MAX_TEX_UNITS) {
                gl.activeTexture(gl.TEXTURE0 + n)
                gl.bindTexture(gl.TEXTURE_2D, textureBinding2DList[n].i32[0])
            }
            gl.activeTexture(activeTexture.i32[0])
        }

        gl.useProgram(currentProgram.i32[0])
        //gl.bindAttribLocation()
        //if (currentProgram.getAlignedInt32(0) > 0) {
        run {
            for (n in 0 until MAX_ATTRIB) {
                gl.enableDisableVertexAttribArray(n, vertexAttribEnabled[n])
                if (vertexAttribEnabled[n]) {
                    val ptr = vertexAttribPointer[n].getAlignedInt64(0)
                    gl.vertexAttribPointer(
                        n,
                        vertexAttribSize[n].i32[0],
                        vertexAttribType[n].i32[0],
                        vertexAttribNormal[n].i32[0] != 0,
                        vertexAttribStride[n].i32[0],
                        ptr
                    )
                }
            }
        }
        gl.bindBuffer(gl.ARRAY_BUFFER, arrayBufferBinding)
        gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, elementArrayBufferBinding)
        gl.getError() // Clears the error

        /*
        gl.blendEquationSeparate(blending.eqRGB.toGl(), blending.eqA.toGl())
        gl.blendFuncSeparate(
            blending.srcRGB.toGl(), blending.dstRGB.toGl(),
            blending.srcA.toGl(), blending.dstA.toGl()
        )
        gl.frontFace(if (renderState.frontFace == AG.FrontFace.CW) gl.CW else gl.CCW)
        gl.depthRangef(renderState.depthNear, renderState.depthFar)
        gl.lineWidth(renderState.lineWidth)
        gl.depthFunc(renderState.depthFunc.toGl())
        gl.stencilFunc(stencil.compareMode.toGl(), stencil.referenceValue, stencil.readMask)
        gl.stencilOp(
            stencil.actionOnDepthFail.toGl(),
            stencil.actionOnDepthPassStencilFail.toGl(),
            stencil.actionOnBothPass.toGl()
        )
        gl.stencilMask(stencil.writeMask)
        gl.colorMask(colorMask.red, colorMask.green, colorMask.blue, colorMask.alpha)
        */

        //println("restored!")
    }

    fun restoreEnable() { for (n in enabledList.indices) gl.enableDisable(enabledList[n], enabledArray[n]) }

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
