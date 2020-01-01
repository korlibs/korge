package com.soywiz.korge.view.effect

import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.view.*
import com.soywiz.korma.geom.*

@KorgeDeprecated
@Deprecated("Use View.filter instead")
class Convolute3EffectView(val kernel: Matrix3D) : EffectView() {
	companion object {
		private val u_Weights = Uniform("weights", VarType.Mat3)


		val KERNEL_GAUSSIAN_BLUR
			get() = Matrix3D.fromRows3x3(
				1f, 2f, 1f,
				2f, 4f, 2f,
				1f, 2f, 1f
			) * (1f / 16f)

		val KERNEL_BOX_BLUR
			get() = Matrix3D.fromRows3x3(
				1f, 1f, 1f,
				1f, 1f, 1f,
				1f, 1f, 1f
			) * (1f / 9f)

		val KERNEL_IDENTITY
			get() = Matrix3D.fromRows3x3(
				0f, 0f, 0f,
				0f, 1f, 0f,
				0f, 0f, 0f
			)

		val KERNEL_EDGE_DETECTION
			get() = Matrix3D.fromRows3x3(
				-1f, -1f, -1f,
				-1f, +8f, -1f,
				-1f, -1f, -1f
			)
	}

	val weights by uniforms.storageForMatrix3D(u_Weights, kernel)

	init {
		borderEffect = 1

		fragment = FragmentShader {
			DefaultShaders {
				out setTo vec4(0f.lit, 0f.lit, 0f.lit, 0f.lit)

				for (y in 0 until 3) {
					for (x in 0 until 3) {
						out setTo out + (tex(fragmentCoords + vec2((x - 1).toFloat().lit, (y - 1).toFloat().lit))) * u_Weights[x][y]
					}
				}
			}
		}
	}
}

@KorgeDeprecated
@Deprecated("Use View.filter instead")
inline fun Container.convolute3EffectView(
	kernel: Matrix3D = Matrix3D(),
	callback: @ViewsDslMarker Convolute3EffectView.() -> Unit = {}
) =
	Convolute3EffectView(kernel).addTo(this).apply(callback)
