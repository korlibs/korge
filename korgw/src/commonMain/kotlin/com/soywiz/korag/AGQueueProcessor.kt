package com.soywiz.korag

import com.soywiz.kmem.FBuffer
import com.soywiz.korag.shader.Program
import com.soywiz.korag.shader.ProgramConfig
import com.soywiz.korag.shader.UniformLayout
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.annotations.KorInternal

@KorInternal
interface AGQueueProcessor {
    // EXTRA
    fun contextLost()
    // SYNC
    fun flush()
    fun finish()
    // ENABLE / DISABLE
    fun enableDisable(kind: AGEnable, enable: Boolean)
    // READ
    fun readPixels(x: Int, y: Int, width: Int, height: Int, data: Any, kind: AG.ReadKind)
    fun readPixelsToTexture(textureId: Int, x: Int, y: Int, width: Int, height: Int, kind: AG.ReadKind)
    // DRAW
    fun draw(type: AGDrawType, vertexCount: Int, offset: Int = 0, instances: Int = 1, indexType: AGIndexType? = null, indices: AG.Buffer? = null)
    // Buffers
    fun bufferCreate(id: Int)
    fun bufferDelete(id: Int)
    // Uniforms + UBO
    fun uniformsSet(layout: UniformLayout, data: FBuffer)
    fun uboCreate(id: Int)
    fun uboDelete(id: Int)
    fun uboSet(id: Int, ubo: AG.UniformValues)
    fun uboUse(id: Int)
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
    fun stencilFunction(compareMode: AG.CompareMode, referenceValue: Int, readMask: Int)
    fun stencilOperation(actionOnDepthFail: AG.StencilOp, actionOnDepthPassStencilFail: AG.StencilOp, actionOnBothPass: AG.StencilOp)
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
    fun vaoCreate(id: Int)
    fun vaoDelete(id: Int)
    fun vaoSet(id: Int, vao: AG.VertexArrayObject)
    fun vaoUse(id: Int)
    // TEXTURES
    fun textureCreate(textureId: Int)
    fun textureDelete(textureId: Int)
    fun textureUpdate(
        textureId: Int,
        target: AG.TextureTargetKind,
        index: Int,
        bmp: Bitmap?,
        source: AG.BitmapSourceBase,
        doMipmaps: Boolean,
        premultiplied: Boolean
    )
    fun textureBind(textureId: Int, target: AG.TextureTargetKind, implForcedTexId: Int)
    fun textureBindEnsuring(tex: AG.Texture?)
    fun textureSetFromFrameBuffer(textureId: Int, x: Int, y: Int, width: Int, height: Int)
    // FRAME BUFFER
    fun frameBufferCreate(id: Int)
    fun frameBufferDelete(id: Int)
    fun frameBufferSet(id: Int, textureId: Int, width: Int, height: Int, hasStencil: Boolean, hasDepth: Boolean)
    fun frameBufferUse(id: Int)
}
