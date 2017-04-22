package com.soywiz.korge.view

import com.soywiz.korge.bitmapfont.BitmapFont
import com.soywiz.korge.bitmapfont.convert
import com.soywiz.korge.html.Html
import com.soywiz.korim.font.BitmapFontGenerator
import com.soywiz.korio.error.invalidOp
import com.soywiz.korma.geom.Rectangle

class FontRepository(val views: Views) : Html.MetricsProvider {
	val fonts = hashMapOf<String, BitmapFont>()

	fun registerFont(name: String, bmp: BitmapFont) {
		fonts[name.toLowerCase()] = bmp
	}

	fun getBitmapFont(name: String, size: Int): BitmapFont {
		val nameLC = name.toLowerCase()
		if (nameLC !in fonts) {
			registerFont(name, BitmapFontGenerator.generate(name, Math.min(size, 32), BitmapFontGenerator.LATIN_ALL).convert(views.ag))
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
		val scale = font.fontSize.toDouble() / format.computedSize.toDouble()
		val width = text.sumByDouble { font[it].xadvance * scale }
		out.setTo(0.0, 0.0, width, font.fontSize.toDouble())
	}
}
