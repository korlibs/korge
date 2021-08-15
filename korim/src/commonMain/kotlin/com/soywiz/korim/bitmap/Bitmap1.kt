package com.soywiz.korim.bitmap

import com.soywiz.kmem.*
import com.soywiz.korim.color.*

class Bitmap1(
	width: Int,
	height: Int,
	data: ByteArray = ByteArray((width * height) divCeil 8),
	palette: RgbaArray = RgbaArray(intArrayOf(Colors.TRANSPARENT_BLACK.value, Colors.WHITE.value))
) : BitmapIndexed(1, width, height, data, palette) {
	override fun createWithThisFormat(width: Int, height: Int): Bitmap = Bitmap1(width, height, palette = palette)
}

inline fun Bitmap32.toBitmap1(): Bitmap1 = toBitmap1 { it.a >= 0x3F }
inline fun Bitmap32.toBitmap1(func: (value: RGBA) -> Boolean): Bitmap1 {
    val out = Bitmap1(width, height, palette = RgbaArray(intArrayOf(Colors.TRANSPARENT_BLACK.value, Colors.WHITE.value)))
    var n = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            out[x, y] = if (func(data[n++])) 1 else 0
        }
    }
    return out
}
