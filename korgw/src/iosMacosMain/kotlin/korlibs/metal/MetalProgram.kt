package korlibs.metal

import korlibs.graphics.shader.*
import platform.Metal.*

data class MetalProgram(
    val renderPipelineState: MTLRenderPipelineStateProtocol,
    val inputBuffers: List<VariableWithOffset>
)