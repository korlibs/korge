package com.soywiz.korge.view

import com.soywiz.korge.render.RenderContext
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.Rectangle

class SolidRect(views: Views, override var width: Double, override var height: Double, color: Int) : View(views) {
	init {
		this.colorMul = color
	}
	private var sLeft = 0.0
	private var sTop = 0.0

	override fun render(ctx: RenderContext, m: Matrix2d) {
		if (!visible) return
		//println("%08X".format(color))
		ctx.batch.drawQuad(views.whiteTexture, x = 0f, y = 0f, width = width.toFloat(), height = height.toFloat(), m = m, filtering = false, colorMul = RGBA.multiply(colorMul, globalColorMul), colorAdd = globalColorAdd, blendFactors = computedBlendMode.factors)
	}

	override fun getLocalBoundsInternal(out: Rectangle) {
		out.setTo(sLeft, sTop, width, height)
	}

	override fun hitTestInternal(x: Double, y: Double): View? {
		return if (checkGlobalBounds(x, y, 0.0, 0.0, width, height)) this else null
	}
}
