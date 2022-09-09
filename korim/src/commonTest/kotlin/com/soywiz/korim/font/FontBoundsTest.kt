package com.soywiz.korim.font

import com.soywiz.korim.text.TextAlignment
import kotlin.jvm.JvmStatic
import kotlin.test.Test

class FontBoundsTest {
    @Test
    fun test() {
        val bounds = DefaultTtfFont.getTextBounds(100.0, "Hello\nhi!", align = TextAlignment.TOP_LEFT)
        println(bounds)
        println(bounds.lineBounds)
        val bounds2 = DefaultTtfFont.getTextBoundsWithGlyphs(100.0, "Hello\nhi!", align = TextAlignment.TOP_LEFT)
        println(bounds2.metrics)
        //println(bounds2.glyphs)
        //buildVectorPath {
        //    circle()
        //}.transformPoints()
    }
}
