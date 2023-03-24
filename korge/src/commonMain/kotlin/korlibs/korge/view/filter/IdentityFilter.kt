package korlibs.korge.view.filter

import korlibs.korge.render.*
import korlibs.korge.view.BlendMode
import korlibs.image.color.RGBA
import korlibs.math.geom.*

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
        renderColorMul: RGBA,
        blendMode: BlendMode,
        filterScale: Double,
    ) {
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
}

val DummyFilter get() = IdentityFilter
