package korlibs.korge.view.filter

import korlibs.datastructure.*
import korlibs.korge.render.*
import korlibs.korge.view.*
import korlibs.math.*

/**
 * An optional [Filter] attached to this view.
 * Filters allow to render this view to a texture, and to control how to render that texture (using shaders, repeating the texture, etc.).
 * You add multiple filters by creating a composite filter [ComposedFilter].
 */
var View.filter: Filter?
    get() = getRenderPhaseOfTypeOrNull<ViewRenderPhaseFilter>()?.filter
    set(value) {
        val enabled = value != null
        //filterDebugExtra.enable = enabled
        if (enabled) {
            getOrCreateAndAddRenderPhase { ViewRenderPhaseFilter(value) }.filter = value
        } else {
            removeRenderPhaseOfType<ViewRenderPhaseFilter>()
        }
        invalidate()
    }

class ViewRenderPhaseFilter(var filter: Filter? = null) : ViewRenderPhase {
    companion object {
        const val PRIORITY = -200
    }
    override val priority: Int get() = PRIORITY

    override fun render(view: View, ctx: RenderContext) {
        if (this.filter != null) {
            view.renderFiltered(ctx, this.filter!!, first = false)
        } else {
            super.render(view, ctx)
        }
    }
}

/** Usually a value between [0.0, 1.0] */
var View.filterScale: Double by extraPropertyThis(transform = { Filter.discretizeFilterScale(it) }) { 1.0 }

//internal const val VIEW_FILTER_TRANSPARENT_EDGE = true
internal const val VIEW_FILTER_TRANSPARENT_EDGE = false

fun View.renderFiltered(
    ctx: RenderContext, filter: Filter,
    first: Boolean = true,
) {
    val bounds = getLocalBounds(includeFilters = false)

    if (bounds.width <= 0.0 || bounds.height <= 0.0) return

    val tryFilterScale = Filter.discretizeFilterScale(kotlin.math.min(filterScale, filter.recommendedFilterScale))
    //println("tryFilterScale=$tryFilterScale")
    val texWidthNoBorder = (bounds.width * tryFilterScale).toInt().coerceAtLeast(1)
    val texHeightNoBorder = (bounds.height * tryFilterScale).toInt().coerceAtLeast(1)

    val realFilterScale: Double = (texWidthNoBorder.toFloat() / bounds.width).clamp(0.03125, 1.0)

    // This edge is meant to keep the edge pixels transparent, since we are using clamping to edge wrapping
    // so for example the blur filter that reads outside [0, 1] bounds can read transparent pixels.
    val edgeSize = when (VIEW_FILTER_TRANSPARENT_EDGE) {
        true -> (1.0 / filterScale).toIntCeil().clamp(1, 8)
        false -> 0
    }

    val texWidth = texWidthNoBorder + (edgeSize * 2)
    val texHeight = texHeightNoBorder + (edgeSize * 2)

    val addx = -bounds.x + edgeSize
    val addy = -bounds.y + edgeSize

    //println("FILTER: $texWidth, $texHeight : $globalMatrixInv, $globalMatrix, addx=$addx, addy=$addy, renderColorMulInt=$renderColorMulInt, blendMode=$blendMode")
    //println("FILTER($this): $texWidth, $texHeight : bounds=${bounds} addx=$addx, addy=$addy, renderColorMul=$renderColorMul, blendMode=$blendMode")

    ctx.renderToTexture(texWidth, texHeight, render = {
        val mat = globalMatrixInv.translated(addx, addy).scaled(realFilterScale)
        //println("globalMatrixInv:$globalMatrixInv, tempMat2d=$tempMat2d")
        //println("texWidth=$texWidth, texHeight=$texHeight, $bounds, addx=$addx, addy=$addy, globalMatrix=$globalMatrix, globalMatrixInv:$globalMatrixInv, tempMat2d=$tempMat2d")
        @Suppress("DEPRECATION")
        ctx.batch.setViewMatrixTemp(mat) {
            // @TODO: Set blendMode to normal, colorMul to WHITE
            //renderInternal(ctx)
            if (first) {
                renderFirstPhase(ctx)
            } else {
                renderNextPhase(ctx)
            }
        }
    }) { texture ->
        //println("texWidthHeight=$texWidth,$texHeight")
        val mat = globalMatrix.pretranslated(-addx, -addy).prescaled(1f / realFilterScale)
        filter.render(
            ctx, mat, texture,
            texWidth, texHeight,
            renderColorMul, blendMode, realFilterScale
        )
    }
}

inline fun <T : View> T.filterScale(scale: Double): T {
    filterScale = scale
    return this
}

inline fun <T : View> T.filters(vararg filters: Filter, filterScale: Number = this.filterScale): T = filters(filters.toList(), filterScale.toDouble())
inline fun <T : View> T.filters(filters: List<Filter>, filterScale: Number = this.filterScale): T {
    this.filter = ComposedFilter.combine(null, filters)
    this.filterScale = filterScale.toDouble()
    return this
}

fun <T : View> T.addFilters(vararg filters: Filter): T = addFilters(filters.toList())
fun <T : View> T.addFilters(filters: List<Filter>): T {
    this.filter = ComposedFilter.combine(this.filter, filters)
    return this
}

fun <T : View> T.addFilter(filter: Filter): T {
    this.filter = ComposedFilter.combine(this.filter, filter)
    return this
}

fun View.removeFilter(filter: Filter) {
    when (this.filter) {
        filter -> this.filter = null
        is ComposedFilter -> this.filter = ComposedFilter((this.filter as ComposedFilter).filters.filter { it != filter })
    }
}


fun List<Filter?>.composedOrNull(): Filter? {
    val items = this.filterNotNull()
    return if (items.isEmpty()) null else ComposedFilter(items)
}
