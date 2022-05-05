package com.soywiz.korim.bitmap

open class MultiBitmap(
    width: Int,
    height: Int,
    val bitmaps: List<Bitmap>,
    premultiplied: Boolean = true
) : Bitmap(width, height, 32, premultiplied, null) {

}

