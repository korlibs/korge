package com.soywiz.korim.font

import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.lang.WString
import com.soywiz.korio.lang.WStringReader
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
        assertEquals(Rectangle(x=32, y=-256, width=1216, height=1216), glyph.metrics1px.bounds)
        //colorPath.scaled(0.01, 0.01).render().showImageAndWait()
    }

   @Test
   fun testLigatureAdvancementBug() = suspendTest {
       val font = resourcesVfs["PlayfairDisplay-BoldItalic.ttf"].readTtfFont()
       fun res(str: String, reader: Boolean = true): Pair<Double, Int> {
           val rreader = WStringReader(str)
           val res = font.getGlyphMetrics(100.0, rreader.peek().codePoint, reader = if (reader) rreader else null)
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
}
