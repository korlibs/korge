package korge.graphics.backend.metal.shader

import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.*
import com.soywiz.korio.util.*
import io.kotest.core.spec.style.*
import io.kotest.matchers.*

/**
 * should match specification
 * https://developer.apple.com/metal/Metal-Shading-Language-Specification.pdf
 */
class MetalShaderGeneratorTest : StringSpec({

    val vertexShader = DefaultShaders.VERTEX_DEFAULT
    val fragmentShader = DefaultShaders.FRAGMENT_SOLID_COLOR

    vertexShader.toNewGlslStringResult()
        .result
        .let(::println)

    fragmentShader.toNewGlslStringResult()
        .result
        .let(::println)

    "check that vertex metal shader is correctly generated" {

        vertexShader to fragmentShader shouldProduceShader {
            +"#include <metal_stdlib>"
            +"using namespace metal;"

            "struct v2f"(expressionSuffix = ";") {
                +"float2 v_Tex;"
                +"float4 v_Col;"
                +"float4 position [[position]];"
            }

            "vertex v2f vertexMain(uint vertexId [[vertex_id]],device const float2* a_Tex [[buffer(0)]],device const float4* a_Col [[buffer(1)]],device const float2* a_Pos [[buffer(2)]])" {
                +"v2f out;"
                +"v_Tex = a_Tex[ vertexId ];"
                +"out.v_Col = a_Col[ vertexId ];"
                +"out.position = ((u_ProjMat * u_ViewMat) * float4(a_Pos[ vertexId ], 0.0, 1.0));"
                +"return out;"
            }

            "fragment float4 fragmentMain( v2f in [[stage_in]] )" {
                +"float4 out;"
                +"out = in.v_Col;"
                +"return out;"
            }
        }
    }
})


infix fun Pair<VertexShader, FragmentShader>.shouldProduceShader(block: Indenter.() -> Unit) {
    val metalShaderAsString = toNewMetalShaderStringResult().result
    val expectedShaderAsString = Indenter {
        block()
    }.toString()

    metalShaderAsString shouldBe expectedShaderAsString
}
