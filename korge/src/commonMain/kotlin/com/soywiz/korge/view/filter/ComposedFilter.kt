package com.soywiz.korge.view.filter

import com.soywiz.kds.FastArrayList
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.Texture
import com.soywiz.korge.view.BlendMode
import com.soywiz.korge.view.Views
import com.soywiz.korim.color.ColorAdd
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.MutableMarginInt
import com.soywiz.korui.UiContainer

/**
 * Allows to create a single [Filter] that will render several [filters] in order.
 */
open class ComposedFilter private constructor(val filters: FastArrayList<Filter>, unit: Unit = Unit) : Filter {
    companion object {
        fun combine(left: Filter?, right: Filter?): Filter? = when {
            left == null && right == null -> null
            left == null -> right
            right == null -> left
            left is ComposedFilter && right is ComposedFilter -> ComposedFilter(left.filters + right.filters)
            left is ComposedFilter -> ComposedFilter(left.filters + right)
            right is ComposedFilter -> ComposedFilter(listOf(left) + right)
            else -> ComposedFilter(left, right)
        }

        fun combine(left: Filter?, right: List<Filter>): Filter? = when {
            left == null -> when {
                right.isEmpty() -> null
                right.size == 1 -> right.first()
                else -> ComposedFilter(right)
            }
            left is ComposedFilter -> ComposedFilter(left.filters + right)
            else -> ComposedFilter(listOf(left) + right)
        }
    }

    constructor() : this(mutableListOf())
    constructor(filters: List<Filter>) : this(if (filters is FastArrayList<Filter>) filters else FastArrayList(filters))
	constructor(vararg filters: Filter) : this(filters.toList())

    override val allFilters: List<Filter> get() = filters.flatMap { it.allFilters }

    override val recommendedFilterScale: Double get() {
        var out = 1.0
        filters.fastForEach { out *= it.recommendedFilterScale  }
        return out
    }

    override fun computeBorder(out: MutableMarginInt, texWidth: Int, texHeight: Int) {
        var sumLeft = 0
        var sumTop = 0
        var sumRight = 0
        var sumBottom = 0
        filters.fastForEach {
            it.computeBorder(out, texWidth, texHeight)
            sumLeft += out.left
            sumRight += out.right
            sumTop += out.top
            sumBottom += out.bottom
        }
        out.setTo(sumTop, sumRight, sumBottom, sumLeft)
        //println(out)
    }

    open val isIdentity: Boolean get() = false

    final override fun render(
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
        if (isIdentity) return IdentityFilter.render(ctx, matrix, texture, texWidth, texHeight, renderColorAdd, renderColorMul, blendMode, filterScale)
        renderIndex(ctx, matrix, texture, texWidth, texHeight, renderColorAdd, renderColorMul, blendMode, filterScale, filters.size - 1)
	}

	fun renderIndex(
		ctx: RenderContext,
		matrix: Matrix,
		texture: Texture,
		texWidth: Int,
		texHeight: Int,
		renderColorAdd: ColorAdd,
		renderColorMul: RGBA,
		blendMode: BlendMode,
        filterScale: Double,
		level: Int,
	) {
        if (level < 0 || filters.isEmpty()) {
            return IdentityFilter.render(ctx, matrix, texture, texWidth, texHeight, renderColorAdd, renderColorMul, blendMode, filterScale)
        }
        //println("ComposedFilter.renderIndex: $level")
		val filter = filters[filters.size - level - 1]

        filter.renderToTextureWithBorder(ctx, matrix, texture, texWidth, texHeight, filterScale) { newtex, newmatrix ->
            renderIndex(ctx, newmatrix, newtex, newtex.width, newtex.height, renderColorAdd, renderColorMul, blendMode, filterScale, level - 1)
        }
	}

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        for (filter in filters) {
            filter.buildDebugComponent(views, container)
        }
    }
}
