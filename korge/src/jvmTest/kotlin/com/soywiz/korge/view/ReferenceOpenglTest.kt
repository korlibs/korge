package com.soywiz.korge.view

import com.soywiz.korge.testing.*
import com.soywiz.korge.view.vector.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.vector.*
import org.junit.*

class ReferenceOpenglTest {
    @Test
    fun testOpengl() = korgeScreenshotTest(100, 100) {
        image(resourcesVfs["texture.png"].readBitmap().mipmaps())
        assertScreenshot(posterize = 6)
    }

    @Test
    fun testOpenglShapeView() = korgeScreenshotTest(500, 500) {
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
        assertScreenshot()
    }
}
