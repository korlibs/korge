package korge.graphics.backend.metal

import com.soywiz.korag.shader.*
import platform.Metal.*

data class MetalProgram(
    val renderPipelineState: MTLRenderPipelineStateProtocol,
    val inputBuffers: List<VariableWithOffset>
)
