package com.soywiz.korge.view

import com.soywiz.korag.*

// https://developer.mozilla.org/en-US/docs/Web/API/WebGLRenderingContext/blendFuncSeparate
// color(RGB) = (sourceColor * srcRGB) + (destinationColor * dstRGB)
// color(A) = (sourceAlpha * srcAlpha) + (destinationAlpha * dstAlpha)
enum class BlendMode(val factors: AG.Blending) {
	INHERIT(AG.Blending.NORMAL),
	NONE(AG.Blending(AG.BlendFactor.ONE, AG.BlendFactor.ZERO)), // REPLACE
	NORMAL(AG.Blending.NORMAL),
	ADD(AG.Blending.ADD),

	// Unchecked
	MULTIPLY(AG.Blending(AG.BlendFactor.DESTINATION_COLOR, AG.BlendFactor.ONE_MINUS_SOURCE_ALPHA)),
	SCREEN(AG.Blending(AG.BlendFactor.ONE, AG.BlendFactor.ONE_MINUS_SOURCE_COLOR)),

	ERASE(AG.Blending(AG.BlendFactor.ZERO, AG.BlendFactor.ONE_MINUS_SOURCE_ALPHA)),
	MASK(AG.Blending(AG.BlendFactor.ZERO, AG.BlendFactor.SOURCE_ALPHA)),
	BELOW(AG.Blending(AG.BlendFactor.ONE_MINUS_DESTINATION_ALPHA, AG.BlendFactor.DESTINATION_ALPHA)),
	SUBTRACT(
		AG.Blending(
			AG.BlendFactor.SOURCE_ALPHA, AG.BlendFactor.DESTINATION_ALPHA,
			AG.BlendFactor.ONE, AG.BlendFactor.ONE,
			AG.BlendEquation.REVERSE_SUBTRACT
		)
	),

	// Unimplemented
	LIGHTEN(
		AG.Blending(
			AG.BlendFactor.SOURCE_ALPHA, AG.BlendFactor.DESTINATION_ALPHA,
			AG.BlendFactor.ONE, AG.BlendFactor.ONE
		)
	),
	DARKEN(
		AG.Blending(
			AG.BlendFactor.SOURCE_ALPHA, AG.BlendFactor.DESTINATION_ALPHA,
			AG.BlendFactor.ONE, AG.BlendFactor.ONE
		)
	),
	DIFFERENCE(
		AG.Blending(
			AG.BlendFactor.SOURCE_ALPHA, AG.BlendFactor.DESTINATION_ALPHA,
			AG.BlendFactor.ONE, AG.BlendFactor.ONE
		)
	),
	INVERT(
		AG.Blending(
			AG.BlendFactor.SOURCE_ALPHA, AG.BlendFactor.DESTINATION_ALPHA,
			AG.BlendFactor.ONE, AG.BlendFactor.ONE
		)
	),
	ALPHA(
		AG.Blending(
			AG.BlendFactor.SOURCE_ALPHA, AG.BlendFactor.DESTINATION_ALPHA,
			AG.BlendFactor.ONE, AG.BlendFactor.ONE
		)
	),
	HARDLIGHT(
		AG.Blending(
			AG.BlendFactor.SOURCE_ALPHA, AG.BlendFactor.DESTINATION_ALPHA,
			AG.BlendFactor.ONE, AG.BlendFactor.ONE
		)
	),
	;

	companion object {
		val OVERLAY = NORMAL

		val BY_ORDINAL = values().map { it.ordinal to it }.toMap()
	}
}
