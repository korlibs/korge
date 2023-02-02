package com.soywiz.korge.test

import com.soywiz.korge.testing.*
import com.soywiz.korge.view.*
import com.soywiz.korim.font.*
import com.soywiz.korio.file.std.*
import org.junit.Test

class TtfFontTest {

    @Test
    fun disableLigaturesWorks(): Unit = korgeScreenshotTest() {
        val ttfFontWithLigatures =
            resourcesVfs["font_atkinson/AtkinsonHyperlegible-Bold.ttf"].readTtfFont()
        val ttfFontWithoutLigatures =
            resourcesVfs["font_atkinson/AtkinsonHyperlegible-Bold.ttf"].readTtfFont(enableLigatures = false)

        val c = container {
            val t1 = text("41/41", font = ttfFontWithLigatures, textSize = 40.0) {
            }
            text("41/41", font = ttfFontWithoutLigatures, textSize = 40.0) {
                alignTopToBottomOf(t1)
            }
        }

        it.recordGolden(c, "text")

        it.endTest()
    }
}
