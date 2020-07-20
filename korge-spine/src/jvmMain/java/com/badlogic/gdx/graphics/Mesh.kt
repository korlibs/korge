package com.badlogic.gdx.graphics;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class Mesh {
    public Mesh(VertexDataType vertexDataType, boolean b, int maxVertices, int i, VertexAttribute a_position, VertexAttribute a_light, VertexAttribute a_dark, VertexAttribute a_texCoord0) {
    }

    public void setVertices(float[] vertices, int i, int vertexIndex) {

    }

    public void setIndices(short[] triangles, int i, int triangleIndex) {

    }

    public void render(ShaderProgram shader, int glTriangles, int i, int triangleIndex) {

    }

    public void dispose() {
    }

    public enum VertexDataType {
        VertexArray, VertexBufferObject, VertexBufferObjectSubData, VertexBufferObjectWithVAO
    }
}
