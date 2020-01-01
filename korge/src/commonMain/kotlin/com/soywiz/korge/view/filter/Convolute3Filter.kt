package com.soywiz.korge.view.filter

import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korma.geom.*

/**
 * A [Filter] that will convolute near pixels (3x3) with a [kernel].
 * [https://en.wikipedia.org/wiki/Kernel_(image_processing)](https://en.wikipedia.org/wiki/Kernel_(image_processing))
 */
class Convolute3Filter(
    /** 3x3 matrix representing a convolution kernel */
    var kernel: Matrix3D
) : ShaderFilter() {
	companion object {
		private val u_Weights = Uniform("weights", VarType.Mat3)

        /** A Gaussian Blur Kernel. This [Matrix3D] can be used as [kernel] for [Convolute3Filter] */
		val KERNEL_GAUSSIAN_BLUR: Matrix3D
			get() = Matrix3D.fromRows3x3(
				1f, 2f, 1f,
				2f, 4f, 2f,
				1f, 2f, 1f
			) * (1f / 16f)

        /** A Box Blur Kernel. This [Matrix3D] can be used as [kernel] for [Convolute3Filter] */
		val KERNEL_BOX_BLUR: Matrix3D
			get() = Matrix3D.fromRows3x3(
				1f, 1f, 1f,
				1f, 1f, 1f,
				1f, 1f, 1f
			) * (1f / 9f)

        /** A Identity Kernel (doesn't perform any operation). This [Matrix3D] can be used as [kernel] for [Convolute3Filter] */
		val KERNEL_IDENTITY: Matrix3D
			get() = Matrix3D.fromRows3x3(
				0f, 0f, 0f,
				0f, 1f, 0f,
				0f, 0f, 0f
			)

        /** A Edge Detection Kernel. This [Matrix3D] can be used as [kernel] for [Convolute3Filter] */
		val KERNEL_EDGE_DETECTION: Matrix3D
			get() = Matrix3D.fromRows3x3(
				-1f, -1f, -1f,
				-1f, +8f, -1f,
				-1f, -1f, -1f
			)
	}

	val weights by uniforms.storageForMatrix3D(u_Weights, kernel)
	override val border: Int = 1

    override val fragment = FragmentShader {
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
