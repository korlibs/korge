package korge.graphics.backend.metal.shader

import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.*
import com.soywiz.korio.util.*
import com.soywiz.ktruth.*
import kotlin.test.*


val a_ColMul: Attribute get() = DefaultShaders.a_Col
val a_ColAdd: Attribute = Attribute("a_Col2", VarType.Byte4, normalized = true, fixedLocation = 3)

val v_ColMul: Varying get() = DefaultShaders.v_Col
val v_ColAdd: Varying = Varying("v_Col2", VarType.Float4)

val a_TexIndex: Attribute = Attribute("a_TexIndex", VarType.UByte1, normalized = false, precision = Precision.LOW, fixedLocation = 4)
val a_Wrap: Attribute = Attribute("a_Wrap", VarType.UByte1, normalized = false, precision = Precision.LOW, fixedLocation = 5)

val v_TexIndex: Varying = Varying("v_TexIndex", VarType.Float1, precision = Precision.LOW)
val v_Wrap: Varying = Varying("v_Wrap", VarType.Float1, precision = Precision.LOW)


/**
 * should match specification
 * https://developer.apple.com/metal/Metal-Shading-Language-Specification.pdf
 */
class MetalShaderGeneratorTest {

    val vertexShader = DefaultShaders.VERTEX_DEFAULT
    val fragmentShader = DefaultShaders.FRAGMENT_SOLID_COLOR

    @Test
    fun `check that vertex metal shader is correctly generated`() {

        vertexShader to fragmentShader shouldProduceShader {
            +"#include <metal_stdlib>"
            +"using namespace metal;"

            "struct v2f"(expressionSuffix = ";") {
                +"float2 v_Tex;"
                +"float4 v_Col;"
                +"float4 position [[position]];"
            }

            val vertexArgs = listOf(
                "uint vertexId [[vertex_id]]",
                "device const float2* a_Tex [[buffer(0)]]",
                "device const float4* a_Col [[buffer(1)]]",
                "device const float2* a_Pos [[buffer(2)]]",
                "constant float4x4& u_ProjMat [[buffer(3)]]",
                "constant float4x4& u_ViewMat [[buffer(4)]]"
            ).joinToString(",")

            "vertex v2f vertexMain($vertexArgs)" {
                +"v2f out;"
                +"v_Tex = a_Tex[vertexId];"
                +"out.v_Col = a_Col[vertexId];"
                +"out.position = ((u_ProjMat * u_ViewMat) * float4(a_Pos[vertexId], 0.0, 1.0));"
                +"return out;"
            }

            "fragment float4 fragmentMain(v2f in [[stage_in]])" {
                +"float4 out;"
                +"out = in.v_Col;"
                +"return out;"
            }
        }
    }
}

infix fun Pair<VertexShader, FragmentShader>.shouldProduceShader(block: Indenter.() -> Unit) {
    val metalShaderAsString = toNewMetalShaderStringResult().result
    val expectedShaderAsString = Indenter {
        block()
    }.toString()

    metalShaderAsString shouldBe expectedShaderAsString
}

infix fun String.shouldBe(expected: String) {

    //assertThat(length).isEqualTo(expected.length)
    val expectedLines = expected.lines()
    val lines = lines()
    assertThat(lines.size).isEqualTo(expectedLines.size)

    lines.forEachIndexed { index, line ->
        val expectedLine = expectedLines[index]
        assertThat(line).isEqualTo(expectedLine)
    }
}
