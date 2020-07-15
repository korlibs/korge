package com.soywiz.korim.bitmap

import com.soywiz.kmem.*
import com.soywiz.korim.color.*

class Bitmap1(
	width: Int,
	height: Int,
	data: ByteArray = ByteArray((width * height) divCeil 8),
	palette: RgbaArray = RgbaArray(2)
) : BitmapIndexed(1, width, height, data, palette) {
	override fun createWithThisFormat(width: Int, height: Int): Bitmap = Bitmap1(width, height, palette = palette)
}
