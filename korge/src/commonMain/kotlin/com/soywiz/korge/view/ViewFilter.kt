package com.soywiz.korge.view

import com.soywiz.kds.extraPropertyThis
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kmem.clamp
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.view.filter.ComposedFilter
import com.soywiz.korge.view.filter.Filter

/**
 * An optional [Filter] attached to this view.
 * Filters allow to render this view to a texture, and to control how to render that texture (using shaders, repeating the texture, etc.).
 * You add multiple filters by creating a composite filter [ComposedFilter].
 */
var View.filter: Filter?
    get() = getRenderPhaseOfTypeOrNull<ViewRenderPhaseFilter>()?.filter
    set(value) {
        if (value != null) {
            getOrCreateAndAddRenderPhase { ViewRenderPhaseFilter(value) }.filter = value
        } else {
            removeRenderPhaseOfType<ViewRenderPhaseFilter>()
        }
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

fun View.addFilter(filter: Filter) {
    this.filter = ComposedFilter.combine(this.filter, filter)
}

fun View.removeFilter(filter: Filter) {
    when (this.filter) {
        filter -> this.filter = null
        is ComposedFilter -> this.filter = ComposedFilter((this.filter as ComposedFilter).filters.filter { it != filter })
    }
}

/** Usually a value between [0.0, 1.0] */
var View.filterScale: Double by extraPropertyThis(transform = { Filter.discretizeFilterScale(it) }) { 1.0 }

fun View.renderFiltered(ctx: RenderContext, filter: Filter, first: Boolean = true) {
    val bounds = getLocalBoundsOptimizedAnchored(includeFilters = false)

    if (bounds.width <= 0.0 || bounds.height <= 0.0) return

    ctx.matrixPool.alloc { tempMat2d ->
        val tryFilterScale = Filter.discretizeFilterScale(kotlin.math.min(filterScale, filter.recommendedFilterScale))
        //println("tryFilterScale=$tryFilterScale")
        val texWidthNoBorder = (bounds.width * tryFilterScale).toInt().coerceAtLeast(1)
        val texHeightNoBorder = (bounds.height * tryFilterScale).toInt().coerceAtLeast(1)

        val realFilterScale = (texWidthNoBorder.toDouble() / bounds.width).clamp(0.03125, 1.0)

        val texWidth = texWidthNoBorder
        val texHeight = texHeightNoBorder

        val addx = -bounds.x
        val addy = -bounds.y

        //println("FILTER: $texWidth, $texHeight : $globalMatrixInv, $globalMatrix, addx=$addx, addy=$addy, renderColorAdd=$renderColorAdd, renderColorMulInt=$renderColorMulInt, blendMode=$blendMode")
        //println("FILTER($this): $texWidth, $texHeight : bounds=${bounds} addx=$addx, addy=$addy, renderColorAdd=$renderColorAdd, renderColorMul=$renderColorMul, blendMode=$blendMode")

        ctx.renderToTexture(texWidth, texHeight, render = {
            tempMat2d.copyFrom(globalMatrixInv)
            //tempMat2d.copyFrom(globalMatrix)
            tempMat2d.translate(addx, addy)
            tempMat2d.scale(realFilterScale)
            //println("globalMatrixInv:$globalMatrixInv, tempMat2d=$tempMat2d")
            //println("texWidth=$texWidth, texHeight=$texHeight, $bounds, addx=$addx, addy=$addy, globalMatrix=$globalMatrix, globalMatrixInv:$globalMatrixInv, tempMat2d=$tempMat2d")
            @Suppress("DEPRECATION")
            ctx.batch.setViewMatrixTemp(tempMat2d) {
                // @TODO: Set blendMode to normal, colorMul to WHITE, colorAdd to NEUTRAL
                //renderInternal(ctx)
                if (first) {
                    renderFirstPhase(ctx)
                } else {
                    renderNextPhase(ctx)
                }
            }
        }) { texture ->
            //println("texWidthHeight=$texWidth,$texHeight")
            tempMat2d.copyFrom(globalMatrix)
            tempMat2d.pretranslate(-addx, -addy)
            tempMat2d.prescale(1.0 / realFilterScale)
            filter.render(
                ctx,
                tempMat2d,
                texture,
                texWidth,
                texHeight,
                renderColorAdd,
                renderColorMul,
                blendMode,
                realFilterScale
            )
        }
    }
}

inline fun <T : View> T.filterScale(scale: Double): T {
    filterScale = scale
    return this
}

inline fun <T : View> T.filters(vararg filters: Filter): T {
    filters.fastForEach { addFilter(it) }
    return this
}
