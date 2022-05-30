package com.soywiz.korge.view.filter

import com.soywiz.kmem.toIntCeil
import com.soywiz.korag.DefaultShaders
import com.soywiz.korag.shader.FragmentShader
import com.soywiz.korag.shader.Uniform
import com.soywiz.korag.shader.VarType
import com.soywiz.korag.shader.storageFor
import com.soywiz.korag.shader.storageForMatrix3D
import com.soywiz.korge.debug.uiEditableValue
import com.soywiz.korge.view.Views
import com.soywiz.korma.geom.Matrix3D
import com.soywiz.korma.geom.MutableMarginInt
import com.soywiz.korma.geom.fromRows3x3
import com.soywiz.korma.geom.times
import com.soywiz.korui.UiContainer

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

        override val fragment = FragmentShader {
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
        }
    }

    /** 3x3 matrix representing a convolution kernel */
    var weights by uniforms.storageForMatrix3D(u_Weights, kernel)
    /** Distance between the origin pixel for sampling for edges */
    var dist by uniforms.storageFor(u_Dist).doubleDelegateX(dist)
    /** Whether or not kernel must be applied to the alpha component */
    var applyAlpha by uniforms.storageFor(u_ApplyAlpha).boolDelegateX(applyAlpha)

    override val programProvider: ProgramProvider get() = Convolute3Filter

    override fun computeBorder(out: MutableMarginInt, texWidth: Int, texHeight: Int) {
        out.setTo(dist.toIntCeil())
    }

    var namedKernel: String
        get() = NAMED_KERNELS.entries.firstOrNull { it.value == weights }?.key ?: NAMED_KERNELS.keys.first()
        set(value) { weights = (NAMED_KERNELS[value] ?: KERNEL_IDENTITY) }

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiEditableValue(listOf(weights::v00, weights::v01, weights::v02), name = "row0")
        container.uiEditableValue(listOf(weights::v10, weights::v11, weights::v12), name = "row1")
        container.uiEditableValue(listOf(weights::v20, weights::v21, weights::v22), name = "row2")
        container.uiEditableValue(::dist)
        container.uiEditableValue(::applyAlpha)
        container.uiEditableValue(::namedKernel, values = { NAMED_KERNELS.keys.toList() })
    }
}
