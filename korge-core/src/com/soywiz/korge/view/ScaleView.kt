package com.soywiz.korge.view

import com.soywiz.korge.render.RenderContext
import com.soywiz.korma.Matrix2d

class ScaleView(views: Views) : FixedSizeContainer(views) {
	var filtering = false

	init {
		width = 128.0
		height = 128.0
		scale = 4.0
	}

	//val once = Once()
	override fun render(ctx: RenderContext, m: Matrix2d) {
		val iwidth = width.toInt()
		val iheight = height.toInt()

		//once {
		//	val bmp = ctx.renderToBitmap(Bitmap32(iwidth, iheight)) {
		//		//ctx.ag.clear(Colors.BLUE)
		//		super.render(ctx, Matrix2d())
		//	}
		//	go {
		//		showImageAndWait(bmp)
		//	}
		//}

		val renderTexture = ctx.renderToTexture(iwidth, iheight) {
			//ctx.ag.clear(Colors.BLUE)
			//super.render(ctx, Matrix2d())
			//super.render(ctx, Matrix2d())
			super.render(ctx, Matrix2d())
		}
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
	}
}
