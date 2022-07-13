package com.soywiz.korge.view

import com.soywiz.korag.AG

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
data class BlendMode(
    val factors: AG.Blending,
    /** Factors to use when the output has non-premultiplied-alpha, and it is full opaque (typically the final output buffer) */
    val nonPremultipliedFactors: AG.Blending = factors,
    val name: String? = null,
) {
    companion object {
        /** Not an actual blending. It is used to indicate that the next non-inherit BlendMode from its ancestors will be used. */
        val INHERIT = BlendMode(name = "INHERIT", factors = AG.Blending.NORMAL)
        /** Doesn't blend at all. Just replaces the colors. */
        val NONE = BlendMode(name = "NONE", factors = AG.Blending(AG.BlendFactor.ONE, AG.BlendFactor.ZERO)) // REPLACE
        /** Mixes the source and destination colors using the source alpha value */
        val NORMAL = BlendMode(name = "NORMAL", factors = AG.Blending.NORMAL)
        /** Additive mixing for lighting effects */
        val ADD = BlendMode(name = "ADD", factors = AG.Blending.ADD)

        // Unchecked
        val MULTIPLY = BlendMode(name = "MULTIPLY", factors = AG.Blending(AG.BlendFactor.DESTINATION_COLOR, AG.BlendFactor.ONE_MINUS_SOURCE_ALPHA))
        val SCREEN = BlendMode(name = "SCREEN", factors = AG.Blending(AG.BlendFactor.ONE, AG.BlendFactor.ONE_MINUS_SOURCE_COLOR))

        val ERASE = BlendMode(name = "ERASE", factors = AG.Blending(AG.BlendFactor.ZERO, AG.BlendFactor.ONE_MINUS_SOURCE_ALPHA))
        val MASK = BlendMode(name = "MASK", factors = AG.Blending(AG.BlendFactor.ZERO, AG.BlendFactor.SOURCE_ALPHA))
        val BELOW = BlendMode(name = "BELOW", factors = AG.Blending(AG.BlendFactor.ONE_MINUS_DESTINATION_ALPHA, AG.BlendFactor.DESTINATION_ALPHA))
        val SUBTRACT = BlendMode(
            name = "SUBTRACT", factors = AG.Blending(
                AG.BlendFactor.SOURCE_ALPHA, AG.BlendFactor.DESTINATION_ALPHA,
                AG.BlendFactor.ONE, AG.BlendFactor.ONE,
                AG.BlendEquation.REVERSE_SUBTRACT
            )
        )
        val INVERT = BlendMode(name = "INVERT", factors = AG.Blending(AG.BlendFactor.ONE_MINUS_DESTINATION_COLOR, AG.BlendFactor.ZERO))

        // Unimplemented
        val LIGHTEN = BlendMode(
            name = "LIGHTEN",
            factors = AG.Blending(
                AG.BlendFactor.SOURCE_ALPHA, AG.BlendFactor.DESTINATION_ALPHA,
                AG.BlendFactor.ONE, AG.BlendFactor.ONE
            )
        )
        val DARKEN = BlendMode(
            name = "DARKEN",
            factors = AG.Blending(
                AG.BlendFactor.SOURCE_ALPHA, AG.BlendFactor.DESTINATION_ALPHA,
                AG.BlendFactor.ONE, AG.BlendFactor.ONE
            )
        )
        val DIFFERENCE = BlendMode(
            name = "DIFFERENCE",
            factors = AG.Blending(
                AG.BlendFactor.SOURCE_ALPHA, AG.BlendFactor.DESTINATION_ALPHA,
                AG.BlendFactor.ONE, AG.BlendFactor.ONE
            )
        )
        val ALPHA = BlendMode(
            name = "ALPHA",
            factors = AG.Blending(
                AG.BlendFactor.SOURCE_ALPHA, AG.BlendFactor.DESTINATION_ALPHA,
                AG.BlendFactor.ONE, AG.BlendFactor.ONE
            )
        )
        val HARDLIGHT = BlendMode(
            name = "HARDLIGHT",
            factors = AG.Blending(
                AG.BlendFactor.SOURCE_ALPHA, AG.BlendFactor.DESTINATION_ALPHA,
                AG.BlendFactor.ONE, AG.BlendFactor.ONE
            )
        )

        val OVERLAY: BlendMode get() = NORMAL
        val REPLACE: BlendMode get() = NONE

        val BY_ORDINAL: Array<BlendMode> = arrayOf(INHERIT, NONE, NORMAL, ADD, MULTIPLY, SCREEN, ERASE, MASK, BELOW, SUBTRACT, INVERT, LIGHTEN, DARKEN, DIFFERENCE, ALPHA, HARDLIGHT)
        val BY_NAME: Map<String, BlendMode> = BY_ORDINAL.associateBy { it.name!! }
        val STANDARD_LIST: List<BlendMode> = BY_ORDINAL.toList()

        val TO_ORDINAL: Map<BlendMode, Int> = BY_ORDINAL.indices.associateBy { BY_ORDINAL[it] }

        operator fun get(ordinal: Int): BlendMode = BY_ORDINAL.getOrElse(ordinal) { INHERIT }
        operator fun get(name: String): BlendMode = BY_NAME[name.uppercase()] ?: INHERIT


        // https://community.khronos.org/t/blending-mode/34770/4
        //multiply 	a * b
        //screen 	1 - (1 - a) * (1 - b)
        //darken 	min(a, b)
        //lighten 	max(a, b)
        //difference 	abs(a - b)
        //negation 	1 - abs(1 - a - b)
        //exclusion 	a + b - 2 * a * b
        //overlay 	a < .5 ? (2 * a * b) : (1 - 2 * (1 - a) * (1 - b))
        //hard light 	b < .5 ? (2 * a * b) : (1 - 2 * (1 - a) * (1 - b))
        //soft light 	b < .5 ? (2 * a * b + a * a * (1 - 2 * b)) : (sqrt(a) * (2 * b - 1) + (2 * a) * (1 - b))
        //dodge 	a / (1 - b)
        //burn 	1 - (1 - a) / b
    }
}

val BlendMode.ordinal: Int get() = BlendMode.TO_ORDINAL.getOrElse(this) { -1 }
