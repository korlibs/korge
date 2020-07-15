package com.soywiz.korge.view

import com.soywiz.korge.render.*

class FilteredContainer : Container(), View.Reference {
	override fun renderInternal(ctx: RenderContext) {
		val bounds = getLocalBounds()
		ctx.renderToTexture(bounds.width.toInt(), bounds.height.toInt(), {
			super.renderInternal(ctx)
		}) {
		}
	}
}