package korlibs.graphics.metal.shader

import korlibs.io.util.*
import korlibs.test.*
import kotlin.test.*

class MetalShaderStructureGeneratorTest {

    @Test
    fun metal_shader_structure_must_be_correctly_generated() {
        // Given
        val attributes = listOf(
            MetalShaderStructureGenerator.Attribute("type", "attribute", "modifier"),
            MetalShaderStructureGenerator.Attribute("type", "attribute2"),
            MetalShaderStructureGenerator.Attribute("type", "attribute3", "modifier"),
            MetalShaderStructureGenerator.Attribute("type", "attribute4")
        )

        val indenter = Indenter()

        // When
        val result = MetalShaderStructureGenerator.generate(indenter, "name", attributes)
            .toString()

        // Then
        assertThat(result.trimIndent()).isEqualTo(
            """
            struct name {
            	type attribute [[modifier]];
            	type attribute2;
            	type attribute3 [[modifier]];
            	type attribute4;
            };
            """.trimIndent()
        )
    }
}
