package com.soywiz.korge.ui

import com.soywiz.kds.*
import com.soywiz.korge.html.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.font.*
import com.soywiz.korim.format.*

val DefaultUIFont by lazy {
    val tex = PNG.decode(DebugBitmapFont.DEBUG_FONT_BYTES).toBMP32().premultiplied().slice()
    val fntAdvance = 7
    val fntWidth = 8
    val fntHeight = 8

    val fntBlockX = 2
    val fntBlockY = 2
    val fntBlockWidth = 12
    val fntBlockHeight = 12

    val bitmapFont = BitmapFont(tex.bmp, fntHeight, fntHeight, fntHeight, (0 until 256).associateWith {
        val x = it % 16
        val y = it / 16
        BitmapFont.Glyph(
            it,
            tex.sliceWithSize(x * fntBlockWidth + fntBlockX, y * fntBlockHeight + fntBlockY, fntWidth, fntHeight),
            0,
            0,
            fntAdvance
        )
    }.toIntMap(), IntMap())

    Html.FontFace.Bitmap(bitmapFont)
}

var View.defaultUIFont: Html.FontFace by defaultElement(DefaultUIFont)
