package com.soywiz.korge.view

import com.soywiz.korge.bitmapfont.*
import com.soywiz.korge.html.*
import com.soywiz.korge.scene.*
import com.soywiz.korim.font.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import kotlin.collections.contains
import kotlin.collections.hashMapOf
import kotlin.collections.set
import kotlin.math.*

class Fonts : Html.MetricsProvider {
	val fonts = hashMapOf<String, Font>()

	fun registerFont(name: String, bmp: Font) {
		fonts[name.toLowerCase()] = bmp
	}

	fun getBitmapFont(name: String, size: Int): Font {
		val nameLC = name.toLowerCase()
		if (nameLC !in fonts) {
			registerFont(
				name,
				BitmapFont(SystemFont(name), min(size, 32), CharacterSet.LATIN_ALL)
			)
		}
		return fonts[nameLC] ?: defaultFont
	}

	fun getBitmapFont(face: Font, size: Int): Font = face
	fun getBitmapFont(format: Html.Format): Font = getBitmapFont(format.computedFace, format.computedSize)

	fun named(name: String) = SystemFont(name)

	override fun getBounds(text: String, format: Html.Format, out: Rectangle) =
		getBitmapFont(format.computedFace, format.computedSize).getBounds(text, format, out)

	companion object {
		val fonts get() = Fonts_fonts
		val defaultFont: BitmapFont get() = debugBmpFont
	}
}

private val Fonts_fonts: Fonts by lazy { Fonts() }
