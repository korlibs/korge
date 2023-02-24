package com.soywiz.kgl

import com.soywiz.kmem.*
import kotlin.test.*

class KglOffScreenTest {
    @Test
    @Ignore
    fun test() {
        if (Platform.isJsNodeJs) return
        if (Platform.isIos) return
        if (Platform.isAndroid) return
        if (Platform.isNative) return
        if (Platform.isLinux) return

        KmlGlContextDefaultTemp { gl ->
            fun <T> T.check(name: String): T {
                val error = gl.getError()
                if (error != 0) error("$name: $error")
                return this
            }

            val fboWidth = 10
            val fboHeight = 10

            // Create and set texture

            //gl.activeTexture(gl.TEXTURE0).check("activeTexture")
            //gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR).check("texParameteri:MIN")
            //gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR).check("texParameteri:MAG")
            //gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE).check("texParameteri:WRAP_S")
            //gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE).check("texParameteri:WRAP_T")

            val fbTex = gl.genTexture().check("genTexture")
            gl.bindTexture(gl.TEXTURE_2D, fbTex).check("bindTexture")

            gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, fboWidth, fboHeight, 0, gl.RGBA, gl.UNSIGNED_BYTE, null).check("texImage2D")
            gl.bindTexture(gl.TEXTURE_2D, 0).check("bindTexture:null")

            val fb = gl.genFramebuffer().check("genFramebuffer")
            gl.bindFramebuffer(gl.FRAMEBUFFER, fb).check("bindFrameBuffer")
            gl.framebufferTexture2D(gl.FRAMEBUFFER, gl.COLOR_ATTACHMENT0, gl.TEXTURE_2D, fbTex, 0).check("framebufferTexture2D")
            val status = gl.checkFramebufferStatus(gl.FRAMEBUFFER).check("checkFramebufferStatus")
            println("status=$status")
            //assertEquals(-9999, status)

            gl.clearColor(1f, 0f, 0f, 1f).check("clearColor")
            gl.clear(KmlGl.COLOR_BUFFER_BIT).check("clear")
            //assertEquals("LOL", gl.getString(KmlGl.VENDOR))
            //println(gl.getString(KmlGl.VENDOR).check("getString:VENDOR"))
            //println(gl.getString(KmlGl.VERSION).check("getString:VERSION"))
            //println(gl.getString(KmlGl.RENDERER).check("getString:RENDERER"))
            //println(gl.getString(KmlGl.SHADING_LANGUAGE_VERSION).check("getString:SHADING_LANGUAGE_VERSION"))
            //println(gl.getString(KmlGl.EXTENSIONS).check("getString:EXTENSIONS"))
            val width = 4
            val height = 4
            val data = Buffer.allocDirect(width * height * 4)
            gl.readPixels(0, 0, width, height, KmlGl.RGBA, KmlGl.UNSIGNED_BYTE, data).check("readPixels")
            assertEquals(
                "ff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ff",
                data.hex()
            )
        }
    }
}
