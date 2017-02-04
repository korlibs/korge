package com.soywiz.korge.view

import com.soywiz.korge.bitmapfont.BitmapFont
import com.soywiz.korge.render.RenderContext

class Text(views: Views, val font: BitmapFont, var text: String, var textSize: Double = 16.0) : View(views) {
    override fun render(ctx: RenderContext) {
        font.drawText(ctx.batch, textSize, text, 0, 0, globalMatrix)
    }
}

fun Views.text(font: BitmapFont, text: String, textSize: Double = 16.0) = Text(this, font, text, textSize)
