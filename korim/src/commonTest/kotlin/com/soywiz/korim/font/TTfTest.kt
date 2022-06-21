package com.soywiz.korim.font

import com.soywiz.korim.format.showImageAndWait
import com.soywiz.korim.vector.SvgBuilder
import com.soywiz.korim.vector.render
import com.soywiz.korim.vector.renderToImage
import com.soywiz.korim.vector.scaled
import com.soywiz.korim.vector.toSvg
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.lang.WChar
import com.soywiz.korio.lang.WString
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
        val colorPath = glyph.colorEntry!!.getColorPath()
        assertEquals(1275, glyph.advanceWidth)
        assertEquals(Rectangle(x=32, y=-256, width=1216, height=1216), glyph.metrics1px.bounds)
        //colorPath.scaled(0.01, 0.01).render().showImageAndWait()
    }
}
