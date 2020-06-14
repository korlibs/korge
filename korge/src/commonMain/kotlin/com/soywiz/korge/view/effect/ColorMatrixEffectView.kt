package com.soywiz.korge.view.effect

import com.soywiz.korag.shader.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korma.geom.*

@KorgeDeprecated
@Deprecated("Use View.filter instead")
class ColorMatrixEffectView(colorMatrix: Matrix3D) : EffectView() {
	companion object {
	    val u_ColorMatrix = Uniform("colorMatrix", VarType.Mat4)

		val GRAYSCALE_MATRIX get() = ColorMatrixFilter.GRAYSCALE_MATRIX
	}

	val colorMatrix by uniforms.storageForMatrix3D(u_ColorMatrix, colorMatrix)

	init {
		fragment = FragmentShader {
			apply {
				out setTo (u_ColorMatrix * tex(fragmentCoords))
			}
		}
	}
}

@KorgeDeprecated
@Deprecated("Use View.filter instead")
inline fun Container.colorMatrixEffectView(
	matrix: Matrix3D = Matrix3D(),
	callback: @ViewsDslMarker ColorMatrixEffectView.() -> Unit = {}
) =
	ColorMatrixEffectView(matrix).addTo(this, callback)
