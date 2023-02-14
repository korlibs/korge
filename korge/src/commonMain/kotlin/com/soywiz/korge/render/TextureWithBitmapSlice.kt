package com.soywiz.korge.render

import com.soywiz.korge.internal.KorgeInternal
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korma.geom.MRectangle

/**
 * A [texture] wrap that includes [scale] and [bounds] information.
 * Used internally for atlases.
 */
@KorgeInternal
data class TextureWithBitmapSlice(
	val texture: BmpSlice,
	val bitmapSlice: BmpSlice,
	val scale: Double,
	val bounds: MRectangle
)
