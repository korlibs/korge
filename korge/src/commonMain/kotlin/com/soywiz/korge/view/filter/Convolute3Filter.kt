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
    var kernel: Matrix3D,
    /** Distance between the origin pixel for sampling for edges */
    dist: Double = 1.0
) : ShaderFilter() {
    companion object {
        private val u_ApplyAlpha = Uniform("apply_alpha", VarType.Float1)
        private val u_Dist = Uniform("dist", VarType.Float1)
        private val u_Weights = Uniform("weights", VarType.Mat3)

        /** A Gaussian Blur Kernel. This [Matrix3D] can be used as [kernel] for [Convolute3Filter] */
        val KERNEL_GAUSSIAN_BLUR: Matrix3D = Matrix3D.fromRows3x3(
            1f, 2f, 1f,
            2f, 4f, 2f,
            1f, 2f, 1f
        ) * (1f / 16f)

        /** A Box Blur Kernel. This [Matrix3D] can be used as [kernel] for [Convolute3Filter] */
        val KERNEL_BOX_BLUR: Matrix3D = Matrix3D.fromRows3x3(
            1f, 1f, 1f,
            1f, 1f, 1f,
            1f, 1f, 1f
        ) * (1f / 9f)

        /** An Identity Kernel (doesn't perform any operation). This [Matrix3D] can be used as [kernel] for [Convolute3Filter] */
        val KERNEL_IDENTITY: Matrix3D = Matrix3D.fromRows3x3(
            0f, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 0f
        )

        /** An Edge Detection Kernel. This [Matrix3D] can be used as [kernel] for [Convolute3Filter] */
        val KERNEL_EDGE_DETECTION: Matrix3D = Matrix3D.fromRows3x3(
            -1f, -1f, -1f,
            -1f, +8f, -1f,
            -1f, -1f, -1f
        )

        /** A Sharpen Kernel. This [Matrix3D] can be used as [kernel] for [Convolute3Filter] */
        val KERNEL_SHARPEN: Matrix3D = Matrix3D.fromRows3x3(
            -1f, -1f, -1f,
            -1f, +9f, -1f,
            -1f, -1f, -1f
        )

        private val FRAGMENT_SHADER = FragmentShader {
            DefaultShaders {
                out setTo vec4(0f.lit, 0f.lit, 0f.lit, 0f.lit)

                for (y in 0 until 3) {
                    for (x in 0 until 3) {
                        val color = tex(
                            fragmentCoords + vec2(
                                (x - 1).toFloat().lit * u_Dist,
                                (y - 1).toFloat().lit * u_Dist
                            )
                        )
                        out setTo out + (color * u_Weights[x][y])
                    }
                }
                IF(u_ApplyAlpha ne 1f.lit) {
                    out["a"] setTo tex(fragmentCoords)["a"]
                }
            }
        }
    }

    /** 3x3 matrix representing a convolution kernel */
    val weights by uniforms.storageForMatrix3D(u_Weights, kernel)
    /** Distance between the origin pixel for sampling for edges */
    var dist by uniforms.storageFor(u_Dist).doubleDelegateX(dist)
    /** Whether or not kernel must be applied to the alpha component */
    var applyAlpha by uniforms.storageFor(u_ApplyAlpha).boolDelegateX(false)

    override val border: Int get() = dist.toInt()
    override val fragment = FRAGMENT_SHADER
}
