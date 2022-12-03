package com.soywiz.korag

import com.soywiz.korag.shader.*
import com.soywiz.korio.annotations.*

@KorInternal
interface AGQueueProcessor {
    // EXTRA
    fun listStart(): Unit = Unit
    fun contextLost()
    // SYNC
    fun flush()
    fun finish()
    // ENABLE / DISABLE
    fun enableDisable(kind: AGEnable, enable: Boolean)
    // READ
    fun readPixels(x: Int, y: Int, width: Int, height: Int, data: Any, kind: AGReadKind)
    fun readPixelsToTexture(tex: AGTexture, x: Int, y: Int, width: Int, height: Int, kind: AGReadKind)
    // DRAW
    fun draw(type: AGDrawType, vertexCount: Int, offset: Int = 0, instances: Int = 1, indexType: AGIndexType = AGIndexType.NONE, indices: AGBuffer? = null)
    // Uniforms
    fun uniformsSet(uniforms: AGUniformValues)
    // Faces
    fun cullFace(face: AGCullFace)
    fun frontFace(face: AGFrontFace)
    // Blending
    fun blendEquation(rgb: AGBlendEquation, a: AGBlendEquation)
    fun blendFunction(srcRgb: AGBlendFactor, dstRgb: AGBlendFactor, srcA: AGBlendFactor = srcRgb, dstA: AGBlendFactor = dstRgb)
    // Color Mask
    fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean)
    // Depth
    fun depthFunction(depthTest: AGCompareMode)
    fun depthMask(depth: Boolean)
    fun depthRange(near: Float, far: Float)
    // Stencil
    fun stencilFunction(compareMode: AGCompareMode, referenceValue: Int, readMask: Int)
    fun stencilOperation(actionOnDepthFail: AGStencilOp, actionOnDepthPassStencilFail: AGStencilOp, actionOnBothPass: AGStencilOp)
    fun stencilMask(writeMask: Int)
    // Scissors & Viewport
    fun scissor(x: Int, y: Int, width: Int, height: Int)
    fun viewport(x: Int, y: Int, width: Int, height: Int)
    // Clearing
    fun clear(color: Boolean, depth: Boolean, stencil: Boolean)
    fun clearColor(red: Float, green: Float, blue: Float, alpha: Float)
    fun clearDepth(depth: Float)
    fun clearStencil(stencil: Int)
    // Programs
    fun programCreate(programId: Int, program: Program, programConfig: ProgramConfig?)
    fun programDelete(programId: Int)
    fun programUse(programId: Int)
    // VAO
    fun vaoUnuse(vao: AGVertexArrayObject)
    fun vaoUse(vao: AGVertexArrayObject)
    // TEXTURES
    fun textureBind(tex: AGTexture?, target: AGTextureTargetKind)
    fun textureSetFromFrameBuffer(tex: AGTexture, x: Int, y: Int, width: Int, height: Int)
    // FRAME BUFFER
    fun frameBufferSet(frameBuffer: AGFrameBuffer)
}
