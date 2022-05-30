package com.soywiz.korag

import com.soywiz.korag.shader.Program
import com.soywiz.korag.shader.VertexShader
import com.soywiz.korag.shader.appending
import com.soywiz.korag.software.AGFactorySoftware
import com.soywiz.korio.async.suspendTest
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
        assertEquals(null, AG.Scissor.combine(null, null))
        assertEquals(AG.Scissor(0.0, 0.0, 100.0, 100.0), AG.Scissor.combine(AG.Scissor(0.0, 0.0, 100.0, 100.0), null))
        assertEquals(AG.Scissor(50.0, 50.0, 50.0, 50.0), AG.Scissor.combine(AG.Scissor(0.0, 0.0, 100.0, 100.0), AG.Scissor(50.0, 50.0, 100.0, 100.0)))
        assertEquals(AG.Scissor(50.0, 50.0, 100.0, 100.0), AG.Scissor.combine(null, AG.Scissor(50.0, 50.0, 100.0, 100.0)))
        assertEquals(AG.Scissor(0.0, 0.0, 0.0, 0.0), AG.Scissor.combine(AG.Scissor(2000.0, 2000.0, 100.0, 100.0), AG.Scissor(50.0, 50.0, 100.0, 100.0)))
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
