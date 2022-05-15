package com.soywiz.korge.render

import com.soywiz.korge.internal.KorgeInternal
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.BitmapSlice
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korma.geom.Rectangle

/**
 * A [texture] wrap that includes [scale] and [bounds] information.
 * Used internally for atlases.
 */
@KorgeInternal
data class TextureWithBitmapSlice(
	val texture: BmpSlice,
	val bitmapSlice: BitmapSlice<Bitmap>,
	val scale: Double,
	val bounds: Rectangle
)
