package com.soywiz.korge.view.effect

import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korma.geom.*

@KorgeDeprecated
@Deprecated("Use View.filter instead")
class Convolute3EffectView(val kernel: Matrix3D) : EffectView() {
	companion object {
		private val u_Weights = Uniform("weights", VarType.Mat3)
		val KERNEL_GAUSSIAN_BLUR get() = Convolute3Filter.KERNEL_GAUSSIAN_BLUR
		val KERNEL_BOX_BLUR get() = Convolute3Filter.KERNEL_BOX_BLUR
		val KERNEL_IDENTITY get() = Convolute3Filter.KERNEL_IDENTITY
		val KERNEL_EDGE_DETECTION get() = Convolute3Filter.KERNEL_EDGE_DETECTION
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
	callback: Convolute3EffectView.() -> Unit = {}
) =
	Convolute3EffectView(kernel).addTo(this, callback)
