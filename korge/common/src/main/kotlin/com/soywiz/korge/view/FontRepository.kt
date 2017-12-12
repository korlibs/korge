package com.soywiz.korge.view

import com.soywiz.korge.bitmapfont.BitmapFont
import com.soywiz.korge.bitmapfont.convert
import com.soywiz.korge.html.Html
import com.soywiz.korim.font.BitmapFontGenerator
import com.soywiz.korio.error.invalidOp
import com.soywiz.korma.geom.Rectangle
import kotlin.math.max
import kotlin.math.min

class FontRepository(val views: Views) : Html.MetricsProvider {
	val fonts = hashMapOf<String, BitmapFont>()

	fun registerFont(name: String, bmp: BitmapFont) {
		fonts[name.toLowerCase()] = bmp
	}

	fun getBitmapFont(name: String, size: Int): BitmapFont {
		val nameLC = name.toLowerCase()
		if (nameLC !in fonts) {
			registerFont(name, BitmapFontGenerator.generate(name, min(size, 32), BitmapFontGenerator.LATIN_ALL).convert(views.ag))
		}
		return fonts[nameLC] ?: views.defaultFont
	}

	fun getBitmapFont(face: Html.FontFace, size: Int): BitmapFont = when (face) {
		is Html.FontFace.Named -> getBitmapFont(face.name, size)
		is Html.FontFace.Bitmap -> face.font
		else -> invalidOp("Unsupported font face: $face")
	}

	fun getBitmapFont(format: Html.Format): BitmapFont = getBitmapFont(format.computedFace, format.computedSize)

	override fun getBounds(text: String, format: Html.Format, out: Rectangle) {
		val font = getBitmapFont(format.computedFace, format.computedSize)
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
}
