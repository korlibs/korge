package korge.graphics.backend.metal

import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.Program
import korge.graphics.backend.metal.shader.MetalShaderCompiler
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.Metal.*
import platform.MetalKit.*
import platform.QuartzCore.*
import platform.posix.*

private const val maxSupportedIndex = 6uL
private val indexTypeSize = sizeOf<IntVar>().toULong()
private val vertexBufferMaxSize = 1024uL * 1024uL

class AGMetal(private val view: MTKView) : AG() {

    private val device = MTLCreateSystemDefaultDevice() ?: error("fail to create metal device")
    private val commandQueue = device.newCommandQueue() ?: error("fail to create metal command queue")
    private val programs = HashMap<Program, MetalProgram>()
    private val buffers = HashMap<AGBuffer, MTLBufferProtocol>()

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
            val renderPassDescriptor = view.currentRenderPassDescriptor ?: error("fail to get current render pass descriptor")

            val renderCommanderEncoder = commandBuffer.renderCommandEncoderWithDescriptor(renderPassDescriptor)
                ?: error("fail to get render commander encoder")

            renderCommanderEncoder.apply {
                setRenderPipelineState(currentProgram.renderPipelineState)

                // TODO: support texture
                //setFragmentTexture(texture, 0)
                //setFragmentSamplerState(samplerState, 0)

                vertexData.list.fastForEachWithIndex { index, buffer ->
                    setVertexBuffer(buffer.buffer.toMetal, 0, index.toULong())
                }

                if (indices != null) {
                    drawIndexedPrimitives(MTLPrimitiveTypeTriangle, 6u, MTLIndexTypeUInt32, indices.toMetal, 0)
                } else {
                    TODO("Not yet supported, rendering without vertex indexes")
                }

                popDebugGroup()
                endEncoding()
            }

            val drawable = view.currentDrawable ?:  error("fail to get current drawable")
            commandBuffer.presentDrawable(drawable)
            commandBuffer.commit()
        }
    }


    private val AGBuffer.toMetal: MTLBufferProtocol
        get() = buffers
            .getOrPut(this) {
                val memory = mem ?: error("cannot create buffer from null memory")
                val size = memory.sizeInBytes.toULong()
                val buffer = device.newBufferWithLength(size, MTLResourceStorageModeManaged)
                        ?: error("fail to create metal buffer")

                memory.data.usePinned {
                    memmove(buffer.contents(), it.startAddressOf, size)
                    buffer.didModifyRange(NSMakeRange(0, buffer.length))
                }

                println("buffer created with size $size")
                val values = buffer.contents()!!.reinterpret<FloatVar>()
                (0 until (size / 4u).toInt()).forEach {
                    println(values[it])
                }
                buffer
        }

    private fun getProgram(program: Program) = programs
        .getOrPut(program) {
            MetalShaderCompiler.compile(device, program)
        }
}

