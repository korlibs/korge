package korge.graphics.backend.metal

import com.soywiz.kgl.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korio.lang.*
import korge.graphics.backend.metal.shader.*
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.Metal.*
import platform.MetalKit.*
import platform.QuartzCore.*
import platform.posix.*

class AGMetal(private val view: MTKView) : AG() {

    private val device = MTLCreateSystemDefaultDevice() ?: error("fail to create metal device")
    private val commandQueue = device.newCommandQueue() ?: error("fail to create metal command queue")
    private val programs = HashMap<Program, MetalProgram>()
    private val buffers = HashMap<Buffer, MTLBufferProtocol>()

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
                    setVertexBuffer(buffer.buffer.toMetal, 0, currentBuffer)
                    currentBuffer += 1uL
                }

                uniforms.values.fastForEach { buffer ->
                    setVertexBuffer(buffer.data.toMetal, 0, currentBuffer)
                    currentBuffer += 1uL
                }

                if (indices != null) {
                    drawIndexedPrimitives(drawType.toMetal(), vertexCount.toULong(), indexType.toMetal(), indices.toMetal, 0)
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


    private val Buffer.toMetal: MTLBufferProtocol
        get() = buffers
            .getOrPut(this) {
                val size = sizeInBytes.toULong()
                val buffer = device.newBufferWithLength(size, MTLResourceStorageModeManaged)
                    ?: error("fail to create metal buffer")

                data.usePinned {
                    memmove(buffer.contents(), it.startAddressOf, size)
                    buffer.didModifyRange(NSMakeRange(0, buffer.length))
                }

                buffer
            }

    private val AGBuffer.toMetal: MTLBufferProtocol
        get() = (mem ?: error("cannot create buffer from null memory")).toMetal

    private fun getProgram(program: Program) = programs
        .getOrPut(program) {
            MetalShaderCompiler.compile(device, program)
        }
}


