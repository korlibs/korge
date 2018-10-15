package com.soywiz.korge.bitmapfont

import com.soywiz.korge.html.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korma.*
import com.soywiz.korma.geom.*
import kotlin.math.*

fun BitmapFont.getBounds(text: String, format: Html.Format, out: Rectangle) {
	//val font = getBitmapFont(format.computedFace, format.computedSize)
	val font = this
	val scale = format.computedSize.toDouble() / font.fontSize.toDouble()
	var width = 0.0
	var height = 0.0
	var dy = 0.0
	var dx = 0.0
	for (n in 0 until text.length) {
		val c1 = text[n].toInt()
		if (c1 == '\n'.toInt()) {
			dx = 0.0
			dy += font.fontSize
			height = max(height, dy)
			continue
		}
		val c2 = text.getOrElse(n + 1) { ' ' }.toInt()
		val kerningOffset = font.kernings[BitmapFont.Kerning.buildKey(c1, c2)]?.amount ?: 0
		val glyph = font[c1]
		dx += glyph.xadvance + kerningOffset
		width = max(width, dx)
	}
	height += font.fontSize
	out.setTo(0.0, 0.0, width * scale, height * scale)
}

fun BitmapFont.drawText(
	ctx: RenderContext,
	textSize: Double,
	str: String,
	x: Int,
	y: Int,
	m: Matrix2d = Matrix2d(),
	colMul: RGBA = Colors.WHITE,
	colAdd: Int = 0x7f7f7f7f,
	blendMode: BlendMode = BlendMode.INHERIT,
	filtering: Boolean = true
) {
	val m2 = m.clone()
	val scale = textSize / fontSize.toDouble()
	m2.pretranslate(x.toDouble(), y.toDouble())
	m2.prescale(scale, scale)
	var dx = 0
	var dy = 0
	for (n in str.indices) {
		val c1 = str[n].toInt()
		if (c1 == '\n'.toInt()) {
			dx = 0
			dy += fontSize
			continue
		}
		val c2 = str.getOrElse(n + 1) { ' ' }.toInt()
		val glyph = this[c1]
		val tex = glyph.texture
		ctx.batch.drawQuad(
			ctx.getTex(tex),
			(dx + glyph.xoffset).toFloat(),
			(dy + glyph.yoffset).toFloat(),
			m = m2,
			colorMulInt = colMul.rgba,
			colorAdd = colAdd,
			blendFactors = blendMode.factors,
			filtering = filtering
		)
		val kerningOffset = kernings[BitmapFont.Kerning.buildKey(c1, c2)]?.amount ?: 0
		dx += glyph.xadvance + kerningOffset
	}
}

fun RenderContext.drawText(
	font: BitmapFont,
	textSize: Double,
	str: String,
	x: Int,
	y: Int,
	m: Matrix2d = com.soywiz.korma.Matrix2d(),
	colMul: RGBA = com.soywiz.korim.color.Colors.WHITE,
	colAdd: Int = 0x7f7f7f7f,
	blendMode: BlendMode = BlendMode.INHERIT,
	filtering: Boolean = true
) {
	font.drawText(this, textSize, str, x, y, m, colMul, colAdd, blendMode, filtering)
}

