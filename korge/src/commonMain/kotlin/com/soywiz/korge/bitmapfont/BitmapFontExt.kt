package com.soywiz.korge.bitmapfont

import com.soywiz.korge.html.Html
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.view.BlendMode
import com.soywiz.korim.color.ColorAdd
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.BitmapFont
import com.soywiz.korim.font.Font
import com.soywiz.korim.font.GlyphMetrics
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Rectangle
import kotlin.math.max

fun Font.getBounds(text: String, format: Html.Format, out: Rectangle = Rectangle()): Rectangle {
	//val font = getBitmapFont(format.computedFace, format.computedSize)
	val font = this
    val textSize = format.computedSize.toDouble()
	var width = 0.0
	var height = 0.0
	var dy = 0.0
	var dx = 0.0
    val glyph = GlyphMetrics()
    val fmetrics = font.getFontMetrics(textSize)
	for (n in 0 until text.length) {
		val c1 = text[n].toInt()
		if (c1 == '\n'.toInt()) {
			dx = 0.0
			dy += fmetrics.lineHeight
			height = max(height, dy)
			continue
		}
		var c2: Int = ' '.toInt()
		if (n + 1 < text.length) c2 = text[n + 1].toInt()
		val kerningOffset = font.getKerning(textSize, c1, c2)
		val glyph = font.getGlyphMetrics(textSize, c1, glyph)
		dx += glyph.xadvance + kerningOffset
		width = max(width, dx)
	}
	height += fmetrics.lineHeight
    //val scale = textSize / font.fontSize.toDouble()
	//out.setTo(0.0, 0.0, width * scale, height * scale)
    out.setTo(0.0, 0.0, width, height)
    return out
}

fun BitmapFont.drawText(
	ctx: RenderContext,
	textSize: Double,
	str: String,
	x: Int,
	y: Int,
	m: Matrix = Matrix(),
	colMul: RGBA = Colors.WHITE,
	colAdd: ColorAdd = ColorAdd.NEUTRAL,
	blendMode: BlendMode = BlendMode.INHERIT,
	filtering: Boolean = true
) {
	val m2 = m.clone()
	val scale = textSize / fontSize.toDouble()
	m2.pretranslate(x.toDouble(), y.toDouble())
	m2.prescale(scale, scale)
	var dx = 0.0
	var dy = 0.0
    ctx.useBatcher { batch ->
        for (n in str.indices) {
            val c1 = str[n].toInt()
            if (c1 == '\n'.toInt()) {
                dx = 0.0
                dy += fontSize
                continue
            }
            val c2 = str.getOrElse(n + 1) { ' ' }.toInt()
            val glyph = this[c1]
            val tex = glyph.texture
            batch.drawQuad(
                ctx.getTex(tex),
                (dx + glyph.xoffset).toFloat(),
                (dy + glyph.yoffset).toFloat(),
                tex.width.toFloat(),
                tex.height.toFloat(),
                m = m2,
                colorMul = colMul,
                colorAdd = colAdd,
                blendMode = blendMode,
                filtering = filtering,
                premultiplied = tex.base.premultiplied,
                wrap = false,
            )
            val kerningOffset = kernings[BitmapFont.Kerning.buildKey(c1, c2)]?.amount ?: 0
            dx += glyph.xadvance + kerningOffset
        }
    }
}

fun RenderContext.drawText(
	font: BitmapFont,
	textSize: Double,
	str: String,
	x: Int,
	y: Int,
	m: Matrix = Matrix(),
	colMul: RGBA = Colors.WHITE,
	colAdd: ColorAdd = ColorAdd.NEUTRAL,
	blendMode: BlendMode = BlendMode.INHERIT,
	filtering: Boolean = true
) {
	font.drawText(this, textSize, str, x, y, m, colMul, colAdd, blendMode, filtering)
}
