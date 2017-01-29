package com.soywiz.korge.view

import com.soywiz.korge.bitmapfont.BitmapFont
import com.soywiz.korge.render.RenderContext

class Text(views: Views, val font: BitmapFont, var text: String) : View(views) {
    override fun render(ctx: RenderContext) {
        font.drawText(ctx.batch, text, 0, 0, globalMatrix)
    }
}

fun Views.text(font: BitmapFont, text: String) = Text(this, font, text)
