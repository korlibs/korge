package com.soywiz.korge.view.filter

import com.soywiz.kmem.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import kotlin.math.*

class BlurFilter(initialRadius: Double = 1.0) : Filter {
    private val gaussianBlurs = mutableListOf<Convolute3Filter>()
    private val composedFilters = arrayListOf<Convolute3Filter>()
    private val composed = ComposedFilter(composedFilters)
    var radius: Double = initialRadius
        set(value) { field = value.clamp(0.0, 32.0) }
    //override val border: Int get() = composed.border
    override val border: Int get() = (radius * 3).toInt()
    val nsteps get() = radius.toIntCeil()

    override fun render(
        ctx: RenderContext,
        matrix: Matrix,
        texture: Texture,
        texWidth: Int,
        texHeight: Int,
        renderColorAdd: Int,
        renderColorMul: RGBA,
        blendMode: BlendMode
    ) {
        val nsteps = this.nsteps
        // Cache values
        while (gaussianBlurs.size < nsteps) gaussianBlurs.add(Convolute3Filter(Convolute3Filter.KERNEL_GAUSSIAN_BLUR, gaussianBlurs.size.toDouble()))

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
        composed.render(ctx, matrix, texture, texWidth, texHeight, renderColorAdd, renderColorMul, blendMode)
    }
}
