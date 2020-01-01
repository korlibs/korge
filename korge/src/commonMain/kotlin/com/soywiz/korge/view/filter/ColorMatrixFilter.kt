package com.soywiz.korge.view.filter

import com.soywiz.korag.shader.*
import com.soywiz.korge.view.*
import com.soywiz.korma.geom.*

/**
 * A [Filter] applying a complex color transformation to the view.
 *
 * [colorMatrix] is a 4x4 Matrix used to multiply each color as floating points by this matrix.
 * [blendRatio] is the ratio that will be used to interpolate the original color with the transformed color.
 *
 * ColorMatrixFilter provides a few pre-baked matrices:
 * - [ColorMatrixFilter.GRAYSCALE_MATRIX] - Used to make the colors grey
 * - [ColorMatrixFilter.IDENTITY_MATRIX]  - Doesn't modify the colors at all
 */
class ColorMatrixFilter(colorMatrix: Matrix3D, blendRatio: Double = 1.0) : ShaderFilter() {
	companion object {
		private val u_ColorMatrix = Uniform("colorMatrix", VarType.Mat4)
		private val u_BlendRatio = Uniform("blendRatio", VarType.Float1)

        /** A Matrix usable for [colorMatrix] that will transform any color into grayscale */
		val GRAYSCALE_MATRIX = Matrix3D.fromRows(
			0.33f, 0.33f, 0.33f, 0f,
			0.59f, 0.59f, 0.59f, 0f,
			0.11f, 0.11f, 0.11f, 0f,
			0f, 0f, 0f, 1f
		)

        /** A Matrix usable for [colorMatrix] that will preserve the original color */
		val IDENTITY_MATRIX = Matrix3D.fromRows(
			1f, 0f, 0f, 0f,
			0f, 1f, 0f, 0f,
			0f, 0f, 1f, 0f,
			0f, 0f, 0f, 1f
		)
	}

    /** The 4x4 [Matrix3D] that will be used for transforming each pixel components [r, g, b, a] */
	var colorMatrix by uniforms.storageForMatrix3D(u_ColorMatrix, colorMatrix)

    /**
     * Ratio for blending the original color with the transformed color.
     * - 0 will return the original color
     * - 1 will return the transformed color
     * - Values between [0 and 1] would be an interpolation between those colors.
     * */
	var blendRatio by uniforms.storageFor(u_BlendRatio).doubleDelegateX(blendRatio)

	override val fragment = FragmentShader {
        apply {
            out setTo tex(fragmentCoords)
            out setTo mix(out, (u_ColorMatrix * out), u_BlendRatio)
        }
    }
}
