package com.soywiz.korge.test

import com.soywiz.korge.testing.*
import com.soywiz.korge.view.*
import com.soywiz.korim.font.*
import com.soywiz.korio.file.std.*
import org.junit.*

class TtfFontTest {
    @Test
    fun disableLigaturesWorks(): Unit = korgeScreenshotTest {
        val ttfFontWithLigatures =
            resourcesVfs["font_atkinson/AtkinsonHyperlegible-Bold.ttf"].readTtfFont()
        val ttfFontWithoutLigatures =
            resourcesVfs["font_atkinson/AtkinsonHyperlegible-Bold.ttf"].readTtfFont(enableLigatures = false)

        val c = container {
            val t1 = text("41/41", font = ttfFontWithLigatures, textSize = 40.0) {
                this.graphicsRenderer = GraphicsRenderer.CPU
            }
            text("41/41", font = ttfFontWithoutLigatures, textSize = 40.0) {
                this.graphicsRenderer = GraphicsRenderer.CPU
                alignTopToBottomOf(t1)
            }
        }

        assertScreenshot(c, "text", posterize = 5, includeBackground = false)
    }
}
