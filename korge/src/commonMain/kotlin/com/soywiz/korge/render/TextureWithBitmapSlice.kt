package com.soywiz.korge.render

import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.*

data class TextureWithBitmapSlice(
	val texture: BmpSlice,
	val bitmapSlice: BitmapSlice<Bitmap>,
	val scale: Double,
	val bounds: Rectangle
)
