package com.soywiz.korge.view

import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.Texture

class Image(var tex: Texture, var anchorX: Double = 0.0, var anchorY: Double = anchorX, views: Views) : View(views) {
	var smoothing = true
	override fun render(ctx: RenderContext) {
		ctx.batch.addQuad(tex, x = -(tex.width * anchorX).toFloat(), y = -(tex.height * anchorY).toFloat(), m = globalMatrix, filtering = smoothing, col1 = globalCol1)
	}

	override fun hitTest(x: Double, y: Double): View? {
		val lx = globalToLocalX(x, y)
		val ly = globalToLocalY(x, y)
		val sLeft = -tex.width * anchorX
		val sTop = -tex.height * anchorY
		val sRight = sLeft + tex.width
		val sBottom = sTop + tex.height
		val hits = lx >= sLeft && ly >= sTop && lx < sRight && ly < sBottom
		//println("GLOBAL($x, $y) - LOCAL($lx, $ly) - ($sLeft, $sTop, $sRight, $sBottom) : $hits")
		return if (hits) this else null
	}
}
