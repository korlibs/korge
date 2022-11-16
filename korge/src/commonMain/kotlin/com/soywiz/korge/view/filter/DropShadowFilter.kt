package com.soywiz.korge.view.filter

import com.soywiz.kmem.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.property.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*

open class DropshadowFilter(
    @ViewProperty
    var dropX: Double = 10.0,
    @ViewProperty
    var dropY: Double = 10.0,
    @ViewProperty
    var shadowColor: RGBA = Colors.BLACK.withAd(0.75),
    @ViewProperty
    var blurRadius: Double = 4.0,
    @ViewProperty
    var smoothing: Boolean = true
) : Filter {
    private val blur = BlurFilter(16.0)

    override fun computeBorder(out: MutableMarginInt, texWidth: Int, texHeight: Int) {
        blur.computeBorder(out, texWidth, texHeight)
        if (dropX >= 0.0) out.right += dropX.toIntCeil() else out.left -= dropX.toIntCeil()
        if (dropY >= 0.0) out.bottom += dropY.toIntCeil() else out.top -= dropY.toIntCeil()
    }

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
        //println(blur.border)
        blur.radius = blurRadius

        blur.renderToTextureWithBorder(ctx, matrix, texture, texWidth, texHeight, filterScale) { newtex, matrix ->
            ctx.useBatcher { batch ->
                batch.drawQuad(
                    newtex,
                    m = matrix,
                    x = (dropX * filterScale).toFloat(),
                    y = (dropY * filterScale).toFloat(),
                    filtering = smoothing,
                    colorAdd = ColorAdd(+255, +255, +255, 0),
                    colorMul = shadowColor,
                    blendMode = blendMode,
                    program = BatchBuilder2D.getTextureLookupProgram(add = BatchBuilder2D.AddType.PRE_ADD),
                    premultiplied = newtex.premultiplied, wrap = false,
                )
            }
        }

        ctx.useBatcher { batch ->
            batch.drawQuad(
                texture,
                m = matrix,
                filtering = smoothing,
                colorAdd = renderColorAdd,
                colorMul = renderColorMul,
                blendMode = blendMode,
                program = BatchBuilder2D.getTextureLookupProgram(add = BatchBuilder2D.AddType.NO_ADD),
                premultiplied = texture.premultiplied, wrap = false,
            )
        }
    }
}
