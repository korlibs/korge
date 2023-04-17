package korlibs.metal

import korlibs.graphics.metal.shader.MetalShaderBufferInputLayouts
import korlibs.graphics.shader.*
import platform.Metal.*

internal data class MetalProgram(
    val renderPipelineState: MTLRenderPipelineStateProtocol,
    val inputBuffers: MetalShaderBufferInputLayouts
) {
    fun indexOfAttributeOnBuffer(attribute: List<Attribute>): ULong {
        return inputBuffers.inputBuffers.indexOf(attribute).toULong()
    }

    fun indexOfUniformOnBuffer(uniform: Uniform): ULong {
        return inputBuffers.inputBuffers.indexOf(listOf(uniform)).toULong()
    }
}
