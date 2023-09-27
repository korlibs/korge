package korlibs.render

import korlibs.kgl.*
import korlibs.graphics.*
import korlibs.graphics.gl.*
import korlibs.graphics.shader.*
import korlibs.image.bitmap.*
import korlibs.io.test.*
import kotlin.test.*

class AGOpenglTest {
    @Test
    fun testClear() {
        val gl = KmlGlProxyLogToString()
        val ag = AGOpengl(gl)
        ag.clear(ag.mainFrameBuffer)
        assertEqualsJvmFileReference("SimpleAGOpengl.clear.ref", gl.log.joinToString("\n"))
    }

    @Test
    fun testContextLost() {
        val gl = KmlGlProxyLogToString()
        val ag = AGOpengl(gl)
        val fb = AGFrameBuffer().also { it.setSize(512, 512) }
        val bmp = Bitmap32(16, 16, korlibs.image.color.Colors.RED).premultipliedIfRequired()
        val batch = AGBatch(
            fb.base, fb.info,
            AGVertexArrayObject(
                AGVertexData(VertexLayout(DefaultShaders.a_Pos), AGBuffer().also { it.upload(floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f)) })
            ),
            AGBuffer().also { it.upload(shortArrayOf(0, 1, 2)) },
            AGIndexType.USHORT,
            textureUnits = AGTextureUnits().also {
                it.set(DefaultShaders.u_Tex, AGTexture().also { it.upload(bmp) })
            },
            program = DefaultShaders.PROGRAM_DEBUG,
        )

        fun drawAndLog(contextLost: Boolean = false): String {
            ag.startFrame()
            if (contextLost) ag.contextLost()
            ag.draw(batch)
            ag.finish()
            ag.endFrame()
            return gl.getLogAsString().also { gl.getLogAsString(clear = true) }
        }

        fun genReport(title: String, str: String): String {
            return """
                ## ${title}:
                genBuffer:${Regex("genBuffer").findAll(str).count()}
                createProgram:${Regex("createProgram").findAll(str).count()}
                texImage2D:${Regex("texImage2D").findAll(str).count()}
                genTextures:${Regex("genTextures").findAll(str).count()}
                
            """.trimIndent()
        }

        val reports = arrayListOf<String>()

        drawAndLog(contextLost = false).also { str -> reports += genReport("start", str) }
        drawAndLog(contextLost = false).also { str -> reports += genReport("next", str) }
        drawAndLog(contextLost = true).also { str -> reports += genReport("lost", str) }
        drawAndLog(contextLost = false).also { str -> reports += genReport("lost-next", str) }
        assertEquals(
            """
                ## start:
                genBuffer:2
                createProgram:1
                texImage2D:2
                genTextures:2
                
                ## next:
                genBuffer:0
                createProgram:0
                texImage2D:0
                genTextures:0
                
                ## lost:
                genBuffer:2
                createProgram:1
                texImage2D:2
                genTextures:2
                
                ## lost-next:
                genBuffer:0
                createProgram:0
                texImage2D:0
                genTextures:0
            """.trimIndent().trim(),
            reports.joinToString("\n").trim()
        )
    }
}
