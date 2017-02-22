package com.soywiz.korge.view

import com.soywiz.korge.bitmapfont.BitmapFont
import com.soywiz.korge.render.RenderContext

class Text(views: Views, val font: BitmapFont, var text: String, var textSize: Double = 16.0) : View(views) {
    override fun render(ctx: RenderContext) {
        font.drawText(ctx.batch, textSize, text, 0, 0, globalMatrix)
    }
}

fun Views.text(font: BitmapFont, text: String, textSize: Double = 16.0) = Text(this, font, text, textSize)

fun Container.text(font: BitmapFont, text: String, textSize: Double = 16.0): Text = text(font, text, textSize) { }

inline fun Container.text(font: BitmapFont, text: String, textSize: Double = 16.0, callback: Text.() -> Unit): Text {
    val child = views.text(font, text, textSize)
    this += child
    callback(child)
    return child
}
