package com.soywiz.korag

import com.soywiz.kgl.*
import com.soywiz.korio.annotations.*
import com.soywiz.korio.test.*
import kotlin.test.*

@OptIn(KorIncomplete::class, KorInternal::class)
class AGQueueProcessorOpenGLTest {
    @Test
    fun test() {
        val gl = KmlGlProxyLogToString()
        val processor = AGQueueProcessorOpenGL(gl)
        val global = AGGlobalState()
        val list = global.createList()
        list.enable(AGEnable.BLEND)
        list.disable(AGEnable.BLEND)
        processor.processBlocking(list, -1)
        assertEqualsJvmFileReference("com/soywiz/korag/AGQueueProcessorOpenGLTest.ref", gl.getLogAsString())
    }
}
