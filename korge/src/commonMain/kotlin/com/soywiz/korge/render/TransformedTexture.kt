package com.soywiz.korge.render

import com.soywiz.korge.internal.*
import com.soywiz.korim.bitmap.*

/**
 * A [BmpSlice] wrap with information about trimming and rotation (useful for packed atlases).
 */
@KorgeInternal
class TransformedTexture(
	val texture: BmpSlice,
	val trimLeft: Float = 0f,
	val trimTop: Float = 0f,
	val rotated: Boolean = false
)
