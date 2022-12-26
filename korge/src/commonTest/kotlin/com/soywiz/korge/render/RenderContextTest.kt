package com.soywiz.korge.render

import com.soywiz.kgl.*
import com.soywiz.korag.gl.*
import kotlin.test.*

class RenderContextTest {
    @Test
    fun testDoRender() {
        val gl = KmlGlProxyLogToString()
        val ag = AGOpengl(gl)
        val renderContext = RenderContext(ag)
        renderContext.doRender {
            gl.log.add("doRender")
        }
        assertEquals(
            """
                beforeDoRender(0)
                getIntegerv(36006, Buffer(size=4))
                doRender
                viewport(0, 0, 128, 128)
                bindFramebuffer(36160, 0)
            """.trimIndent(),
            gl.getLogAsString()
        )
    }
}
