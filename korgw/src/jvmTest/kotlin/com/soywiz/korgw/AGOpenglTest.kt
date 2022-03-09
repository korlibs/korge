package com.soywiz.korgw

import com.soywiz.kgl.*
import com.soywiz.korag.*
import kotlin.test.*

class AGOpenglTest {
    @Test
    fun testClear() {
        val proxy = KmlGlProxyLogToString()
        val ag = SimpleAGOpengl(proxy)
        ag.clear()
        assertEquals(
            """
                colorMask([true, true, true, true])
                clearColor([0.0, 0.0, 0.0, 0.0])
                depthMask([true])
                clearDepthf([1.0])
                stencilMask([-1])
                clearStencil([0])
                clear([17664])
            """.trimIndent(),
            ag.gl.log.joinToString("\n")
        )
    }
}
