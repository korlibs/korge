package com.soywiz.korim.bitmap

import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*

// -1 if dimensions do not match
fun Bitmap.matchContentsDistinctCount(that: Bitmap): Int {
	if (this.width != that.width || this.height != that.height) return -1
	val l = this.toBMP32().depremultipliedIfRequired()
	val r = that.toBMP32().depremultipliedIfRequired()
	val width = l.width
	val height = l.height
	var count = 0
	for (y in 0 until height) {
		for (x in 0 until width) {
			if (l.getRgba(x, y) != r.getRgba(x, y)) count++
		}
	}
	return count
}

fun Bitmap.matchContents(that: Bitmap): Boolean = matchContentsDistinctCount(that) == 0

fun Bitmap32.setAlpha(value: Int) {
	for (n in 0 until this.data.size) this.data[n] = RGBA(this.data[n].rgb, value)
}

fun <T : Bitmap> T.putWithBorder(x: Int, y: Int, bmp: T, border: Int = 1) {
    return putSliceWithBorder(x, y, bmp.slice(), border)
}

fun Bitmap.putSliceWithBorder(x: Int, y: Int, bmp: BmpSlice, border: Int = 1) {
    val width = bmp.width
    val height = bmp.height

    // Block copy
    bmp.bmpBase.copy(bmp.left, bmp.top, this, x, y, width, height)

    // Horizontal replicate
    for (n in 1..border) {
        this.copy(x, y, this, x - n, y, 1, height)
        this.copy(x + width - 1, y, this, x + width - 1 + n, y, 1, height)
    }
    // Vertical replicate
    for (n in 1..border) {
        val rwidth = width + border * 2
        this.copy(x, y, this, x, y - n, rwidth, 1)
        this.copy(x, y + height - 1, this, x, y + height - 1 + n, rwidth, 1)
    }
}

fun Bitmap.resized(out: Bitmap, scale: ScaleMode, anchor: Anchor): Bitmap {
    val bmp = this
    val width = out.width
    val height = out.height
    out.context2d(antialiased = true) {
        val rect = Rectangle(0, 0, width, height).place(bmp.width.toDouble(), bmp.height.toDouble(), anchor, scale)
        drawImage(bmp, rect.x, rect.y, rect.width, rect.height)
    }
    return out
}


fun Bitmap.resized(width: Int, height: Int, scale: ScaleMode, anchor: Anchor, native: Boolean = true): Bitmap =
    resized(if (native) NativeImage(width, height) else createWithThisFormat(width, height), scale, anchor)

fun Bitmap.resizedUpTo(width: Int, height: Int, native: Boolean = true): Bitmap {
    val rect = Rectangle(0, 0, width, height)
        .place(this.width.toDouble(), this.height.toDouble(), Anchor.TOP_LEFT, ScaleMode.FIT)
    return resized(rect.width.toInt(), rect.height.toInt(), ScaleMode.FILL, Anchor.TOP_LEFT, native)
}
