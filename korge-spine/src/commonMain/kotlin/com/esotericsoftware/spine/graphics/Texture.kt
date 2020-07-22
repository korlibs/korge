package com.esotericsoftware.spine.graphics

import com.soywiz.korim.bitmap.*

class Texture(val bmp: Bitmap) {
    val width: Float = bmp.width.toFloat()
    val height: Float = bmp.height.toFloat()
    fun bind() {}
}
