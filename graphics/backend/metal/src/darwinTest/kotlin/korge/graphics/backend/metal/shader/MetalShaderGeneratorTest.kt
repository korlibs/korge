package korge.graphics.backend.metal.shader

import com.soywiz.korag.*
import com.soywiz.korag.shader.*
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

    "check that vertex metal shader is correctly generated" {

        vertexShader to fragmentShader shouldProduceShader {
            +"#include <metal_stdlib>"
            +"using namespace metal;"

            "struct VertexInput" {
                +"packed_float3 position;"
                +"packed_float3 color;"
            }

            "struct VertexInput" {
                +"float4 computedPosition [[position]];"
                +"float4 color;"
            }

            "vertex VertexOutput vertexMain(device const VertexInput* vertexInput [[buffer(0)]], const device SceneMatrices& sceneMatrices [[ buffer(1) ]], unsigned int vertexId [[ vertex_id ]])" {
                +"vertexInput vertex = vertexInput[vertexId];"
                +"VertexOutput vertexOutput = VertexOutput;"
                +"vertexOutput.computedPosition = sceneMatrices.projectionMatrix * sceneMatrices.viewModelMatrix * float4(vertex.position, 1.0);"
                +"vertexOutput.color = v.color;"
                +"return vertexOutput"
            }

            "fragment float4 fragmentMain(VertexOut vertexOutput [[stage_in]])" {
                +"return float4(vertexOutput.color)"
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
