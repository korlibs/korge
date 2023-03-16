package korlibs.metal

import korlibs.graphics.shader.*
import platform.Metal.*

data class MetalProgram(
    val renderPipelineState: MTLRenderPipelineStateProtocol,
    val inputBuffers: List<VariableWithOffset>
) {
    fun indexOfAttributeOnBuffer(attribute: Attribute): ULong {
        return inputBuffers.indexOf(attribute).toULong()
    }

    fun indexOfUniformOnBuffer(uniform: Uniform): ULong {
        return inputBuffers.indexOf(uniform).toULong()
    }
}
