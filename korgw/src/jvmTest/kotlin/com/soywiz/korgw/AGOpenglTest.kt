package com.soywiz.korgw

import com.soywiz.kgl.*
import com.soywiz.korag.*
import com.soywiz.korag.gl.*
import com.soywiz.korio.test.*
import kotlin.test.*

class AGOpenglTest {
    @Test
    fun testClear() {
        val gl = KmlGlProxyLogToString()
        val ag = AGOpengl(gl)
        ag.clear(ag.mainFrameBuffer)
        assertEqualsJvmFileReference("SimpleAGOpengl.clear.ref", gl.log.joinToString("\n"))
    }
}
