package com.soywiz.korge.render

import com.soywiz.korge.internal.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.*

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
