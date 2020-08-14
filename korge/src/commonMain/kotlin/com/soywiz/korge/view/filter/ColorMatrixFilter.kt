package com.soywiz.korge.view.filter

import com.soywiz.korag.shader.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.view.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*

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
		val GRAYSCALE_MATRIX = Matrix3D.fromColumns(
			0.33f, 0.33f, 0.33f, 0f,
			0.59f, 0.59f, 0.59f, 0f,
			0.11f, 0.11f, 0.11f, 0f,
			0f, 0f, 0f, 1f
		)

        /** A Matrix usable for [colorMatrix] that will preserve the original color */
		val IDENTITY_MATRIX = Matrix3D.fromColumns(
			1f, 0f, 0f, 0f,
			0f, 1f, 0f, 0f,
			0f, 0f, 1f, 0f,
			0f, 0f, 0f, 1f
		)

        private val FRAGMENT_SHADER = FragmentShader {
            apply {
                out setTo tex(fragmentCoords)
                out setTo mix(out, (u_ColorMatrix * out), u_BlendRatio)
            }
        }

        val NAMED_MATRICES = mapOf(
            "IDENTITY" to IDENTITY_MATRIX,
            "GRAYSCALE" to GRAYSCALE_MATRIX,
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

	override val fragment = FRAGMENT_SHADER

    var namedColorMatrix: String
        get() = NAMED_MATRICES.entries.firstOrNull { it.value == colorMatrix }?.key ?: NAMED_MATRICES.keys.first()
        set(value) = run { colorMatrix = (NAMED_MATRICES[value] ?: IDENTITY_MATRIX) }

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiEditableValue(listOf(colorMatrix::v00, colorMatrix::v01, colorMatrix::v02, colorMatrix::v03), name = "row0")
        container.uiEditableValue(listOf(colorMatrix::v10, colorMatrix::v11, colorMatrix::v12, colorMatrix::v13), name = "row1")
        container.uiEditableValue(listOf(colorMatrix::v20, colorMatrix::v21, colorMatrix::v22, colorMatrix::v23), name = "row2")
        container.uiEditableValue(listOf(colorMatrix::v30, colorMatrix::v31, colorMatrix::v32, colorMatrix::v33), name = "row3")
        container.uiEditableValue(::namedColorMatrix, values = { NAMED_MATRICES.keys.toList() })
        container.uiEditableValue(::blendRatio)
    }
}
