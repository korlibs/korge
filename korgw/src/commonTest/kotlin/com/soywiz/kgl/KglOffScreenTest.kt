package com.soywiz.kgl

import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korim.color.*
import kotlin.test.*

class KglOffScreenTest {
    @Test
    fun test() {
        if (Platform.isJsNodeJs) return
        if (Platform.isIos) return
        if (Platform.isAndroid) return

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

    @Test
    fun testNAG() {
        run {
            val log = KmlGlProxyLogToString()
            val ag = NAGOpengl(log)
            val fb = NAGFrameBuffer().set(10, 10)
            ag.clear(fb, Colors.RED, 1f, 0)
            val bytes = ByteArray(4 * 4 * 4)
            val buffer = Buffer(bytes)
            ag.readBits(fb, AGReadKind.COLOR, 0, 0, 4, 4, bytes)
            println(log.log.joinToString("\n"))
        }

        KmlGlContextDefaultTemp { gl ->
            val ag = NAGOpengl(gl)
            val fb = NAGFrameBuffer().set(10, 10)
            //val fb: NAGFrameBuffer? = null
            ag.clear(fb, Colors.RED, 1f, 0)
            val buffer = Buffer(4 * 4 * 4)
            ag.readBits(fb, AGReadKind.COLOR, 0, 0, 4, 4, buffer)
            assertEquals(
                "ff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ff",
                buffer.hex()
            )
        }
    }

    @Test
    fun testNAG2() {
        KmlGlContextDefaultTemp { gl ->
            val rgl = KmlGlProxyLogToString(gl)
            val ag = NAGOpengl(rgl)
            val fb = NAGFrameBuffer().set(4, 4)
            rgl.viewport(0, 0, 4, 4)
            //val fb: NAGFrameBuffer? = null
            val vertexData = NAGBuffer()
            vertexData.upload(Float32Buffer(floatArrayOf(
                -10f, -10f,
                +10f, -10f,
                -10f, +10f,
            )).buffer)
            //val fb: NAGFrameBuffer? = null
            ag.startFrame()
            ag.clear(fb, Colors["#1f1f1f"], 1f, 0)
            ag.draw(
                NAGBatch(
                    vertexData = NAGVertices(NAGVerticesPart(DefaultShaders.LAYOUT_DEBUG, vertexData)),
                    //indexData = NAGBuffer().upload(Int16Buffer(shortArrayOf(0, 1, 2, 1, 2, 0)).buffer),
                    batches = listOf(
                        NAGUniformBatch(
                            frameBuffer = fb,
                            program = DefaultShaders.PROGRAM_DEBUG,
                            state = AGFullState(),
                            drawCommands = NAGDrawCommandArray.invoke {
                                it.add(AGDrawType.TRIANGLES, AGIndexType.NONE, 0, 3)
                                //it.add(AGDrawType.TRIANGLES, AGIndexType.USHORT, 2, 3)
                            }
                        )
                    )
                )
            )
            val buffer = Buffer(4 * 4 * 4)
            ag.readBits(fb, AGReadKind.COLOR, 0, 0, 4, 4, buffer)
            println(rgl.log.joinToString("\n"))
            println(buffer.hex())
            /*
            assertEquals(
                "ff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ffff0000ff",
                buffer.hex()
            )
            */
        }

    }
}

