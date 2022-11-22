package com.soywiz.korge.view.filter

import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.view.property.*
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
	companion object : BaseProgramProvider() {
		private val u_ColorMatrix = Uniform("colorMatrix", VarType.Mat4)
		private val u_BlendRatio = Uniform("blendRatio", VarType.Float1)

        /** A Matrix usable for [colorMatrix] that will transform any color into grayscale */
		val GRAYSCALE_MATRIX = Matrix3D.fromColumns(
			0.33f, 0.33f, 0.33f, 0f,
			0.59f, 0.59f, 0.59f, 0f,
			0.11f, 0.11f, 0.11f, 0f,
			0f, 0f, 0f, 1f
		)

        val SEPIA_MATRIX = Matrix3D.fromColumns(
            0.393f, 0.349f, 0.272f, 0f,
            0.769f, 0.686f, 0.534f, 0f,
            0.189f, 0.168f, 0.131f, 0f,
            0f, 0f, 0f, 1f
        )

        /** A Matrix usable for [colorMatrix] that will preserve the original color */
		val IDENTITY_MATRIX = Matrix3D.fromColumns(
			1f, 0f, 0f, 0f,
			0f, 1f, 0f, 0f,
			0f, 0f, 1f, 0f,
			0f, 0f, 0f, 1f
		)

        val NAMED_MATRICES = mapOf(
            "IDENTITY" to IDENTITY_MATRIX,
            "GRAYSCALE" to GRAYSCALE_MATRIX,
        )

        override val fragment: FragmentShader = FragmentShaderDefault {
            SET(out, tex(fragmentCoords))
            SET(out, mix(out, (u_ColorMatrix * out), u_BlendRatio))
            //BatchBuilder2D.DO_INPUT_OUTPUT(this, out)
        }
    }

    /** The 4x4 [Matrix3D] that will be used for transforming each pixel components [r, g, b, a] */
    @ViewProperty
	var colorMatrix: Matrix3D by uniforms.storageForMatrix3D(u_ColorMatrix, colorMatrix)

    /**
     * Ratio for blending the original color with the transformed color.
     * - 0 will return the original color
     * - 1 will return the transformed color
     * - Values between [0 and 1] would be an interpolation between those colors.
     * */
    @ViewProperty
	var blendRatio: Double by uniforms.storageFor(u_BlendRatio).doubleDelegateX(blendRatio)

    override val programProvider: ProgramProvider get() = ColorMatrixFilter

    @ViewProperty
    var namedColorMatrix: String
        get() = NAMED_MATRICES.entries.firstOrNull { it.value == colorMatrix }?.key ?: NAMED_MATRICES.keys.first()
        set(value) { colorMatrix = (NAMED_MATRICES[value] ?: IDENTITY_MATRIX) }
}
