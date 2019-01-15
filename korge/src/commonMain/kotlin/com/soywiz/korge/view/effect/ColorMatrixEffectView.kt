package com.soywiz.korge.view.effect

import com.soywiz.korag.shader.*
import com.soywiz.korge.view.*
import com.soywiz.korma.geom.*

class ColorMatrixEffectView(colorMatrix: Matrix3D) : EffectView() {
	companion object {
	    val u_ColorMatrix = Uniform("colorMatrix", VarType.Mat4)

		val GRAYSCALE_MATRIX = Matrix3D.fromRows(
			0.33f, 0.33f, 0.33f, 0f,
			0.59f, 0.59f, 0.59f, 0f,
			0.11f, 0.11f, 0.11f, 0f,
			0f, 0f, 0f, 1f
		)
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

inline fun Container.colorMatrixEffectView(
	matrix: Matrix3D = Matrix3D(),
	callback: @ViewsDslMarker ColorMatrixEffectView.() -> Unit = {}
) =
	ColorMatrixEffectView(matrix).addTo(this).apply(callback)
