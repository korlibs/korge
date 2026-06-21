package korlibs.graphics.metal

import korlibs.graphics.*
import korlibs.graphics.gl.*
import korlibs.graphics.shader.*
import korlibs.image.color.*

class AGMetal : AG() {
    override fun clear(
        frameBuffer: AGFrameBufferBase,
        frameBufferInfo: AGFrameBufferInfo,
        color: RGBA,
        depth: Float,
        stencil: Int,
        clearColor: Boolean,
        clearDepth: Boolean,
        clearStencil: Boolean,
        scissor: AGScissor
    ) {
        frameBuffer._native
        super.clear(frameBuffer, frameBufferInfo, color, depth, stencil, clearColor, clearDepth, clearStencil, scissor)
    }

    override fun draw(
        frameBuffer: AGFrameBufferBase,
        frameBufferInfo: AGFrameBufferInfo,
        vertexData: AGVertexArrayObject,
        program: Program,
        drawType: AGDrawType,
        vertexCount: Int,
        indices: AGBuffer?,
        indexType: AGIndexType,
        drawOffset: Int,
        blending: AGBlending,
        uniformBlocks: UniformBlocksBuffersRef,
        textureUnits: AGTextureUnits,
        stencilRef: AGStencilReference,
        stencilOpFunc: AGStencilOpFunc,
        colorMask: AGColorMask,
        depthAndFrontFace: AGDepthAndFrontFace,
        scissor: AGScissor,
        cullFace: AGCullFace,
        instances: Int
    ) {
        super.draw(
            frameBuffer,
            frameBufferInfo,
            vertexData,
            program,
            drawType,
            vertexCount,
            indices,
            indexType,
            drawOffset,
            blending,
            uniformBlocks,
            textureUnits,
            stencilRef,
            stencilOpFunc,
            colorMask,
            depthAndFrontFace,
            scissor,
            cullFace,
            instances
        )
    }

    override fun readToTexture(
        frameBuffer: AGFrameBufferBase,
        frameBufferInfo: AGFrameBufferInfo,
        texture: AGTexture,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) {
        super.readToTexture(frameBuffer, frameBufferInfo, texture, x, y, width, height)
    }
}
