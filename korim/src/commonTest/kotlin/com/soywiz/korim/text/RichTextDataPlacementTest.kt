package com.soywiz.korim.text

import com.soywiz.klogger.*
import com.soywiz.korim.font.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class RichTextDataPlacementTest {
    val logger = Logger("RichTextDataPlacementTest")

    @Test
    fun test() = suspendTest {
        //val result = DefaultTtfFont.renderGlyphToBitmap(42.0, 'A'.code, paint = Colors.WHITE, fill = true, border = 1, effect = null)
        //result.bmp.showImageAndWait()

        val fonts = listOf(
            resourcesVfs["fnt/SaniTrixieSans.ttf"].readTtfFont().toLazyBitmapFont(42.0, distanceField = null),
            resourcesVfs["msdf/SaniTrixieSans.json"].readBitmapFont(),
            resourcesVfs["fnt/SaniTrixieSans.fnt"].readBitmapFont()
        )

        val texts = fonts.map { RichTextData("HELLO WORLD", font = it, textSize = 32.0) }

        val placements = texts.map { it.place(MRectangle(0, 0, 300, 100), align = TextAlignment.MIDDLE_CENTER) }

        for (font in fonts) logger.debug { font.naturalFontMetrics }

        for (text in texts) logger.debug { "heights=${text.lines.first().maxHeight}, ${text.lines.first().maxLineHeight}" }
        for (font in fonts) logger.debug { "M: " + font.getGlyph('M') }
        for (placement in placements) {
            logger.debug { "placement: $placement" }
        }
    }
}
