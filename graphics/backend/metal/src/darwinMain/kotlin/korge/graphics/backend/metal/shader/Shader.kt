package korge.graphics.backend.metal.shader

import com.soywiz.korag.shader.*

fun Shader.toNewMetalShaderStringResult() = MetalShaderGenerator(type)
    .generateResult(this)

