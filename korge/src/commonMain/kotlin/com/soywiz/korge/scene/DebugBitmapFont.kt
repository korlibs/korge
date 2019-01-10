package com.soywiz.korge.scene

import com.soywiz.kds.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.font.*
import com.soywiz.korim.format.*
import com.soywiz.korio.util.*

object DebugBitmapFont {
	val DEBUG_FONT_BYTES: ByteArray =
		("" +
				"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" +
				"sT1xehsAAAAASUVORK5CYII=")
					.fromBase64()

}

@NativeThreadLocal
private var bmpFontOnce2 = AsyncOnce<BitmapFont>()

suspend fun getDebugBmpFontOnce() = bmpFontOnce2 {
	val tex = nativeImageFormatProvider.decode(DebugBitmapFont.DEBUG_FONT_BYTES).slice()
	val fntAdvance = 7
	val fntWidth = 8
	val fntHeight = 8
	BitmapFont(tex.bmp, fntHeight, fntHeight, fntHeight, (0 until 256).associate {
		val x = it % 16
		val y = it / 16
		it to BitmapFont.Glyph(
			it,
			tex.sliceWithSize(x * fntWidth, y * fntHeight, fntWidth, fntHeight),
			0,
			0,
			fntAdvance
		)
	}.toIntMap(), IntMap())
}
