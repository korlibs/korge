package korlibs.graphics.metal.shader

import korlibs.graphics.shader.*

data class MetalShaderBufferInputLayouts(
    var vertexLayouts: List<VertexLayout>
) {

    val vertexInputStructure by lazy {
        vertexLayouts.filter { it.items.size >= 2 }
    }

}
