package korlibs.graphics.metal.shader

import korlibs.graphics.shader.*

data class MetalShaderBufferInputLayouts(
    var vertexLayouts: List<VertexLayout>,
    var uniforms: List<UniformBlock>
) {

    val vertexInputStructure by lazy {
        vertexLayouts.filter { it.items.size >= 2 }
    }

    fun MetalShaderGenerator.computeInputBuffers(): Lazy<MutableList<List<VariableWithOffset>>> = lazy {
        (vertexLayouts.map { it.items }  + uniforms.map { it.uniforms })
            .toMutableList()
    }
}
