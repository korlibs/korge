package com.soywiz.korge.view.filter

import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*

open class DropshadowFilter(
    var dropX: Double = 10.0,
    var dropY: Double = 10.0,
    val shadowColor: RGBA = Colors.BLACK.withAd(0.75),
    val blurRadius: Double = 4.0,
    val smoothing: Boolean = true
) : Filter {
    private val tm: Matrix = Matrix()

    private val blur = BlurFilter(16.0)
    private val identity = Matrix()

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
        //println(blur.border)
        blur.radius = blurRadius
        val newTexWidth = texWidth + blur.border
        val newTexHeight = texHeight + blur.border
        ctx.renderToTexture(newTexWidth, newTexHeight, {
            identity.identity()
            identity.translate(blur.border, blur.border)
            blur.render(ctx, identity, texture, newTexWidth, newTexHeight, renderColorAdd, renderColorMul, blendMode)
        }, { newtex ->
            tm.copyFrom(matrix)
            tm.pretranslate(dropX - blur.border, dropY - blur.border)
            ctx.useBatcher { batch ->
                batch.drawQuad(
                    newtex,
                    m = tm,
                    filtering = smoothing,
                    colorAdd = ColorAdd(+255, +255, +255, 0),
                    colorMul = shadowColor,
                    blendFactors = blendMode.factors,
                    program = BatchBuilder2D.getTextureLookupProgram(texture.premultiplied, add = BatchBuilder2D.AddType.PRE_ADD)
                )

                batch.drawQuad(
                    texture,
                    m = matrix,
                    filtering = smoothing,
                    colorAdd = renderColorAdd,
                    colorMul = renderColorMul,
                    blendFactors = blendMode.factors,
                    program = BatchBuilder2D.getTextureLookupProgram(texture.premultiplied, add = BatchBuilder2D.AddType.NO_ADD)
                )
            }
        })
    }
}
