package korlibs.graphics

import korlibs.datastructure.*
import korlibs.graphics.shader.*
import korlibs.graphics.shader.gl.*
import korlibs.math.geom.*
import korlibs.memory.*
import kotlin.test.*

class AGUniformBlockTest {
    object NewUniformUB : UniformBlock(fixedLocation = 2) {
        val u_ShadowColor by vec4()
        val u_ShadowRadius by float()
        val u_ShadowOffset by vec2()
        val u_HighlightPos by vec2()
        val u_HighlightRadius by float()
        val u_HighlightColor by vec4()
        val u_Size by vec2()
        val u_Radius by vec4()
        val u_BorderSizeHalf by float()
        val u_BorderColor by vec4()
        val u_BackgroundColor by vec4()

        //val uniform1 by bool4()
        //val uniform2 by short2()
        //val uniform3 by int()
    }

    object ArrayUniformUB : UniformBlock(fixedLocation = 2) {
        val u_Vec4Array10 by array(3) { vec4() }
        val u_BorderColor by vec4()
    }

    @Test
    fun testArrayUB() {
        assertEquals(
            """
                u_Vec4Array10:0
                u_BorderColor:48
            """.trimIndent(),
            ArrayUniformUB.uniforms.joinToString("\n") { "${it.name}:${it.voffset}" }
        )

        val ubb = UniformBlockBuffer(ArrayUniformUB)

        ubb.push {
            it[u_BorderColor] = Vector4.func { 33f }
            it[u_Vec4Array10] = Array(3) { N -> Vector4.func { it.toFloat() + (N * 10f) } }
        }

        assertEquals(
            listOf(0.0, 1.0, 2.0, 3.0, 10.0, 11.0, 12.0, 13.0, 20.0, 21.0, 22.0, 23.0, 33.0, 33.0, 33.0, 33.0).map { it.toFloat() },
            ubb.buffer.slice(0, ubb.blockSizeNoGlAlign).f32.toFloatArray().toList()
        )

        val str = FragmentShader {
            SET(out, ArrayUniformUB.u_Vec4Array10[0])
        }.toNewGlslString(GlslConfig.DEFAULT)
        assertTrue { "uniform vec4 u_Vec4Array10[3];" in str }
        assertTrue { "gl_FragColor = u_Vec4Array10[0];" in str }
        //println("str=$str")
    }

    @Test
    fun testAlign() {
        assertEquals(
            """
                u_ShadowColor:0
                u_ShadowRadius:16
                u_ShadowOffset:24
                u_HighlightPos:32
                u_HighlightRadius:40
                u_HighlightColor:48
                u_Size:64
                u_Radius:80
                u_BorderSizeHalf:96
                u_BorderColor:112
                u_BackgroundColor:128
            """.trimIndent(),
            NewUniformUB.uniforms.joinToString("\n") { "${it.name}:${it.voffset}" }
        )
    }

    //@Test
    //fun test() {
    //    val data = UniformBlockData(uniformBlock)
    //    data[uniform1].set(1f, 0f, 0f, 1f)
    //    data[uniform2].set(7033f, 9999f)
    //    assertFails { data[uniform3] }
    //    assertEquals("01000001791b0f27", data.data.hex())
    //}

    @Test
    fun testProgram() {
        val program = Program(
            vertex = VertexShaderDefault {
                SET(v_Tex, a_Tex)
                SET(v_Col, a_Col)
                SET(out, u_ProjMat * u_ViewMat * vec4(a_Pos, 0f.lit, 1f.lit))
            },
            fragment = FragmentShaderDefault {
                SET(out, texture2D(v_Tex, u_Tex))
            }
        )
        val program2 = Program(
            vertex = VertexShaderDefault {
                SET(v_Tex, a_Tex)
                SET(out, a_Pos)
            },
            fragment = FragmentShaderDefault {
                SET(out, texture2D(v_Tex, u_Tex))
            }
        )
        val bufferCache = AGProgramWithUniforms.BufferCache()
        val data1 = AGProgramWithUniforms(program, bufferCache)
        val data2 = AGProgramWithUniforms(program2, bufferCache)

        //data1[DefaultShaders.ub_ProjViewMatBlock].push {
        //    it[DefaultShaders.u_ProjMat].set(MMatrix4().setToOrtho(0f, 0f, 100f, 100f))
        //    it[DefaultShaders.u_ViewMat].set(MMatrix4().identity())
        //}
        data1[DefaultShaders.ProjViewUB].push {
            it[u_ProjMat] = Matrix4.ortho(0f, 0f, 100f, 100f)
            it[u_ViewMat] = Matrix4.IDENTITY
        }

        /*
        val texBlock = data1[DefaultShaders.ub_TexBlock]
        texBlock.push { it[DefaultShaders.u_Tex].set(1) }
        assertEquals(0, texBlock.currentIndex)
        texBlock.push { it[DefaultShaders.u_Tex].set(2) }
        assertEquals(1, texBlock.currentIndex)

        texBlock.pop()
        assertEquals(0, texBlock.currentIndex)

        texBlock.push(deduplicate = true) { it[DefaultShaders.u_Tex].set(3) }.also {
            assertEquals(true, it)
        }
        assertEquals(2, texBlock.currentIndex)

        texBlock.push(deduplicate = true) { it[DefaultShaders.u_Tex].set(3) }.also {
            assertEquals(false, it)
        }
        assertEquals(2, texBlock.currentIndex)

        texBlock.pop()
        assertEquals(0, texBlock.currentIndex)

        assertEquals("010000000200000003000000", texBlock.upload().agBuffer.mem?.hex())

        data2[DefaultShaders.ub_TexBlock].push { it[DefaultShaders.u_Tex].set(4) }
        assertEquals(3, texBlock.currentIndex)

        assertEquals("01000000020000000300000004000000", data2[DefaultShaders.ub_TexBlock].upload().agBuffer.mem?.hex())

        //println(data1.createRef().second.toList())
        //println(data2.createRef().second.toList())

        data2[DefaultShaders.ub_TexBlock].reset()

        assertEquals("", data2[DefaultShaders.ub_TexBlock].upload().agBuffer.mem?.hex())

         */
    }
}
