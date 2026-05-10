package korlibs.korge.view.filter

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.korge.view.property.*
import korlibs.math.*
import korlibs.math.geom.*

/**
 * A [Filter] that will convolute near pixels (3x3) with a [kernel].
 * [https://en.wikipedia.org/wiki/Kernel_(image_processing)](https://en.wikipedia.org/wiki/Kernel_(image_processing))
 */
class Convolute3Filter(
    /** 3x3 matrix representing a convolution kernel */
    var kernel: Matrix4,
    /** Distance between the origin pixel for sampling for edges */
    dist: Double = 1.0,
    applyAlpha: Boolean = false
) : ShaderFilter() {
    object ConvoluteUB : UniformBlock(fixedLocation = 5) {
        val u_ApplyAlpha by float()
        val u_Dist by float()
        val u_Weights by mat3()
    }

    companion object : BaseProgramProvider() {
        inline operator fun invoke(
            /** 3x3 matrix representing a convolution kernel */
            kernel: Matrix4,
            /** Distance between the origin pixel for sampling for edges */
            dist: Number = 1.0,
            applyAlpha: Boolean = false
        ): Convolute3Filter = Convolute3Filter(kernel, dist.toDouble(), applyAlpha)

        /** A Gaussian Blur Kernel. This [Matrix4] can be used as [kernel] for [Convolute3Filter] */
        val KERNEL_GAUSSIAN_BLUR: Matrix4 = Matrix4.fromRows3x3(
            1f, 2f, 1f,
            2f, 4f, 2f,
            1f, 2f, 1f
        ) * (1f / 16f)

        /** A Box Blur Kernel. This [Matrix4] can be used as [kernel] for [Convolute3Filter] */
        val KERNEL_BOX_BLUR: Matrix4 = Matrix4.fromRows3x3(
            1f, 1f, 1f,
            1f, 1f, 1f,
            1f, 1f, 1f
        ) * (1f / 9f)

        /** An Identity Kernel (doesn't perform any operation). This [MMatrix3D] can be used as [kernel] for [Convolute3Filter] */
        val KERNEL_IDENTITY: Matrix4 = Matrix4.fromRows3x3(
            0f, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 0f
        )

        /** An Edge Detection Kernel. This [Matrix4] can be used as [kernel] for [Convolute3Filter] */
        val KERNEL_EDGE_DETECTION: Matrix4 = Matrix4.fromRows3x3(
            -1f, -1f, -1f,
            -1f, +8f, -1f,
            -1f, -1f, -1f
        )

        /** A Sharpen Kernel. This [MMatrix3D] can be used as [kernel] for [Convolute3Filter] */
        val KERNEL_SHARPEN: Matrix4 = Matrix4.fromRows3x3(
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
                            (x - 1).toFloat().lit * ConvoluteUB.u_Dist,
                            (y - 1).toFloat().lit * ConvoluteUB.u_Dist
                        )
                    )
                    SET(out, out + (color * ConvoluteUB.u_Weights[x][y]))
                }
            }
            IF(ConvoluteUB.u_ApplyAlpha ne 1f.lit) {
                SET(out["a"], tex(fragmentCoords)["a"])
            }
            //BatchBuilder2D.DO_INPUT_OUTPUT(this, out)
        }
    }

    object KernelNameProvider : ViewPropertyProvider.ItemsMapImpl<Matrix4>() {
        override val ITEMS = NAMED_KERNELS
    }

    /** 3x3 matrix representing a convolution kernel */
    @ViewProperty
    var weights: Matrix4 = kernel
    /** Distance between the origin pixel for sampling for edges */
    @ViewProperty
    var dist: Double = dist
    /** Whether or not kernel must be applied to the alpha component */
    @ViewProperty
    var applyAlpha: Boolean = applyAlpha

    override val programProvider: ProgramProvider get() = Convolute3Filter

    override fun computeBorder(texWidth: Int, texHeight: Int): MarginInt {
        return MarginInt(dist.toIntCeil())
    }

    override fun updateUniforms(ctx: RenderContext, filterScale: Double) {
        super.updateUniforms(ctx, filterScale)
        ctx[ConvoluteUB].push {
            it[u_ApplyAlpha] = applyAlpha
            it[u_Dist] = dist
            it[u_Weights] = weights
        }

        //println("weights=$weights, dist=$dist,${uniforms[u_Dist].f32.toFloatArray().toList()}, ${uniforms[u_Weights].f32.toFloatArray().toList()}")
    }

    @ViewProperty
    @ViewPropertyProvider(KernelNameProvider::class)
    var namedKernel: String
        get() = NAMED_KERNELS.entries.firstOrNull { it.value == weights }?.key ?: NAMED_KERNELS.keys.first()
        set(value) { weights = (NAMED_KERNELS[value] ?: KERNEL_IDENTITY) }
}
