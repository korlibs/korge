package com.soywiz.korge.view

import com.soywiz.kgl.*
import com.soywiz.korag.*
import com.soywiz.korag.gl.*
import com.soywiz.korev.*
import com.soywiz.korge.input.*
import com.soywiz.korge.test.*
import com.soywiz.korge.tests.*
import com.soywiz.korge.view.vector.*
import com.soywiz.korgw.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import org.junit.*

class ReferenceOpenglTest : ViewsForTesting() {
    val gl = KmlGlProxyLogToString()
    override fun createAg(): AG = SimpleAGOpengl(gl)

    @Test
    fun testOpengl() = viewsTest {
        image(resourcesVfs["texture.png"].readBitmapOptimized().mipmaps())
        gl.clearLog()
        render(views.renderContext)
        assertEqualsFileReference("korge/render/OpenGL.log", gl.getLogAsString())
    }

    @Test
    fun testOpenglShapeView() = viewsTest {
        container {
            xy(300, 300)
            val shape = gpuShapeView({
                //val lineWidth = 6.12123231 * 2
                val lineWidth = 12.0
                val width = 300.0
                val height = 300.0
                //rotation = 180.degrees
                this.stroke(Colors.WHITE.withAd(0.5), lineWidth = lineWidth, lineJoin = LineJoin.MITER, lineCap = LineCap.BUTT) {
                    this.rect(
                        lineWidth / 2, lineWidth / 2,
                        width,
                        height
                    )
                }
            }) {
                xy(-150, -150)
            }
        }
        gl.clearLog()
        render(views.renderContext)
        assertEqualsFileReference("korge/render/OpenGLShapeView.log", gl.getLogAsString())
    }
}
