package com.soywiz.korge.view

import com.soywiz.korag.*

/**
 * Determines how pixels should be blended. The most common blend modes are: [NORMAL] (normal mix) and [ADD] (additive blending) along with [MULTIPLY] and others.
 *
 * [https://developer.mozilla.org/en-US/docs/Web/API/WebGLRenderingContext/blendFuncSeparate](https://developer.mozilla.org/en-US/docs/Web/API/WebGLRenderingContext/blendFuncSeparate)
 *
 * ```kotlin
 * // color(RGB) = (sourceColor * srcRGB) + (destinationColor * dstRGB)
 * // color(A) = (sourceAlpha * srcAlpha) + (destinationAlpha * dstAlpha)
 * ```
 */
enum class BlendMode(val factors: AG.Blending) {
    /** Not an actual blending. It is used to indicate that the next non-inherit BlendMode from its ancestors will be used. */
	INHERIT(AG.Blending.NORMAL),
    /** Doesn't blend at all. Just replaces the colors. */
	NONE(AG.Blending(AG.BlendFactor.ONE, AG.BlendFactor.ZERO)), // REPLACE
    /** Mixes the source and destination colors using the source alpha value */
	NORMAL(AG.Blending.NORMAL),
    /** Additive mixing for lighting effects */
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

		val BY_ORDINAL = values().associateBy { it.ordinal }
        val BY_NAME = values().associateBy { it.name }

        operator fun get(ordinal: Int) = BY_ORDINAL.getOrElse(ordinal) { INHERIT }
        operator fun get(name: String) = BY_NAME[name.toUpperCase()] ?: INHERIT
	}
}
