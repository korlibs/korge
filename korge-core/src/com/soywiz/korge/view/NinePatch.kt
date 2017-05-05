package com.soywiz.korge.view

import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.Texture
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.Point2d
import com.soywiz.korma.geom.Rectangle

class NinePatch(
	views: Views,
	var tex: Texture,
	var width: Double,
	var height: Double,
	var left: Double,
	var top: Double,
	var right: Double,
	var bottom: Double
) : View(views) {
	var smoothing = true

	private val sLeft = 0.0
	private val sTop = 0.0

	val posCuts = arrayOf(
		Point2d(0, 0),
		Point2d(left, top),
		Point2d(1.0 - right, 1.0 - bottom),
		Point2d(1.0, 1.0)
	)

	val texCuts = arrayOf(
		Point2d(0, 0),
		Point2d(left, top),
		Point2d(1.0 - right, 1.0 - bottom),
		Point2d(1.0, 1.0)
	)

	override fun render(ctx: RenderContext, m: Matrix2d) {
		// Precalculate points to avoid matrix multiplication per vertex on each frame

		//for (n in 0 until 4) posCuts[n].setTo(posCutsRatios[n].x * width, posCutsRatios[n].y * height)

		val texLeftWidth = tex.width * left
		val texTopHeight = tex.height * top

		val texRighttWidth = tex.width * right
		val texBottomHeight = tex.height * bottom

		posCuts[1].setTo(texLeftWidth / width, texTopHeight / height)
		posCuts[2].setTo(1.0 - texRighttWidth / width, 1.0 - texBottomHeight / height)

		ctx.batch.drawNinePatch(
			tex,
			sLeft.toFloat(), sTop.toFloat(),
			width.toFloat(), height.toFloat(),
			posCuts = posCuts,
			texCuts = texCuts,
			m = m,
			colMul = colorMul,
			colAdd = colorAdd,
			filtering = smoothing,
			blendFactors = blendMode.factors
		)
	}

	override fun getLocalBoundsInternal(out: Rectangle) {
		out.setTo(sLeft, sTop, width, height)
	}

	override fun hitTestInternal(x: Double, y: Double): View? {
		val sRight = sLeft + width
		val sBottom = sTop + height
		return if (checkGlobalBounds(x, y, sLeft, sTop, sRight, sBottom)) this else null
	}
}

fun Views.ninePatch(tex: Texture, width: Double, height: Double, left: Double, top: Double, right: Double, bottom: Double) = NinePatch(this, tex, width, height, left, top, right, bottom)
