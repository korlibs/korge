package korlibs.korge.view

import korlibs.graphics.*
import korlibs.korge.view.BlendMode.Companion.ADD
import korlibs.korge.view.BlendMode.Companion.MULTIPLY
import korlibs.korge.view.BlendMode.Companion.NORMAL
import korlibs.korge.view.property.*
import korlibs.image.color.*

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
    val factors: AGBlending,
    val name: String? = null,
) {
    val _hashCode: Int = factors.hashCode() + name.hashCode() * 7
    override fun hashCode(): Int = _hashCode
    override fun equals(other: Any?): Boolean = (this === other) || (other is BlendMode && this.factors == other.factors && name == other.name)
    override fun toString(): String = name ?: super.toString()

    fun apply(src: RGBAf, dst: RGBAf, out: RGBAf = RGBAf()): RGBAf {
        return factors.apply(src, dst, out)
    }

    fun apply(src: RGBA, dst: RGBA): RGBA {
        return factors.apply(src, dst)
    }


    @Suppress("unused")
    object Provider {
        val ITEMS get() = STANDARD_LIST
    }

    companion object {
        /** Mixes the source and destination colors using the source alpha value */
        val NORMAL = BlendMode(name = "NORMAL", factors = AGBlending.NORMAL_PRE)
        /** Not an actual blending. It is used to indicate that the next non-inherit BlendMode from its ancestors will be used. */
        val INHERIT = NORMAL.copy(name = "INHERIT")
        /** Doesn't blend at all. Just replaces the colors. */
        val NONE = BlendMode(name = "NONE", factors = AGBlending(AGBlendFactor.ONE, AGBlendFactor.ZERO)) // REPLACE
        /** Additive mixing for lighting effects */
        val ADD = BlendMode(name = "ADD", factors = AGBlending.ADD_PRE)

        // Unchecked
        val MULTIPLY = BlendMode(name = "MULTIPLY", factors = AGBlending(AGBlendFactor.DESTINATION_COLOR, AGBlendFactor.ONE_MINUS_SOURCE_ALPHA))
        val SCREEN = BlendMode(name = "SCREEN", factors = AGBlending(AGBlendFactor.ONE, AGBlendFactor.ONE_MINUS_SOURCE_COLOR))

        val ERASE = BlendMode(name = "ERASE", factors = AGBlending(AGBlendFactor.ZERO, AGBlendFactor.ONE_MINUS_SOURCE_ALPHA))
        val MASK = BlendMode(name = "MASK", factors = AGBlending(AGBlendFactor.ZERO, AGBlendFactor.SOURCE_ALPHA))
        val BELOW = BlendMode(name = "BELOW", factors = AGBlending(AGBlendFactor.ONE_MINUS_DESTINATION_ALPHA, AGBlendFactor.DESTINATION_ALPHA))
        val SUBTRACT = BlendMode(
            name = "SUBTRACT", factors = AGBlending(
                AGBlendFactor.SOURCE_ALPHA, AGBlendFactor.DESTINATION_ALPHA,
                AGBlendFactor.ONE, AGBlendFactor.ONE,
                AGBlendEquation.REVERSE_SUBTRACT
            )
        )
        val INVERT = BlendMode(name = "INVERT", factors = AGBlending(AGBlendFactor.ONE_MINUS_DESTINATION_COLOR, AGBlendFactor.ZERO))

        // Unimplemented
        val LIGHTEN = BlendMode(
            name = "LIGHTEN",
            factors = AGBlending(
                AGBlendFactor.SOURCE_ALPHA, AGBlendFactor.DESTINATION_ALPHA,
                AGBlendFactor.ONE, AGBlendFactor.ONE
            )
        )
        val DARKEN = BlendMode(
            name = "DARKEN",
            factors = AGBlending(
                AGBlendFactor.SOURCE_ALPHA, AGBlendFactor.DESTINATION_ALPHA,
                AGBlendFactor.ONE, AGBlendFactor.ONE
            )
        )
        val DIFFERENCE = BlendMode(
            name = "DIFFERENCE",
            factors = AGBlending(
                AGBlendFactor.SOURCE_ALPHA, AGBlendFactor.DESTINATION_ALPHA,
                AGBlendFactor.ONE, AGBlendFactor.ONE
            )
        )
        val ALPHA = BlendMode(
            name = "ALPHA",
            factors = AGBlending(
                AGBlendFactor.SOURCE_ALPHA, AGBlendFactor.DESTINATION_ALPHA,
                AGBlendFactor.ONE, AGBlendFactor.ONE
            )
        )
        val HARDLIGHT = BlendMode(
            name = "HARDLIGHT",
            factors = AGBlending(
                AGBlendFactor.SOURCE_ALPHA, AGBlendFactor.DESTINATION_ALPHA,
                AGBlendFactor.ONE, AGBlendFactor.ONE
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