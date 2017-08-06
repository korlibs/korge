package com.soywiz.korge.view

import com.soywiz.korge.render.RenderContext
import com.soywiz.korma.Matrix2d

class ScaleView(views: Views, width: Int, height: Int, scale: Double = 2.0, var filtering: Boolean = false) : FixedSizeContainer(views) {
	init {
		this.width = width.toDouble()
		this.height = height.toDouble()
		this.scale = scale
	}

	//val once = Once()

	private fun super_render(ctx: RenderContext, m: Matrix2d) {
		super.render(ctx, m);
	}

	override fun render(ctx: RenderContext, m: Matrix2d) {
		val iwidth = width.toInt()
		val iheight = height.toInt()

		ctx.renderToTexture(iwidth, iheight, renderToTexture = {
			//super.render(ctx, Matrix2d()) // @TODO: Bug with JTransc 0.6.6
			super_render(ctx, Matrix2d())
		}, use = { renderTexture ->
			ctx.batch.drawQuad(
				tex = renderTexture,
				x = 0f, y = 0f,
				width = iwidth.toFloat(),
				height = iheight.toFloat(),
				m = m,
				colorMul = colorMul,
				colorAdd = colorAdd,
				filtering = filtering
			)
			ctx.flush()
		})
	}
}

fun Views.scaleView(width: Int, height: Int, scale: Double = 2.0, filtering: Boolean = false): ScaleView = ScaleView(this, width, height, scale, filtering)
