package korlibs.graphics.metal.shader

import korlibs.graphics.shader.*

internal fun Pair<VertexShader, FragmentShader>.toNewMetalShaderStringResult(bufferInputsLayout: MetalShaderBufferInputLayouts)
    = let { (vertexShader, fragmentShader) -> MetalShaderGenerator(vertexShader, fragmentShader, bufferInputsLayout) }
        .generateResult()
