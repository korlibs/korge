package korlibs.graphics

import korlibs.graphics.shader.*
import korlibs.math.geom.*
import kotlin.test.*

class AGUniformBlockTest {
    object NewUniformUB : UniformBlock(fixedLocation = 2) {
        val uniform1 by bool4()
        val uniform2 by short2()
        //val uniform3 by int()
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
            it[DefaultShaders.ProjViewUB.u_ProjMat] = MMatrix4().setToOrtho(0f, 0f, 100f, 100f)
            it[DefaultShaders.ProjViewUB.u_ViewMat] = MMatrix4().identity()
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
