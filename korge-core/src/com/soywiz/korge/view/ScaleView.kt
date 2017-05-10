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

fun Views.scaleView(width: Int, height: Int, scale: Double = 2.0, filtering: Boolean = false): ScaleView = ScaleView(this, width, height, scale, filtering)
