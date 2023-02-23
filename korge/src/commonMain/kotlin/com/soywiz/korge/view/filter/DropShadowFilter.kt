package com.soywiz.korge.view.filter

import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
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

    override fun computeBorder(out: MMarginInt, texWidth: Int, texHeight: Int) {
        blur.computeBorder(out, texWidth, texHeight)
        if (dropX >= 0.0) out.right += dropX.toIntCeil() else out.left -= dropX.toIntCeil()
        if (dropY >= 0.0) out.bottom += dropY.toIntCeil() else out.top -= dropY.toIntCeil()
    }

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
                    colorMul = shadowColor,
                    blendMode = blendMode,
                    program = NON_TRANSPARENT_IS_WHITE,
                )
            }
        }

        ctx.useBatcher { batch ->
            batch.drawQuad(
                texture,
                m = matrix,
                filtering = smoothing,
                colorMul = renderColorMul,
                blendMode = blendMode,
                program = BatchBuilder2D.PROGRAM,
            )
        }
    }

    companion object {
        val NON_TRANSPARENT_IS_WHITE = BatchBuilder2D.PROGRAM.replacingFragment("nontransparentiswhite") {
            BatchBuilder2D.createTextureLookup(this)
            SET(out, out + vec4(1f, 1f, 1f, 0f))
            SET(out, out * BatchBuilder2D.v_ColMul)
            IF(out["a"] le 0f.lit) { DISCARD() }
        }
    }
}
