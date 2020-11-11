package com.soywiz.korge.view.filter

import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*

/**
 * Simple [Filter] that draws the texture pixels without any kind of transformation
 */
object IdentityFilter : Filter {
    override fun render(
        ctx: RenderContext,
        matrix: Matrix,
        texture: Texture,
        texWidth: Int,
        texHeight: Int,
        renderColorAdd: ColorAdd,
        renderColorMul: RGBA,
        blendMode: BlendMode
    ) {
        ctx.batch.drawQuad(
            texture,
            m = matrix,
            filtering = true,
            colorAdd = renderColorAdd,
            colorMul = renderColorMul,
            blendFactors = blendMode.factors,
            program = BatchBuilder2D.getTextureLookupProgram(texture.premultiplied)
        )
    }
}

val DummyFilter get() = IdentityFilter
