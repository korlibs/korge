package korlibs.korge.view

import korlibs.korge.render.RenderContext

class FilteredContainer : Container(), View.Reference {
	override fun renderInternal(ctx: RenderContext) {
		val bounds = getLocalBoundsOptimizedAnchored()
		ctx.renderToTexture(bounds.width.toInt(), bounds.height.toInt(), {
			super.renderInternal(ctx)
		}) {
		}
	}
}