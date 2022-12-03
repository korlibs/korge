package com.soywiz.korag

import com.soywiz.kgl.*
import com.soywiz.korag.gl.*
import com.soywiz.korag.log.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.annotations.KorIncomplete
import com.soywiz.korio.annotations.KorInternal
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(KorIncomplete::class, KorInternal::class)
class AGListTest {
    val global = AGGlobalState()
    val log = KmlGlProxyLogToString()
    val glGlobal = GLGlobalState(log, global)
    val ag = DummyAG()
    val _list = AGList(global)
    fun AGQueueProcessor.sync(list: AGList = _list, block: AGList.() -> Unit) {
        block(list)
        list.listFlush()
        processBlockingAll(list)
    }
    @Test
    fun test() {
        val processor = AGQueueProcessorOpenGL(log, glGlobal)
        val tex = ag.createTexture().upload(Bitmap32(1, 1, Colors.RED))
        processor.sync {
            bindTexture(tex, AGTextureTargetKind.TEXTURE_2D)
        }
        tex.close()
        processor.sync {
            finish()
        }
        assertEquals(
            """
                genTextures(1, [6001])
                bindTexture(3553, 6001)
                texImage2D(3553, 0, 6408, 1, 1, 0, 6408, 5121, Buffer(size=4))
                flush()
                deleteTextures(1, [6001])
            """.trimIndent(),
            log.log.joinToString("\n")
        )
    }
}
