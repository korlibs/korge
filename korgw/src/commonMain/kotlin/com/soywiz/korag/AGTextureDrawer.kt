package com.soywiz.korag

import com.soywiz.kmem.*
import com.soywiz.korag.shader.*

class AGTextureDrawer(val ag: AG) {
    val VERTEX_COUNT = 4
    val vertices = AGBuffer()
    val vertexLayout = VertexLayout(DefaultShaders.a_Pos, DefaultShaders.a_Tex)
    val vertexData = AGVertexArrayObject(AGVertexData(vertexLayout, vertices))
    val verticesData = Buffer(VERTEX_COUNT * vertexLayout.totalSize)
    val program = Program(VertexShader {
        DefaultShaders {
            SET(v_Tex, a_Tex)
            SET(out, vec4(a_Pos, 0f.lit, 1f.lit))
        }
    }, FragmentShader {
        DefaultShaders {
            //out setTo vec4(1f, 1f, 0f, 1f)
            SET(out, texture2D(u_Tex, v_Tex["xy"]))
        }
    })
    val uniforms = AGUniformValues()

    fun setVertex(n: Int, px: Float, py: Float, tx: Float, ty: Float) {
        val offset = n * 4
        verticesData.setFloat32(offset + 0, px)
        verticesData.setFloat32(offset + 1, py)
        verticesData.setFloat32(offset + 2, tx)
        verticesData.setFloat32(offset + 3, ty)
    }

    fun draw(frameBuffer: AGFrameBuffer, tex: AGTexture, left: Float, top: Float, right: Float, bottom: Float) {
        //tex.upload(Bitmap32(32, 32) { x, y -> Colors.RED })
        uniforms.set(DefaultShaders.u_Tex, tex)

        val texLeft = -1f
        val texRight = +1f
        val texTop = -1f
        val texBottom = +1f

        setVertex(0, left, top, texLeft, texTop)
        setVertex(1, right, top, texRight, texTop)
        setVertex(2, left, bottom, texLeft, texBottom)
        setVertex(3, right, bottom, texRight, texBottom)

        vertices.upload(verticesData)
        ag.draw(
            frameBuffer,
            vertexData = vertexData,
            program = program,
            drawType = AGDrawType.TRIANGLE_STRIP,
            vertexCount = 4,
            uniforms = uniforms,
            blending = AGBlending.NONE
        )
    }
}
