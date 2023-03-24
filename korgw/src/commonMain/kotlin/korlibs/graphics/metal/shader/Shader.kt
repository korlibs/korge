package korlibs.graphics.metal.shader

import korlibs.graphics.shader.*

fun Pair<VertexShader, FragmentShader>.toNewMetalShaderStringResult() = let { (vertexShader, fragmentShader) ->
    MetalShaderGenerator(vertexShader, fragmentShader) }
    .generateResult()
