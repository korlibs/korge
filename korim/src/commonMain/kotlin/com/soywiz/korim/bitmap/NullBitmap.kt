package com.soywiz.korim.bitmap

open class NullBitmap(
    width: Int,
    height: Int,
    premultiplied: Boolean = true
) : Bitmap(width, height, 32, premultiplied, null)


