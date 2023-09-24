package korlibs.korge.view.filter

import korlibs.datastructure.*
import korlibs.graphics.*
import korlibs.graphics.annotation.*
import korlibs.image.color.*
import korlibs.io.lang.*
import korlibs.korge.render.*
import korlibs.korge.view.*
import korlibs.math.*
import korlibs.math.geom.*
import kotlin.math.*
import kotlin.native.concurrent.*

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
interface Filter {
    companion object {
        private val VALID_FILTER_SCALES = doubleArrayOf(0.03125, 0.0625, 0.125, 0.25, 0.5, 0.75, 1.0)
        fun discretizeFilterScale(scale: Double): Double {
            //return scale.clamp(0.03125, 1.5)
            return VALID_FILTER_SCALES.minByOrNull { (scale - it).absoluteValue }!!
        }
    }

    val allFilters: List<Filter> get() = listOf(this)

    val recommendedFilterScale: Double get() = 1.0

    /**
     * The number of pixels the passed texture should be bigger at each direction: left, right, top, left.
     *
     * A 0 value means that the texture should be passed with its original size.
     * A 1 value means that the texture should be passed width 2 more pixels of width and height (1 left, 1 right), (1 top, 1 bottom)
     */
    fun computeBorder(texWidth: Int, texHeight: Int): MarginInt = MarginInt.ZERO

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
        renderColorMul: RGBA,
        blendMode: BlendMode,
        filterScale: Double,
    )
}

fun Filter.getBorder(texWidth: Int, texHeight: Int): MarginInt {
    return computeBorder(texWidth, texHeight)
}

@Deprecated("")
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
    val margin = filter.getBorder(texWidth, texHeight)

    val borderLeft = (margin.left * filterScale).toIntCeil()
    val borderTop = (margin.top * filterScale).toIntCeil()

    val newTexWidth = texWidth + (margin.leftPlusRight * filterScale).toIntCeil()
    val newTexHeight = texHeight + (margin.topPlusBottom * filterScale).toIntCeil()

    //println("texWidth=$newTexWidth,$newTexHeight")

    ctx.renderToTexture(newTexWidth, newTexHeight, {
        ctx.batch.setViewMatrixTemp(Matrix.IDENTITY) {
            filter.render(
                ctx,
                Matrix.IDENTITY.translated(borderLeft, borderTop),
                texture,
                newTexWidth,
                newTexHeight,
                Colors.WHITE,
                BlendMode.NORMAL,
                filterScale
            )
        }
    }) { newtex ->
        block(newtex, matrix.pretranslated(-borderLeft, -borderTop))
    }
}

class RenderToTextureResult() : Disposable {
    var filter: Filter? = null
    var newTexWidth: Int = 0
    var newTexHeight: Int = 0
    var borderLeft: Int = 0
    var borderTop: Int = 0
    var filterScale: Double = 1.0
    var matrix = Matrix.IDENTITY
    var texture: Texture? = null
    var fb: AGFrameBuffer? = null
    var newtex: Texture? = null
    var ctx: RenderContext? = null

    fun render() {
        val fb = fb ?: return
        val ctx = ctx ?: return
        ctx.renderToFrameBuffer(fb) {
            ctx.batch.setViewMatrixTemp(Matrix.IDENTITY) {
                texture?.let {
                    filter?.render(
                        ctx,
                        Matrix.IDENTITY.translated(borderLeft, borderTop),
                        it,
                        newTexWidth,
                        newTexHeight,
                        Colors.WHITE,
                        BlendMode.NORMAL,
                        filterScale
                    )
                }
            }
        }
        newtex = Texture(fb).sliceWithSize(0, 0, newTexWidth, newTexHeight)
    }

    override fun dispose() {
        if (fb == null || ctx == null) return
        fb?.let { ctx?.unsafeFreeFrameBuffer(it) }
        filter = null
        texture = null
        fb = null
        ctx = null
        newtex = null
    }
}

@KoragExperimental
fun Filter.renderToTextureWithBorderUnsafe(
    ctx: RenderContext,
    matrix: Matrix,
    texture: Texture,
    texWidth: Int,
    texHeight: Int,
    filterScale: Double,
    result: RenderToTextureResult = RenderToTextureResult()
): RenderToTextureResult {
    val filter = this
    val margin = filter.getBorder(texWidth, texHeight)

    val borderLeft = (margin.left * filterScale).toIntCeil()
    val borderTop = (margin.top * filterScale).toIntCeil()
    val newTexWidth = texWidth + (margin.leftPlusRight * filterScale).toIntCeil()
    val newTexHeight = texHeight + (margin.topPlusBottom * filterScale).toIntCeil()

    //println("texWidth=$newTexWidth,$newTexHeight")

    ctx.flush()
    val fb: AGFrameBuffer = ctx.unsafeAllocateFrameBuffer(newTexWidth, newTexHeight)
    result.borderLeft = borderLeft
    result.borderTop = borderTop
    result.newTexWidth = newTexWidth
    result.newTexHeight = newTexHeight
    result.texture = texture
    result.filter = filter
    result.filterScale = filterScale
    result.matrix = matrix.pretranslated(-borderLeft, -borderTop)
    result.ctx = ctx
    result.fb = fb
    return result
}

fun Filter.expandedBorderRectangle(inp: Rectangle): Rectangle =
    inp.expanded(getBorder(inp.width.toIntCeil(), inp.height.toIntCeil()))

@ThreadLocal
val RenderContext.renderToTextureResultPool by Extra.Property { Pool({ it.dispose() }) { RenderToTextureResult() } }
