package korlibs.metal

import korlibs.graphics.*
import korlibs.graphics.metal.shader.*
import korlibs.graphics.shader.*
import korlibs.logger.*
import korlibs.memory.*
import korlibs.metal.shader.*
import kotlinx.cinterop.*
import platform.Metal.*
import platform.MetalKit.*

class AGMetal(
    private val view: MTKView,
    // TODO: fix support of vertex data with complex layouts to remove this
    private val shouldFlattenVertexData: Boolean = true
) : AG() {

    private val logger = Logger("AGMetal")
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
        autoreleasepool { // TODO: Check if that necessary

            val intermediateVertexData = computeIntermediateVertexData(vertexData)

            val currentProgram = getProgram(
                program,
                intermediateVertexData,
                uniformBlocks
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

                intermediateVertexData.list.fastForEach { vertexDataUnit ->
                    val bufferLocation = currentProgram.indexOfAttributeOnBuffer(vertexDataUnit.layout.items)
                    logger.trace { "${vertexDataUnit.layout.items} will be bind at location $bufferLocation" }
                    val buffer = vertexDataUnit.buffer.toMetal.buffer
                    setVertexBuffer(buffer, offset = 0uL, bufferLocation)
                }

                //var currentBuffer = 0uL
                //vertexData.list.fastForEach{ buffer ->
                //    setVertexBuffer(buffer.buffer.toMetal.buffer, 0.convert(), currentBuffer)
                //    currentBuffer += 1uL
                //}

                uniformBlocks.fastForEachBlock { index, block, buffer, valueIndex ->
                    if (buffer == null) {
                        logger.warn { "empty buffer" }
                        return
                    }
                    val uniform = block.block.uniforms.map { it.uniform }.first()
                    val bufferLocation = currentProgram.indexOfUniformOnBuffer(uniform)
                    logger.trace { "$uniform will be bind at location $bufferLocation" }
                    setVertexBuffer(buffer.toMetal.buffer, 0, bufferLocation)
                }

                if (indices != null) {
                    indices.mem!!.size
                    drawIndexedPrimitives(
                        drawType.toMetal(),
                        vertexCount.toULong(),
                        indexType.toMetal(),
                        indices.toMetal.buffer,
                        0
                    )
                    //drawIndexedPrimitives(drawType.toMetal(), vertexCount.toULong(), indexType.toMetal(), indices.toMetal.buffer, 0.convert())
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

    private fun computeIntermediateVertexData(vertexData: AGVertexArrayObject): AGVertexArrayObject = when (shouldFlattenVertexData) {
            true -> vertexData.flatten()
            false -> vertexData
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

    private fun getProgram(
        program: Program,
        vertexData: AGVertexArrayObject,
        uniformBlocks: UniformBlocksBuffersRef
    ) = programs
        .getOrPut(program) {
            MetalShaderCompiler.compile(
                device,
                program,
                bufferInputLayouts = lazyMetalShaderBufferInputLayouts(
                    vertexLayouts = vertexData.map { it.layout },
                    uniforms = uniformBlocks.map()
                )
            )
        }
}

private fun AGVertexArrayObject.map(function: (AGVertexData) -> ProgramLayout<Attribute>): List<ProgramLayout<Attribute>> {
    return mutableListOf<ProgramLayout<Attribute>>().apply {
        list.fastForEach { input -> function(input).also { add(it) } }
    }
}

private fun UniformBlocksBuffersRef.map(): List<Uniform> {
    return mutableListOf<Uniform>().apply {
        fastForEachBlock { _, block, _, _ ->
            addAll(block.block.uniforms.map { it.uniform })
        }
    }
}
