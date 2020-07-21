package com.esotericsoftware.spine.rendering

class Mesh(vertexDataType: VertexDataType?, b: Boolean, maxVertices: Int, i: Int, a_position: VertexAttribute?, a_light: VertexAttribute?, a_dark: VertexAttribute?, a_texCoord0: VertexAttribute?) {
    fun setVertices(vertices: FloatArray?, i: Int, vertexIndex: Int) {}
    fun setIndices(triangles: ShortArray?, i: Int, triangleIndex: Int) {}
    fun render(shader: ShaderProgram?, glTriangles: Int, i: Int, triangleIndex: Int) {}
    fun dispose() {}
    enum class VertexDataType {
        VertexArray, VertexBufferObject, VertexBufferObjectSubData, VertexBufferObjectWithVAO
    }
}
