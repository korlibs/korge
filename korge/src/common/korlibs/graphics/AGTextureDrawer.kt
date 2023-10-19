package korlibs.graphics

import korlibs.datastructure.*
import korlibs.graphics.shader.*
import korlibs.memory.*

val AG.textureDrawer by Extra.PropertyThis {
    AGTextureDrawer(this)
}

class AGTextureDrawer(val ag: AG) {
    val VERTEX_COUNT = 4
    val vertices = AGBuffer()
    val vertexLayout = VertexLayout(DefaultShaders.a_Pos, DefaultShaders.a_Tex)
    val vertexData = AGVertexArrayObject(AGVertexData(vertexLayout, vertices), isDynamic = false)
    val verticesData = Buffer(VERTEX_COUNT * vertexLayout.totalSize)
    val verticesDataF32 = verticesData.asFloat32()
    val textureUnits = AGTextureUnits()
    companion object {
        val PROGRAM = Program(VertexShader {
            DefaultShaders {
                SET(v_Tex, a_Tex)
                SET(out, vec4(a_Pos, 0f.lit, 1f.lit))
            }
        }, FragmentShader {
            DefaultShaders {
                //SET(out, vec4(1f, .5f, 0f, 1f))
                SET(out, texture2D(u_Tex, v_Tex["xy"]))
            }
        })
    }
    val ref = AGProgramWithUniforms(PROGRAM)

    fun setVertex(n: Int, px: Float, py: Float, tx: Float, ty: Float) {
        val offset = n * 4
        verticesDataF32[offset + 0] = px
        verticesDataF32[offset + 1] = py
        verticesDataF32[offset + 2] = tx
        verticesDataF32[offset + 3] = ty
    }

    fun draw(frameBuffer: AGFrameBuffer, tex: AGTexture, left: Float = -1f, top: Float = +1f, right: Float = +1f, bottom: Float = -1f) {
        //tex.upload(Bitmap32(32, 32) { x, y -> Colors.RED })
        //uniforms.set(DefaultShaders.u_Tex, tex)
        textureUnits.set(DefaultShaders.u_Tex, tex)

        val texLeft = 0f
        val texRight = +1f
        val texTop = 0f
        val texBottom = +1f

        setVertex(0, left, top, texLeft, texTop)
        setVertex(1, right, top, texRight, texTop)
        setVertex(2, left, bottom, texLeft, texBottom)
        setVertex(3, right, bottom, texRight, texBottom)

        vertices.upload(verticesData)

        //println(verticesDataF32.toFloatArray().toList())

        //ag.clear(frameBuffer.base, frameBuffer.info, Colors.BLUE)
        //println("READ_PIXEL[a]:" + ag.readPixel(frameBuffer, 0, 0))
        ag.draw(
            frameBuffer,
            vertexData = vertexData,
            program = PROGRAM,
            drawType = AGDrawType.TRIANGLE_STRIP,
            indexType = AGIndexType.NONE,
            vertexCount = 4,
            uniformBlocks = ref.createRef(),
            textureUnits = textureUnits,
            blending = AGBlending.NONE,
        )
        //println("READ_PIXEL[b]:" + ag.readPixel(frameBuffer, 0, 0))
    }
}
