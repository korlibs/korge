package com.soywiz.korge.render

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.BitmapSlice
import com.soywiz.korma.geom.Rectangle

data class TextureWithBitmapSlice(
	val texture: Texture,
	val bitmapSlice: BitmapSlice<Bitmap>,
	val scale: Double,
	val bounds: Rectangle
)
