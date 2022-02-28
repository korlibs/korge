package com.soywiz.korge.view.filter

import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*

/**
 * Allows to create a single [Filter] that will render several [filters] in order.
 */
class ComposedFilter(val filters: List<Filter>) : Filter {
	constructor(vararg filters: Filter) : this(filters.toList())

    override val allFilters: List<Filter> get() = filters.flatMap { it.allFilters }

	override val border get() = filters.sumBy { it.border }

	override fun render(
        ctx: RenderContext,
        matrix: Matrix,
        texture: Texture,
        texWidth: Int,
        texHeight: Int,
        renderColorAdd: ColorAdd,
        renderColorMul: RGBA,
        blendMode: BlendMode
	) {
		if (filters.isEmpty()) {
            IdentityFilter.render(ctx, matrix, texture, texWidth, texHeight, renderColorAdd, renderColorMul, blendMode)
		} else {
			renderIndex(ctx, matrix, texture, texWidth, texHeight, renderColorAdd, renderColorMul, blendMode, filters.size - 1)
		}
	}

	private val identity = Matrix()

	fun renderIndex(
		ctx: RenderContext,
		matrix: Matrix,
		texture: Texture,
		texWidth: Int,
		texHeight: Int,
		renderColorAdd: ColorAdd,
		renderColorMul: RGBA,
		blendMode: BlendMode,
		level: Int
	) {
        //println("ComposedFilter.renderIndex: $level")
		val filter = filters[filters.size - level - 1]
		val newTexWidth = (texWidth + filter.border)
		val newTexHeight = (texHeight + filter.border)
		// @TODO: We only need two render textures
		ctx.renderToTexture(newTexWidth, newTexHeight, {
            ctx.batch.setViewMatrixTemp(identity) {
                filter.render(ctx, identity, texture, it.width, it.height, renderColorAdd, renderColorMul, blendMode)
            }
		}, { newtex ->
            //println("newtex=${newtex.width}x${newtex.height}")
			if (level > 0) {
				renderIndex(ctx, matrix, newtex, newtex.width, newtex.height, renderColorAdd, renderColorMul, blendMode, level - 1)
			} else {
                IdentityFilter.render(ctx, matrix, newtex, newtex.width, newtex.height, renderColorAdd, renderColorMul, blendMode)
			}
		})
	}

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        for (filter in filters) {
            filter.buildDebugComponent(views, container)
        }
    }
}
