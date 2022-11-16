package com.soywiz.korge.view.filter

import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.view.property.*
import com.soywiz.korma.geom.*

/**
 * A [Filter] that will convolute near pixels (3x3) with a [kernel].
 * [https://en.wikipedia.org/wiki/Kernel_(image_processing)](https://en.wikipedia.org/wiki/Kernel_(image_processing))
 */
class Convolute3Filter(
    /** 3x3 matrix representing a convolution kernel */
    var kernel: Matrix3D,
    /** Distance between the origin pixel for sampling for edges */
    dist: Double = 1.0,
    applyAlpha: Boolean = false
) : ShaderFilter() {
    companion object : BaseProgramProvider() {
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

        val NAMED_KERNELS = mapOf(
            "IDENTITY" to KERNEL_IDENTITY,
            "GAUSSIAN_BLUR" to KERNEL_GAUSSIAN_BLUR,
            "BOX_BLUR" to KERNEL_BOX_BLUR,
            "EDGE_DETECTION" to KERNEL_EDGE_DETECTION,
            "SHARPEN" to KERNEL_SHARPEN,
        )

        override val fragment = FragmentShaderDefault {
            SET(out, vec4(0f.lit, 0f.lit, 0f.lit, 0f.lit))

            for (y in 0 until 3) {
                for (x in 0 until 3) {
                    val color = tex(
                        fragmentCoords + vec2(
                            (x - 1).toFloat().lit * u_Dist,
                            (y - 1).toFloat().lit * u_Dist
                        )
                    )
                    SET(out, out + (color * u_Weights[x][y]))
                }
            }
            IF(u_ApplyAlpha ne 1f.lit) {
                SET(out["a"], tex(fragmentCoords)["a"])
            }
            //BatchBuilder2D.DO_INPUT_OUTPUT(this, out)
        }
    }

    object KernelNameProvider {
        val LIST = NAMED_KERNELS
    }

    /** 3x3 matrix representing a convolution kernel */
    @ViewProperty
    var weights: Matrix3D by uniforms.storageForMatrix3D(u_Weights, kernel)
    /** Distance between the origin pixel for sampling for edges */
    @ViewProperty
    var dist: Double by uniforms.storageFor(u_Dist).doubleDelegateX(dist)
    /** Whether or not kernel must be applied to the alpha component */
    @ViewProperty
    var applyAlpha: Boolean by uniforms.storageFor(u_ApplyAlpha).boolDelegateX(applyAlpha)

    override val programProvider: ProgramProvider get() = Convolute3Filter

    override fun computeBorder(out: MutableMarginInt, texWidth: Int, texHeight: Int) {
        out.setTo(dist.toIntCeil())
    }

    @ViewProperty
    @ViewPropertyProvider(KernelNameProvider::class)
    var namedKernel: String
        get() = NAMED_KERNELS.entries.firstOrNull { it.value == weights }?.key ?: NAMED_KERNELS.keys.first()
        set(value) { weights = (NAMED_KERNELS[value] ?: KERNEL_IDENTITY) }
}
