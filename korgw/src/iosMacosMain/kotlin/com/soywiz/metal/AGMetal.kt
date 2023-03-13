package com.soywiz.metal

import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.metal.shader.*
import kotlinx.cinterop.*
import platform.Metal.*
import platform.MetalKit.*

class AGMetal(private val view: MTKView) : AG() {

    private val device = MTLCreateSystemDefaultDevice() ?: error("fail to create metal device")
    private val commandQueue = device.newCommandQueue() ?: error("fail to create metal command queue")
    private val programs = HashMap<Program, MetalProgram>()
    // TODO: this will be rework on next iteration
    private val buffersv1 = HashMap<Buffer, MTLBuffer>()
    private val buffers = HashMap<MetalProgram, List<MTLBuffer>>()

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
        uniforms: AGUniformValues,
        uniformBlocks: AGUniformBlocksBuffersRef,
        stencilRef: AGStencilReference,
        stencilOpFunc: AGStencilOpFunc,
        colorMask: AGColorMask,
        depthAndFrontFace: AGDepthAndFrontFace,
        scissor: AGScissor,
        cullFace: AGCullFace,
        instances: Int
    ) {
        autoreleasepool { // TODO: Check if that necessary

            val currentProgram = getProgram(
                program
            )

            val commandBuffer = commandQueue.commandBuffer() ?: error("fail to get command buffer")
            val renderPassDescriptor =
                view.currentRenderPassDescriptor ?: error("fail to get current render pass descriptor")

            val renderCommanderEncoder = commandBuffer.renderCommandEncoderWithDescriptor(renderPassDescriptor)
                ?: error("fail to get render commander encoder")

            renderCommanderEncoder.apply {
                setViewport(frameBufferInfo.toViewPort())
                setRenderPipelineState(currentProgram.renderPipelineState)

                // TODO: support texture
                //setFragmentTexture(texture, 0)
                //setFragmentSamplerState(samplerState, 0)

                var currentBuffer = 0uL
                vertexData.list.fastForEach{ buffer ->
                    setVertexBuffer(buffer.buffer.toMetal.buffer, 0, currentBuffer)
                    currentBuffer += 1uL
                }

                uniformBlocks.fastForEachUniform {
                    setVertexBuffer(it.data.toMetal.buffer, 0, currentBuffer)
                    currentBuffer += 1uL
                }

                uniforms.values.fastForEach { buffer ->
                    setVertexBuffer(buffer.data.toMetal.buffer, 0, currentBuffer)
                    currentBuffer += 1uL
                }

                if (indices != null) {
                    drawIndexedPrimitives(drawType.toMetal(), vertexCount.toULong(), indexType.toMetal(), indices.toMetal.buffer, 0)
                } else {
                    TODO("Not yet supported, rendering without vertex indexes")
                }

                popDebugGroup()
                endEncoding()
            }

            val drawable = view.currentDrawable ?: error("fail to get current drawable")
            commandBuffer.presentDrawable(drawable)
            commandBuffer.commit()
        }
    }


    private val Buffer.toMetal: MTLBuffer
        get() = buffersv1
            .getOrPut(this) {
                val size = sizeInBytes.toULong()
                device.newBuffer(size)
                    .also { it.insert(data) }
            }

    private val AGBuffer.toMetal: MTLBuffer
        get() = (mem ?: error("cannot create buffer from null memory")).toMetal

    private fun getProgram(program: Program) = programs
        .getOrPut(program) {
            MetalShaderCompiler.compile(device, program)
        }
}


