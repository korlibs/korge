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
    val gl = KmlGlProxyLogToString()
    val ag = SimpleAGOpengl(gl)
    val global = AGGlobalState()
    val glGlobal = GLGlobalState(gl, global)
    val processor = AGQueueProcessorOpenGL(gl, glGlobal)
    val list = ag._list

    @Test
    fun test() {
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
        val tex = ag.createTexture().upload(Bitmap32(1, 1, Colors.RED))
        list.bindTexture(tex, AGTextureTargetKind.TEXTURE_2D)
        processor.processBlocking(list, -1)
        ag.contextLost()
        list.bindTexture(tex, AGTextureTargetKind.TEXTURE_2D)
        processor.processBlocking(list, -1)
        ag.commandsSync {  }
        assertEqualsJvmFileReference("com/soywiz/korag/AGQueueProcessorContextLost.ref", gl.getLogAsString())
    }
}
