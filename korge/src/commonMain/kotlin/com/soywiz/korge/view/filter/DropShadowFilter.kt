package com.soywiz.korge.view.filter

import com.soywiz.kmem.toIntCeil
import com.soywiz.korge.render.BatchBuilder2D
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.Texture
import com.soywiz.korge.view.BlendMode
import com.soywiz.korim.color.ColorAdd
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.MutableMarginInt

open class DropshadowFilter(
    var dropX: Double = 10.0,
    var dropY: Double = 10.0,
    var shadowColor: RGBA = Colors.BLACK.withAd(0.75),
    var blurRadius: Double = 4.0,
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
                    blendFactors = blendMode.factors,
                    program = BatchBuilder2D.getTextureLookupProgram(texture.premultiplied, add = BatchBuilder2D.AddType.PRE_ADD)
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
                blendFactors = blendMode.factors,
                program = BatchBuilder2D.getTextureLookupProgram(texture.premultiplied, add = BatchBuilder2D.AddType.NO_ADD)
            )
        }
    }
}
