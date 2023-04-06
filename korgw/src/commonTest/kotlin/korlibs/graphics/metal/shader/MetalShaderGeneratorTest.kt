package korlibs.graphics.metal.shader

import korlibs.graphics.DefaultShaders.a_Col
import korlibs.graphics.DefaultShaders.a_Pos
import korlibs.graphics.DefaultShaders.a_Tex
import korlibs.graphics.DefaultShaders.u_ProjMat
import korlibs.graphics.DefaultShaders.u_ViewMat
import korlibs.graphics.shader.*
import korlibs.test.*
import kotlin.test.*

/**
 * should match specification
 * https://developer.apple.com/metal/Metal-Shading-Language-Specification.pdf
 */
class MetalShaderGeneratorTest {

    object UB : UniformBlock(1) {
        val u_ColorModifier by vec4()
    }

    private val simpleBufferInputLayouts = MetalShaderBufferInputLayouts(
        vertexLayouts = listOf(
            VertexLayout(a_Pos),
            VertexLayout(a_Tex),
            VertexLayout(a_Col)
        ),
        uniforms = listOf(
            u_ProjMat,
            u_ViewMat,
            UB.u_ColorModifier.uniform
        )
    )
    private val bufferInputLayoutsWithComplexLayout = MetalShaderBufferInputLayouts(
        vertexLayouts = listOf(
            VertexLayout(a_Pos),
            VertexLayout(a_Tex, a_Col)
        ),
        uniforms = listOf(
            u_ProjMat,
            u_ViewMat,
            UB.u_ColorModifier.uniform
        )
    )

    private val vertexShader = VertexShader {
        SET(v_Tex, a_Tex)
        SET(v_Col, a_Col)
        SET(out, u_ProjMat * u_ViewMat * vec4(a_Pos, 0f.lit, 1f.lit))
    }

    private val fragmentShader = FragmentShader {
        SET(out, v_Col * UB.u_ColorModifier)
    }

    @Test
    fun check_that_vertex_metal_shader_is_correctly_generated_with_complex_layout_in_buffer_input() {
        // Given
        val metalResult = (vertexShader to fragmentShader)
            // When
            .toNewMetalShaderStringResult(bufferInputLayoutsWithComplexLayout)

        // Then
        assertThat(metalResult.result.trim()).isEqualTo("""
            #include <metal_stdlib>
            using namespace metal;
            struct Buffer1 {
            	float2 a_Tex;
            	uchar4 a_Col;
            };
            struct v2f {
            	float2 v_Tex;
            	float4 v_Col;
            	float4 position [[position]];
            };
            vertex v2f vertexMain(
            	uint vertexId [[vertex_id]],
            	device const float2* buffer0 [[buffer(0)]],
            	device const Buffer1* buffer1 [[buffer(1)]],
            	constant float4x4& u_ProjMat [[buffer(2)]],
            	constant float4x4& u_ViewMat [[buffer(3)]]
            ) {
            	v2f out;
            	auto a_Pos = buffer0[vertexId];
            	auto a_Tex = buffer1[vertexId].a_Tex;
            	auto a_Col = buffer1[vertexId].a_Col;
            	v_Tex = a_Tex;
            	out.v_Col = a_Col;
            	out.position = ((u_ProjMat * u_ViewMat) * float4(a_Pos, 0.0, 1.0));
            	return out;
            }
            fragment float4 fragmentMain(
            	v2f in [[stage_in]],
            	constant float4& u_ColorModifier [[buffer(4)]]
            ) {
            	float4 out;
            	out = (in.v_Col * u_ColorModifier);
            	return out;
            }
        """.trimIndent())

        assertThat(metalResult.inputBuffers).isEqualTo(listOf(
            listOf(a_Pos),
            listOf(a_Tex, a_Col),
            listOf(u_ProjMat),
            listOf(u_ViewMat),
            listOf(UB.u_ColorModifier)
        ))
    }

    @Test
    fun check_that_vertex_metal_shader_is_correctly_generated_with_simple_buffer_input_layout() {
        // Given
        val metalResult = (vertexShader to fragmentShader)
            // When
            .toNewMetalShaderStringResult(simpleBufferInputLayouts)

        // Then
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
            	device const float2* buffer0 [[buffer(0)]],
            	device const float2* buffer1 [[buffer(1)]],
            	device const uchar4* buffer2 [[buffer(2)]],
            	constant float4x4& u_ProjMat [[buffer(3)]],
            	constant float4x4& u_ViewMat [[buffer(4)]]
            ) {
            	v2f out;
            	auto a_Pos = buffer0[vertexId];
            	auto a_Tex = buffer1[vertexId];
            	auto a_Col = buffer2[vertexId];
            	v_Tex = a_Tex;
            	out.v_Col = a_Col;
            	out.position = ((u_ProjMat * u_ViewMat) * float4(a_Pos, 0.0, 1.0));
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
            listOf(a_Pos),
            listOf(a_Tex),
            listOf(a_Col),
            listOf(u_ProjMat),
            listOf(u_ViewMat),
            listOf(UB.u_ColorModifier)
        ))
    }
}
