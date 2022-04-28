package com.soywiz.korgw

import com.soywiz.kgl.*
import com.soywiz.korag.gl.*
import com.soywiz.korio.test.*
import kotlin.test.*

class AGOpenglTest {
    @Test
    fun testClear() {
        val proxy = KmlGlProxyLogToString()
        val ag = SimpleAGOpengl(proxy)
        ag.clear()
        assertEqualsJvmFileReference("SimpleAGOpengl.clear.ref", ag.gl.log.joinToString("\n"))
    }
}
