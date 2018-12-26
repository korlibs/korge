package com.soywiz.korge.view.filter

import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korma.geom.*

class Convolute3Filter(var kernel: Matrix3D) : Filter() {
	companion object {
		private val u_Weights = Uniform("weights", VarType.Mat3)

		val KERNEL_GAUSSIAN_BLUR: Matrix3D
			get() = Matrix3D(
				1f, 2f, 1f,
				2f, 4f, 2f,
				1f, 2f, 1f
			) * (1f / 16f)

		val KERNEL_BOX_BLUR: Matrix3D
			get() = Matrix3D(
				1f, 1f, 1f,
				1f, 1f, 1f,
				1f, 1f, 1f
			) * (1f / 9f)

		val KERNEL_IDENTITY: Matrix3D
			get() = Matrix3D(
				0f, 0f, 0f,
				0f, 1f, 0f,
				0f, 0f, 0f
			)

		val KERNEL_EDGE_DETECTION: Matrix3D
			get() = Matrix3D(
				-1f, -1f, -1f,
				-1f, +8f, -1f,
				-1f, -1f, -1f
			)
	}

	val weights by uniforms.storageForMatrix3D(u_Weights, kernel)
	override val border: Int = 1

	init {
		fragment = FragmentShader {
			DefaultShaders {
				out setTo vec4(0f.lit, 0f.lit, 0f.lit, 0f.lit)

				for (y in 0 until 3) {
					for (x in 0 until 3) {
						out setTo out + (tex(
							fragmentCoords + vec2(
								(x - 1).toFloat().lit,
								(y - 1).toFloat().lit
							)
						)) * u_Weights[x][y]
					}
				}
			}
		}
	}
}
