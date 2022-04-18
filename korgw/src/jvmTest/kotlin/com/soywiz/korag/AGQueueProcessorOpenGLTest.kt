package com.soywiz.korag

import com.soywiz.kgl.*
import com.soywiz.korag.gl.*
import com.soywiz.korio.annotations.*
import com.soywiz.korio.test.*
import kotlin.test.*

@OptIn(KorIncomplete::class, KorInternal::class)
class AGQueueProcessorOpenGLTest {
    @Test
    fun test() {
        val gl = KmlGlProxyLogToString()
        val global = AGGlobalState()
        val processor = AGQueueProcessorOpenGL(gl, global)
        val list = global.createList()
        list.enable(AGEnable.BLEND)
        val program = list.createProgram(DefaultShaders.PROGRAM_DEBUG)
        list.useProgram(program)
        list.deleteProgram(program)
        list.disable(AGEnable.BLEND)
        list.finish()
        processor.processBlocking(list, -1)
        assertEqualsJvmFileReference("com/soywiz/korag/AGQueueProcessorOpenGLTest.ref", gl.getLogAsString())
    }
}
