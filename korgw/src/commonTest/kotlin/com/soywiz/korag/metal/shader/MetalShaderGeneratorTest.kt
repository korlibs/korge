package com.soywiz.korag.metal.shader

import com.soywiz.korag.DefaultShaders.a_Col
import com.soywiz.korag.DefaultShaders.a_Pos
import com.soywiz.korag.DefaultShaders.a_Tex
import com.soywiz.korag.DefaultShaders.u_ProjMat
import com.soywiz.korag.DefaultShaders.u_ViewMat
import com.soywiz.korag.shader.*
import com.soywiz.ktruth.*
import kotlin.test.*

/**
 * should match specification
 * https://developer.apple.com/metal/Metal-Shading-Language-Specification.pdf
 */
class MetalShaderGeneratorTest {

    val u_ColorModifier = Uniform("u_ColorModifier", VarType.Float4)

    private val vertexShader = VertexShader {
        SET(v_Tex, a_Tex)
        SET(v_Col, a_Col)
        SET(out, u_ProjMat * u_ViewMat * vec4(a_Pos, 0f.lit, 1f.lit))
    }

    private val fragmentShader = FragmentShader {
        SET(out, v_Col * u_ColorModifier)
    }

    @Test
    //@Ignore
    fun checkThatVertexMetalShaderIsCorrectlyGenerated() {
        val metalResult = (vertexShader to fragmentShader).toNewMetalShaderStringResult()

        assertThat(metalResult.result.trim()).isEqualTo("""
            #include <metal_stdlib>
            using namespace metal;
            struct v2f {
            	float2 v_Tex;
            	float4 v_Col;
            	float4 position [[position]];
            };
            vertex v2f vertexMain(
            	uint vertexId [[vertex_id]],
            	device const float2* a_Tex [[buffer(0)]],
            	device const char4* a_Col [[buffer(1)]],
            	device const float2* a_Pos [[buffer(2)]],
            	constant float4x4& u_ProjMat [[buffer(3)]],
            	constant float4x4& u_ViewMat [[buffer(4)]]
            ) {
            	v2f out;
            	v_Tex = a_Tex[vertexId];
            	out.v_Col = a_Col[vertexId];
            	out.position = ((u_ProjMat * u_ViewMat) * float4(a_Pos[vertexId], 0.0, 1.0));
            	return out;
            }
            fragment float4 fragmentMain(
            	v2f in [[stage_in]],
            	constant float4& u_ColorModifier [[buffer(5)]]
            ) {
            	float4 out;
            	out = (in.v_Col * u_ColorModifier);
            	return out;
            }
        """.trimIndent())

        assertThat(metalResult.inputBuffers).isEqualTo(listOf(
            a_Tex,
            a_Col,
            a_Pos,
            u_ProjMat,
            u_ViewMat,
            u_ColorModifier
        ))
    }
}
