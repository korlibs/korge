package korge.graphics.backend.metal.shader

import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korio.util.*
import io.kotest.core.spec.style.*
import io.kotest.matchers.*

class MetalShaderGeneratorTest : StringSpec({

    val vertexShader = VertexShader {
        IF(true.lit) {
            DefaultShaders.t_Temp0 setTo 1.lit * 2.lit
        } ELSE {
            DefaultShaders.t_Temp0 setTo 3.lit * 4.lit
        }
    }

    "check that vertex metal shader is correctly generated" {

        vertexShader shouldProduceShader {
            +"#include <metal_stdlib>"
            +"using namespace metal;"

            "v2f vertex main()" {
                +"vec4 temp0;"
                "if (true)" {
                    +"temp0 = (1 * 2);"
                }
                "else" {
                    +"temp0 = (3 * 4);"
                }
            }
        }
    }
})


infix fun Shader.shouldProduceShader(block: Indenter.() -> Unit) {
    val metalShaderAsString = toNewMetalShaderStringResult().result
    val expectedShaderAsString = Indenter {
        block()
    }.toString()

    metalShaderAsString shouldBe expectedShaderAsString
}
