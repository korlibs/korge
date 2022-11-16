package com.soywiz.korge.view.filter

import com.soywiz.kmem.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.property.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import kotlin.math.*

class OldBlurFilter(radius: Double = 4.0) : Filter {
    companion object {
        @Deprecated("", ReplaceWith("OldBlurFilter(radius = initialRadius)"))
        operator fun invoke(initialRadius: Double = 4.0, dummy: Unit = Unit): OldBlurFilter = OldBlurFilter(radius = initialRadius)
    }

    private val gaussianBlurs = mutableListOf<Convolute3Filter>()
    private val composedFilters = arrayListOf<Convolute3Filter>()
    private val composed = ComposedFilter(composedFilters)
    @ViewProperty
    var radius: Double = radius
        set(value) { field = value.clamp(0.0, 32.0) }
    //override val border: Int get() = composed.border
    override fun computeBorder(out: MutableMarginInt, texWidth: Int, texHeight: Int) = composed.computeBorder(out, texWidth, texHeight)

    override val recommendedFilterScale: Double get() = if (radius <= 2.0) 1.0 else 1.0 / log2(radius * 0.5)

    override fun render(
        ctx: RenderContext,
        matrix: Matrix,
        texture: Texture,
        texWidth: Int,
        texHeight: Int,
        renderColorAdd: ColorAdd,
        renderColorMul: RGBA,
        blendMode: BlendMode,
        filterScale: Double,
    ) {
        val radius = this.radius * sqrt(filterScale) //
        val nsteps = (radius).toIntCeil()
        // Cache values
        while (gaussianBlurs.size < nsteps) {
            gaussianBlurs.add(Convolute3Filter(Matrix3D(Convolute3Filter.KERNEL_GAUSSIAN_BLUR), gaussianBlurs.size.toDouble(), applyAlpha = true))
        }

        //println("border: $border")

        composedFilters.clear()
        val scale = radius != ceil(radius)

        for (n in 0 until nsteps) {
            val isLast = n == nsteps - 1
            val blur = gaussianBlurs[n]
            composedFilters.add(blur)
            val ratio = if (scale && isLast) 1.0 - (ceil(radius) - radius) else 1.0
            blur.weights.setToInterpolated(Convolute3Filter.KERNEL_IDENTITY, Convolute3Filter.KERNEL_GAUSSIAN_BLUR, ratio)
        }
        composed.render(ctx, matrix, texture, texWidth, texHeight, renderColorAdd, renderColorMul, blendMode, filterScale)
    }
}
