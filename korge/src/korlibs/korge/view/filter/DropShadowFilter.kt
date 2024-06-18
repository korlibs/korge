package korlibs.korge.view.filter

import korlibs.graphics.shader.*
import korlibs.image.color.*
import korlibs.korge.render.*
import korlibs.korge.view.BlendMode
import korlibs.korge.view.property.*
import korlibs.math.*
import korlibs.math.geom.*

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
) : FilterWithFiltering {
    private val blur = BlurFilter(16.0)

    override var filtering: Boolean by blur::filtering

    override fun computeBorder(texWidth: Int, texHeight: Int): MarginInt {
        val out = blur.computeBorder(texWidth, texHeight)
        var top = out.top
        var right = out.right
        var bottom = out.bottom
        var left = out.left
        if (dropX >= 0.0) right += dropX.toIntCeil() else left -= dropX.toIntCeil()
        if (dropY >= 0.0) bottom += dropY.toIntCeil() else top -= dropY.toIntCeil()
        return MarginInt(top, right, bottom, left)
    }

    override fun render(
        ctx: RenderContext,
        matrix: Matrix,
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

        inline operator fun invoke(
            dropX: Number = 10.0,
            dropY: Number = 10.0,
            shadowColor: RGBA = Colors.BLACK.withAd(0.75),
            blurRadius: Number = 4.0,
            smoothing: Boolean = true
        ): DropshadowFilter = DropshadowFilter(
            dropX.toDouble(), dropY.toDouble(),
            shadowColor, blurRadius.toDouble(),
            smoothing
        )
    }
}
