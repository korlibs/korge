package com.soywiz.korge.view.filter

import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*

/**
 * Interface for [View] filters.
 *
 * A filter is in charge of rendering a precomputed texture of a [View].
 *
 * [Filter] defines a [computeBorder]. The border is the amount of pixels, the generated texture should be grown from each side: left, top, right and bottom.
 * For example, a Gauissan Blur effect would require a bigger texture to blur the edges.
 *
 * [Filter] defines how to render the precomputed texture of the View inside the [render] method.
 *
 * Filters are usually [ComposedFilter] or [ShaderFilter]
 */
interface Filter : KorgeDebugNode {
    companion object {
        //val u_Time = Uniform("time", VarType.Float1)
        val u_TextureSize = Uniform("effectTextureSize", VarType.Float2)
        val DEFAULT_FRAGMENT = BatchBuilder2D.getTextureLookupProgram(premultiplied = false, add = BatchBuilder2D.AddType.NO_ADD).fragment

        val Program.Builder.fragmentCoords01 get() = DefaultShaders.v_Tex["xy"]
        val Program.Builder.fragmentCoords get() = fragmentCoords01 * u_TextureSize
        fun Program.Builder.tex(coords: Operand) = texture2D(DefaultShaders.u_Tex, coords / u_TextureSize)
    }

    val allFilters: List<Filter> get() = listOf(this)

    /**
     * The number of pixels the passed texture should be bigger at each direction: left, right, top, left.
     *
     * A 0 value means that the texture should be passed with its original size.
     * A 1 value means that the texture should be passed width 2 more pixels of width and height (1 left, 1 right), (1 top, 1 bottom)
     */
    @Deprecated("")
    val border: Int get() = 0

    val recommendedFilterScale: Double get() = 1.0

    fun computeBorder(out: MutableMarginInt, texWidth: Int, texHeight: Int) {
        out.setTo(border)
    }

    /**
     * The method in charge of rendering the texture transformed using [ctx] [RenderContext] and [matrix].
     * The method receives a [texture] that should be the original image with [computeBorder] additional pixels on each side.
     */
    fun render(
        ctx: RenderContext,
        matrix: Matrix,
        texture: Texture,
        texWidth: Int,
        texHeight: Int,
        renderColorAdd: ColorAdd,
        renderColorMul: RGBA,
        blendMode: BlendMode,
        filterScale: Double,
    )

    override fun buildDebugComponent(views: Views, container: UiContainer) {
    }
}

fun Filter.getBorder(texWidth: Int, texHeight: Int, out: MutableMarginInt = MutableMarginInt()): MarginInt {
    computeBorder(out, texWidth, texHeight)
    return out
}

fun Filter.renderToTextureWithBorder(
    ctx: RenderContext,
    matrix: Matrix,
    texture: Texture,
    texWidth: Int,
    texHeight: Int,
    filterScale: Double,
    block: (texture: Texture, matrix: Matrix) -> Unit,
) {
    val filter = this
    val margin = filter.getBorder(texWidth, texHeight, ctx.tempMargin)

    val borderLeft = (margin.left * filterScale).toIntCeil()
    val borderTop = (margin.top * filterScale).toIntCeil()

    val newTexWidth = texWidth + (margin.leftPlusRight * filterScale).toIntCeil()
    val newTexHeight = texHeight + (margin.topPlusBottom * filterScale).toIntCeil()

    //println("texWidth=$newTexWidth,$newTexHeight")

    ctx.renderToTexture(newTexWidth, newTexHeight, {
        ctx.matrixPool.alloc { matrix ->
            matrix.identity()
            matrix.translate(borderLeft, borderTop)
            ctx.batch.setViewMatrixTemp(ctx.identityMatrix) {
                filter.render(ctx, matrix, texture, newTexWidth, newTexHeight, ColorAdd.NEUTRAL, Colors.WHITE, BlendMode.NORMAL, filterScale)
            }
        }
    }) { newtex ->
        ctx.matrixPool.alloc { matrix2 ->
            matrix2.copyFrom(matrix)
            matrix2.pretranslate(-borderLeft, -borderTop)
            block(newtex, matrix2)
        }
    }
}

fun Filter.expandBorderRectangle(out: Rectangle, temp: MutableMarginInt = MutableMarginInt()) {
    out.expand(getBorder(out.width.toIntCeil(), out.height.toIntCeil(), temp))
}
