package com.soywiz.korge.view

import com.soywiz.kgl.*
import com.soywiz.korag.*
import com.soywiz.korag.gl.*
import com.soywiz.korge.test.*
import com.soywiz.korge.tests.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
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
}
