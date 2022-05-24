package com.soywiz.korag

import com.soywiz.kgl.KmlGlDummy
import com.soywiz.kgl.KmlGlProxyLogToString
import com.soywiz.korag.gl.AGQueueProcessorOpenGL
import com.soywiz.korio.annotations.KorIncomplete
import com.soywiz.korio.annotations.KorInternal
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(KorIncomplete::class, KorInternal::class)
class AGListTest {
    val global = AGGlobalState()
    val _list = AGList(global)
    fun AGQueueProcessor.sync(list: AGList = _list, block: AGList.() -> Unit) {
        block(list)
        processBlockingAll(list)
    }
    @Test
    fun test() {
        val log = KmlGlProxyLogToString()
        val processor = AGQueueProcessorOpenGL(log, global)
        processor.sync {
            val tex1 = createTexture()
            deleteTexture(tex1)
            val tex2 = createTexture()
            deleteTexture(tex2)
        }
        processor.sync {
            val tex1 = createTexture()
            deleteTexture(tex1)
            val tex2 = createTexture()
            deleteTexture(tex2)
        }
        assertEquals(
            """
                genTextures(1, [6001])
                deleteTextures(1, [6001])
                genTextures(1, [6001])
                deleteTextures(1, [6001])
                genTextures(1, [6001])
                deleteTextures(1, [6001])
                genTextures(1, [6001])
                deleteTextures(1, [6001])
            """.trimIndent(),
            log.log.joinToString("\n")
        )
    }
}
