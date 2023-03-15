package com.soywiz.korge.view.filter

import com.soywiz.korge.render.*
import com.soywiz.korge.view.BlendMode
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.geom.MMatrix

/**
 * Simple [Filter] that draws the texture pixels without any kind of transformation
 */
open class IdentityFilter(val smoothing: Boolean) : Filter {
    companion object : IdentityFilter(smoothing = true)
    object Linear : IdentityFilter(smoothing = true)
    object Nearest : IdentityFilter(smoothing = false)

    override fun render(
        ctx: RenderContext,
        matrix: MMatrix,
        texture: Texture,
        texWidth: Int,
        texHeight: Int,
        renderColorMul: RGBA,
        blendMode: BlendMode,
        filterScale: Double,
    ) {
        ctx.useBatcher { batch ->
            batch.drawQuad(
                texture,
                m = matrix.immutable,
                filtering = smoothing,
                colorMul = renderColorMul,
                blendMode = blendMode,
                program = BatchBuilder2D.PROGRAM,
            )
        }
    }
}

val DummyFilter get() = IdentityFilter
