package com.soywiz.korge.view.filter

import com.soywiz.korag.shader.*
import com.soywiz.korge.view.*
import com.soywiz.korma.*

class ColorMatrixFilter(colorMatrix: Matrix4, blendRatio: Double) : Filter() {
	companion object {
		private val u_ColorMatrix = Uniform("colorMatrix", VarType.Mat4)
		private val u_BlendRatio = Uniform("blendRatio", VarType.Float1)

		val GRAYSCALE_MATRIX = Matrix4(
			0.33f, 0.33f, 0.33f, 0f,
			0.59f, 0.59f, 0.59f, 0f,
			0.11f, 0.11f, 0.11f, 0f,
			0f, 0f, 0f, 1f
		)

		val IDENTITY_MATRIX = Matrix4(
			1f, 0f, 0f, 0f,
			0f, 1f, 0f, 0f,
			0f, 0f, 1f, 0f,
			0f, 0f, 0f, 1f
		)
	}

	var colorMatrix by uniforms.storageForMatrix4(u_ColorMatrix, colorMatrix)
	var blendRatio by uniforms.storageFor(u_BlendRatio).doubleDelegateX(blendRatio)

	init {
		fragment = FragmentShader {
			apply {
				out setTo tex(fragmentCoords)
				out setTo mix(out, (u_ColorMatrix * out), u_BlendRatio)
			}
		}
	}
}
