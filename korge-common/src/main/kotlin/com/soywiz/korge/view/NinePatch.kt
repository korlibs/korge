package com.soywiz.korge.view

import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.Texture
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.Point2d
import com.soywiz.korma.geom.Rectangle
import kotlin.math.min

class NinePatch(
	views: Views,
	var tex: Texture,
	override var width: Double,
	override var height: Double,
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
		if (!visible) return
		// Precalculate points to avoid matrix multiplication per vertex on each frame

		//for (n in 0 until 4) posCuts[n].setTo(posCutsRatios[n].x * width, posCutsRatios[n].y * height)

		val texLeftWidth = tex.width * left
		val texTopHeight = tex.height * top

		val texRighttWidth = tex.width * right
		val texBottomHeight = tex.height * bottom

		val ratioX = if (width < tex.width) width / tex.width else 1.0
		val ratioY = if (height < tex.height) height / tex.height else 1.0

		val actualRatioX = min(ratioX, ratioY)
		val actualRatioY = min(ratioX, ratioY)

		//val ratioX = 1.0
		//val ratioY = 1.0

		posCuts[1].setTo(texLeftWidth * actualRatioX / width, texTopHeight * actualRatioY / height)
		posCuts[2].setTo(1.0 - texRighttWidth * actualRatioX / width, 1.0 - texBottomHeight * actualRatioY / height)

		ctx.batch.drawNinePatch(
			tex,
			sLeft.toFloat(), sTop.toFloat(),
			width.toFloat(), height.toFloat(),
			posCuts = posCuts,
			texCuts = texCuts,
			m = m,
			colorMul = colorMul,
			colorAdd = colorAdd,
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
