package korge.graphics.backend.metal

import com.soywiz.korag.*
import com.soywiz.korag.shader.Program
import korge.graphics.backend.metal.shader.MetalShaderCompiler
import platform.Metal.MTLCreateSystemDefaultDevice

class AGMetal : AG() {

    private val device = MTLCreateSystemDefaultDevice() ?: error("fail to create device")
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
        val currentProgram = getProgram(
            program
        )
        TODO()
    }


    private fun getProgram(program: Program) = programs
        .getOrPut(program) {
            MetalShaderCompiler.compile(device, program)
        }
}

