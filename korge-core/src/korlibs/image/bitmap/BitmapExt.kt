package korlibs.image.bitmap

import korlibs.image.color.*
import korlibs.math.geom.*
import kotlin.math.*

// -1 if dimensions do not match
fun Bitmap.matchContentsDistinctCount(that: Bitmap): Int {
	if (this.width != that.width || this.height != that.height) return -1
	val l = this.toBMP32()
	val r = that.toBMP32()
	val width = l.width
	val height = l.height
    var rdiff = 0
    var gdiff = 0
    var bdiff = 0
    var adiff = 0
	for (y in 0 until height) {
		for (x in 0 until width) {
            val rgba1 = l.getRgbaPremultiplied(x, y)
            val rgba2 = r.getRgbaPremultiplied(x, y)
            rdiff += (rgba1.r - rgba2.r).absoluteValue
            gdiff += (rgba1.g - rgba2.g).absoluteValue
            bdiff += (rgba1.b - rgba2.b).absoluteValue
            adiff += (rgba1.a - rgba2.a).absoluteValue
		}
	}
	return rdiff + gdiff + bdiff + adiff
}

fun Bitmap.matchContents(that: Bitmap): Boolean = matchContentsDistinctCount(that) == 0

fun Bitmap32.setAlpha(value: Int) {
	for (n in 0 until this.ints.size) this.ints[n] = RGBA(RGBA(this.ints[n]).rgb, value).value
}

fun <T : Bitmap> T.putWithBorder(x: Int, y: Int, bmp: T, border: Int = 1) {
    return putSliceWithBorder(x, y, bmp.slice(), border)
}

fun Bitmap.putSliceWithBorder(x: Int, y: Int, bmp: BmpSlice, border: Int = 1) {
    val width = bmp.width
    val height = bmp.height

    // Block copy
    bmp.bmp.copy(bmp.left, bmp.top, this, x, y, width, height)

    // Horizontal replicate
    for (n in 1..border) {
        this.copy(x, y, this, x - n, y, 1, height)
        this.copy(x + width - 1, y, this, x + width - 1 + n, y, 1, height)
    }
    // Vertical replicate
    for (n in 1..border) {
        val rwidth = width + border * 2
        this.copy(x - border, y, this, x - border, y - n, rwidth, 1)
        this.copy(x - border, y + height - 1, this, x - border, y + height - 1 + n, rwidth, 1)
    }
}

fun Bitmap.resized(out: Bitmap, scale: ScaleMode, anchor: Anchor): Bitmap {
    val bmp = this
    val width = out.width
    val height = out.height
    out.context2d(antialiased = true) {
        val rect = Rectangle(0, 0, width, height).place(bmp.size.toFloat(), anchor, scale)
        drawImage(bmp, rect.position, rect.size)
    }
    return out
}


fun Bitmap.resized(width: Int, height: Int, scale: ScaleMode, anchor: Anchor, native: Boolean = true): Bitmap =
    resized(if (native) NativeImage(width, height) else createWithThisFormat(width, height), scale, anchor)

fun Bitmap.resizedUpTo(width: Int, height: Int, native: Boolean = true): Bitmap {
    val rect = Rectangle(0, 0, width, height)
        .place(this.size.toFloat(), Anchor.TOP_LEFT, ScaleMode.FIT)
    return resized(rect.width.toInt(), rect.height.toInt(), ScaleMode.FILL, Anchor.TOP_LEFT, native)
}
