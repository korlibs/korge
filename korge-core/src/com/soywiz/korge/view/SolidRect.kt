package com.soywiz.korge.view

import com.soywiz.korge.render.RenderContext
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.Rectangle

class SolidRect(views: Views, var width: Double, var height: Double, color: Int) : View(views) {
	init {
		this.color = color
	}
	private var sLeft = 0.0
	private var sTop = 0.0

	override fun render(ctx: RenderContext, m: Matrix2d) {
		//println("%08X".format(color))
		ctx.batch.addQuad(views.whiteTexture, x = 0f, y = 0f, width = width.toFloat(), height = height.toFloat(), m = m, filtering = false, col1 = RGBA.multiply(color, globalColor), blendMode = blendMode)
	}

	override fun getLocalBounds(out: Rectangle) {
		out.setTo(sLeft, sTop, width, height)
	}

	override fun hitTestInternal(x: Double, y: Double): View? {
		return if (checkGlobalBounds(x, y, 0.0, 0.0, width, height)) this else null
	}
}
