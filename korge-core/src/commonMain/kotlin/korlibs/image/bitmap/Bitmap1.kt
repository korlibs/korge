package korlibs.image.bitmap

import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.color.RgbaArray
import korlibs.math.*

class Bitmap1(
	width: Int,
	height: Int,
	data: ByteArray = ByteArray((width * height) divCeil 8),
	palette: RgbaArray = RgbaArray(intArrayOf(Colors.TRANSPARENT.value, Colors.WHITE.value))
) : BitmapIndexed(1, width, height, data, palette) {
    companion object {
        fun fromString(str: String, transform: (Char) -> Boolean = { it != '.' && it != ' ' }): Bitmap1 {
            val lines = str.split('\n')
            val height = lines.size
            val width = lines.maxOf { it.length }
            val bitmap = Bitmap1(width, height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    bitmap[x, y] = transform(lines[y].getOrElse(x) { '.' }).toInt()
                }
            }
            return bitmap
        }
    }
	override fun createWithThisFormat(width: Int, height: Int): Bitmap = Bitmap1(width, height, palette = palette)
}

inline fun Bitmap32.toBitmap1(): Bitmap1 = toBitmap1 { it.a >= 0x3F }
inline fun Bitmap32.toBitmap1(func: (value: RGBA) -> Boolean): Bitmap1 {
    val out = Bitmap1(width, height, palette = RgbaArray(intArrayOf(Colors.TRANSPARENT.value, Colors.WHITE.value)))
    var n = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            out[x, y] = if (func(getRgbaAtIndex(n++))) 1 else 0
        }
    }
    return out
}
