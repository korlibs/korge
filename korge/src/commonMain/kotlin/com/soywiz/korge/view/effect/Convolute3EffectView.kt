package com.soywiz.korge.view.effect

import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.view.*
import com.soywiz.korma.*

/**
 * https://en.wikipedia.org/wiki/Kernel_(image_processing)
 */
class Convolute3EffectView(val kernel: Matrix3) : EffectView() {
	companion object {
		private val u_Weights = Uniform("weights", VarType.Mat3)


		val KERNEL_GAUSSIAN_BLUR: Matrix3
			get() = Matrix3(
				1f, 2f, 1f,
				2f, 4f, 2f,
				1f, 2f, 1f
			) * (1f / 16f)

		val KERNEL_BOX_BLUR: Matrix3
			get() = Matrix3(
				1f, 1f, 1f,
				1f, 1f, 1f,
				1f, 1f, 1f
			) * (1f / 9f)

		val KERNEL_IDENTITY: Matrix3
			get() = Matrix3(
				0f, 0f, 0f,
				0f, 1f, 0f,
				0f, 0f, 0f
			)

		val KERNEL_EDGE_DETECTION: Matrix3
			get() = Matrix3(
				-1f, -1f, -1f,
				-1f, +8f, -1f,
				-1f, -1f, -1f
			)
	}

	val weights by uniforms.storageForMatrix3(u_Weights, kernel)

	init {
		borderEffect = 1

		fragment = FragmentShader {
			DefaultShaders {
				out setTo vec4(0f, 0f, 0f, 0f)

				for (y in 0 until 3) {
					for (x in 0 until 3) {
						out setTo out + (tex(fragmentCoords + vec2((x - 1).toFloat(), (y - 1).toFloat()))) * u_Weights[x][y]
					}
				}
			}
		}
	}
}

inline fun Container.convolute3EffectView(
	kernel: Matrix3 = Matrix3(),
	callback: @ViewsDslMarker Convolute3EffectView.() -> Unit = {}
) =
	Convolute3EffectView(kernel).addTo(this).apply(callback)
