import korlibs.graphics.shader.Program
import korlibs.graphics.shader.VertexShader
import korlibs.graphics.shader.appending
import korlibs.graphics.software.AGFactorySoftware
import korlibs.io.async.suspendTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AGTest {
	@Test
	fun testOnReady() = suspendTest {
		val ag = AGFactorySoftware().create(null, AGConfig())
		val buffer = ag.createIndexBuffer()
		buffer.upload(intArrayOf(1, 2, 3, 4))
	}

    @Test
    fun testCombineScissor() {
        assertEquals(AGScissor.NIL, AGScissor.combine(AGScissor.NIL, AGScissor.NIL))
        assertEquals(AGScissor(0, 0, 100, 100), AGScissor.combine(AGScissor(0, 0, 100, 100), AGScissor.NIL))
        assertEquals(AGScissor(50, 50, 50, 50), AGScissor.combine(AGScissor(0, 0, 100, 100), AGScissor(50, 50, 100, 100)))
        assertEquals(AGScissor(50, 50, 100, 100), AGScissor.combine(AGScissor.NIL, AGScissor(50, 50, 100, 100)))
        assertEquals(AGScissor(0, 0, 0, 0), AGScissor.combine(AGScissor(2000, 2000, 100, 100), AGScissor(50, 50, 100, 100)))
    }

    val VERTEX = VertexShaderDefault {
        SET(v_Tex, a_Tex)
        SET(out, (u_ProjMat * u_ViewMat) * vec4(a_Pos, 0f.lit, 1f.lit))
    }
    val FRAGMENT = FragmentShaderDefault {
        SET(out, out["bgra"])
    }

    @Test
    fun testReusedShaders() {
        fun createProgram() = Program(
            VERTEX,
            FRAGMENT.appending { SET(out, out["gbba"]) }
        )
        val ag = AGFactorySoftware().create(null, AGConfig())
        val program1 = ag.getProgram(createProgram())
        val program2 = ag.getProgram(createProgram())
        assertSame(program1, program2, "Recreating a shader model, should not regenerate a hardware shader")
    }
}