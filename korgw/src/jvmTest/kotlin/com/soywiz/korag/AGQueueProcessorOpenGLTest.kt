package com.soywiz.korag

import com.soywiz.kgl.*
import com.soywiz.korag.gl.*
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.Colors
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

    @Test
    fun testContextLost() {
        val gl = KmlGlProxyLogToString()
        val ag = SimpleAGOpengl(gl)
        val tex = ag.createTexture()
        tex.upload(Bitmap32(1, 1, Colors.RED))
        tex.bindEnsuring()
        ag.contextLost()
        tex.bindEnsuring()
        ag.commandsSync {  }
        assertEqualsJvmFileReference("com/soywiz/korag/AGQueueProcessorContextLost.ref", gl.getLogAsString())
    }
}
