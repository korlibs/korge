package com.soywiz.korge.view.filter

import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.geom.*

class ComposedFilter(val filters: List<Filter>) : Filter() {
	constructor(vararg filters: Filter) : this(filters.toList())

	override val border get() = filters.sumBy { it.border }

	val filtering = true

	override fun render(
		ctx: RenderContext,
		matrix: Matrix,
		texture: Texture,
		texWidth: Int,
		texHeight: Int,
		renderColorAdd: Int,
		renderColorMul: RGBA,
		blendMode: BlendMode
	) {
		if (filters.isEmpty()) {
			super.render(ctx, matrix, texture, texWidth, texHeight, renderColorAdd, renderColorMul, blendMode)
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
		renderColorAdd: Int,
		renderColorMul: RGBA,
		blendMode: BlendMode,
		level: Int
	) {
		val filter = filters[filters.size - level - 1]
		val newTexWidth = texWidth + filter.border
		val newTexHeight = texHeight + filter.border
		// @TODO: We only need two render textures
		ctx.renderToTexture(newTexWidth, newTexHeight, {
			filter.render(ctx, identity, texture, newTexWidth, newTexHeight, renderColorAdd, renderColorMul, blendMode)
		}, { newtex ->
			if (level > 0) {
				renderIndex(ctx, matrix, newtex, newTexWidth, newTexHeight, renderColorAdd, renderColorMul, blendMode, level - 1)
			} else {
				filter.render(ctx, matrix, newtex, newTexWidth, newTexHeight, renderColorAdd, renderColorMul, blendMode)
			}
		})
	}
}
