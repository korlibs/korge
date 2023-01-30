package com.soywiz.korim.font

import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals

class TTfTest {
    @Test
    fun test() = suspendTest {
        val font = resourcesVfs["twemoji-glyf_colr_1.ttf"].readTtfFont()
        val wstr = WString("😀👩🏽‍🦳👨🏻‍🦳")
        val glyph = font[wstr.codePointAt(0)]!!
        //println("ADVANCE:" + glyph.advanceWidth)
        //println("BOUNDS:" + glyph.metrics1px.bounds)
        val colorPath = glyph.colorEntry!!.getColorShape()
        assertEquals(1275, glyph.advanceWidth)
        assertEquals(
            Rectangle(x = 32, y = -256, width = 1216, height = 1216),
            glyph.metrics1px.bounds
        )
        //colorPath.scaled(0.01, 0.01).render().showImageAndWait()
    }

    @Test
    fun testLigatureAdvancementBug() = suspendTest {
        val font = resourcesVfs["PlayfairDisplay-BoldItalic.ttf"].readTtfFont()
        fun res(str: String, reader: Boolean = true): Pair<Double, Int> {
            val rreader = WStringReader(str)
            val res = font.getGlyphMetrics(
                100.0,
                rreader.peek().codePoint,
                reader = if (reader) rreader else null
            )
            return res.xadvance to rreader.position
        }

        res("s", reader = false).also { (advance, pos) ->
            assertEquals(42.0, advance, absoluteTolerance = 0.1)
            assertEquals(0, pos)
        }
        res("t", reader = false).also { (advance, pos) ->
            assertEquals(35.3, advance, absoluteTolerance = 0.1)
            assertEquals(0, pos)
        }
        res("st", reader = true).also { (advance, pos) ->
            assertEquals(76.4, advance, absoluteTolerance = 0.1)
            assertEquals(2, pos)
        }
    }

    @Test
    fun ligaturesEnabledWorks() = suspendTest {
        val ttfFontWithLigatures =
            resourcesVfs["font_atkinson/AtkinsonHyperlegible-Bold.ttf"].readTtfFont()

        val reader = WStringReader("1/4")

        val glyphs = mutableListOf<BaseTtfFont.Glyph>()

        while (reader.hasMore) {
            val c = reader.peek().codePoint
            val g = ttfFontWithLigatures.getGlyphByReader(reader, c)!!
            glyphs.add(g)
        }

        // The string parses to {'¼'}
        assertEquals(glyphs.size, 1)
    }

    @Test
    fun ligaturesDisabledWorks() = suspendTest {
        val ttfFontWithoutLigatures =
            resourcesVfs["font_atkinson/AtkinsonHyperlegible-Bold.ttf"].readTtfFont(enableLigatures = false)

        val reader = WStringReader("1/4")

        val glyphs = mutableListOf<BaseTtfFont.Glyph>()

        while (reader.hasMore) {
            val c = reader.peek().codePoint
            val g = ttfFontWithoutLigatures.getGlyphByReader(reader, c)!!
            glyphs.add(g)
        }

        // The string parses to {'1', '/', '4'}
        assertEquals(glyphs.size, 3)
    }
}
