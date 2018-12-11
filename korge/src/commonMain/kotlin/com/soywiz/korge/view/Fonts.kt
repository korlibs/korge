package com.soywiz.korge.view

import com.soywiz.korge.bitmapfont.*
import com.soywiz.korge.html.*
import com.soywiz.korge.scene.*
import com.soywiz.korim.font.*
import com.soywiz.korio.error.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import kotlin.math.*

class Fonts : Html.MetricsProvider {
	val fonts = hashMapOf<String, BitmapFont>()

	fun registerFont(name: String, bmp: BitmapFont) {
		fonts[name.toLowerCase()] = bmp
	}

	fun getBitmapFont(name: String, size: Int): BitmapFont {
		val nameLC = name.toLowerCase()
		if (nameLC !in fonts) {
			registerFont(
				name,
				BitmapFontGenerator.generate(name, min(size, 32), BitmapFontGenerator.LATIN_ALL)
			)
		}
		return fonts[nameLC] ?: defaultFont
	}

	fun getBitmapFont(face: Html.FontFace, size: Int): BitmapFont = when (face) {
		is Html.FontFace.Named -> getBitmapFont(face.name, size)
		is Html.FontFace.Bitmap -> face.font
		else -> invalidOp("Unsupported font face: $face")
	}

	fun getBitmapFont(format: Html.Format): BitmapFont = getBitmapFont(format.computedFace, format.computedSize)

	fun named(name: String) = Html.FontFace.Named(name)

	override fun getBounds(text: String, format: Html.Format, out: Rectangle) =
		getBitmapFont(format.computedFace, format.computedSize).getBounds(text, format, out)

	@NativeThreadLocal
	companion object {
		val fonts = Fonts()
		lateinit var defaultFont: BitmapFont

		suspend fun init() {
			defaultFont = getDebugBmpFontOnce()
		}
	}
}
