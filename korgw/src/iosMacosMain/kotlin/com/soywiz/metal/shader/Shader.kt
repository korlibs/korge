package com.soywiz.metal.shader

import com.soywiz.korag.shader.*

fun Pair<VertexShader, FragmentShader>.toNewMetalShaderStringResult() = let { (vertexShader, fragmentShader) ->
    MetalShaderGenerator(vertexShader, fragmentShader) }
    .generateResult()

