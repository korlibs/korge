package com.soywiz.korge.view.filter

import com.soywiz.korge.render.BatchBuilder2D
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.Texture
import com.soywiz.korge.view.BlendMode
import com.soywiz.korim.color.ColorAdd
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.geom.Matrix

/**
 * Simple [Filter] that draws the texture pixels without any kind of transformation
 */
open class IdentityFilter(val smoothing: Boolean) : Filter {
    companion object : IdentityFilter(smoothing = true)
    object Linear : IdentityFilter(smoothing = true)
    object Nearest : IdentityFilter(smoothing = false)

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
        ctx.useBatcher { batch ->
            batch.drawQuad(
                texture,
                m = matrix,
                filtering = smoothing,
                colorAdd = renderColorAdd,
                colorMul = renderColorMul,
                blendMode = blendMode,
                program = BatchBuilder2D.getTextureLookupProgram(BatchBuilder2D.AddType.NO_ADD)
            )
        }
    }
}

val DummyFilter get() = IdentityFilter
