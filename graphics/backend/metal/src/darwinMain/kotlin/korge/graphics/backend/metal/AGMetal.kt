package korge.graphics.backend.metal

import com.soywiz.korag.*
import com.soywiz.korag.shader.Program
import korge.graphics.backend.metal.shader.MetalShaderCompiler
import kotlinx.cinterop.*
import platform.Metal.*
import platform.QuartzCore.*

class AGMetal(private val drawable: CAMetalDrawableProtocol) : AG() {

    private val device = MTLCreateSystemDefaultDevice() ?: error("fail to create metal device")
    private val commandQueue = device.newCommandQueue() ?: error("fail to create metal command queue")
    private val programs = HashMap<Program, MetalProgram>()

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
        autoreleasepool {
            val currentProgram = getProgram(
                program
            )

            val commandBuffer = commandQueue.commandBuffer() ?: error("fail to get command buffer")

            val renderPassDescriptor = MTLRenderPassDescriptor()
            (renderPassDescriptor.colorAttachments as? List<MTLRenderPassColorAttachmentDescriptor>)
                ?.get(0)
                ?.let {
                    //it.texture = drawable.texture
                    it.loadAction = MTLLoadActionClear
                    it.clearColor = MTLClearColorMake(0.85, 0.85, 0.85, 1.0)

                }

            val renderCommanderEncoder = commandBuffer.renderCommandEncoderWithDescriptor(renderPassDescriptor)
                ?: error("fail to get render commander encoder")

            renderCommanderEncoder.apply {
                setRenderPipelineState(currentProgram.renderPipelineState)
                //TODO: complete

                endEncoding()
            }


            commandBuffer.presentDrawable(drawable)
            commandBuffer.commit()

            TODO()
        }
    }


    private fun getProgram(program: Program) = programs
        .getOrPut(program) {
            MetalShaderCompiler.compile(device, program)
        }
}

