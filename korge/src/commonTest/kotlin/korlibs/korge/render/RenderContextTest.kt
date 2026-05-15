package korlibs.korge.render

import korlibs.kgl.*
import korlibs.graphics.gl.*
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
                activeTexture(33984)
                texParameteri(3553, 33084, 0)
                texParameteri(3553, 33085, 0)
                doRender
                viewport(0, 0, 128, 128)
                bindFramebuffer(36160, 0)
            """.trimIndent(),
            gl.getLogAsString()
        )
    }
}
