package com.soywiz.korim.bitmap

import com.soywiz.korim.color.*

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
	val width = bmp.width
	val height = bmp.height

	// Block copy
	bmp.copy(0, 0, this, x, y, width, height)

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
