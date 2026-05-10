package korlibs.graphics.metal.shader

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.test.*
import kotlin.test.*

class MetalShaderBufferInputLayoutsTest {

    @Test
    fun should_return_correct_attribute_index() {
        // Given
        val bufferInputLayoutsWithComplexLayout = lazyMetalShaderBufferInputLayouts(
            vertexLayouts = listOf(
                VertexLayout(DefaultShaders.a_Pos),
                VertexLayout(DefaultShaders.a_Tex, DefaultShaders.a_Col)
            ),
            uniforms = listOf(
                DefaultShaders.u_ProjMat,
                DefaultShaders.u_ViewMat,
                MetalShaderGeneratorTest.UB.u_ColorModifier.uniform
            )
        ).value


        // When
        bufferInputLayoutsWithComplexLayout.attributeIndexOf(DefaultShaders.a_Col)
            //Then
            .let { assertThat(it).isEqualTo(2) }

        // When
        bufferInputLayoutsWithComplexLayout.attributeIndexOf(DefaultShaders.a_Tex)
        //Then
            .let { assertThat(it).isEqualTo(1) }

        // When
        bufferInputLayoutsWithComplexLayout.attributeIndexOf(DefaultShaders.a_Pos)
            //Then
            .let { assertThat(it).isEqualTo(0) }
    }
}
