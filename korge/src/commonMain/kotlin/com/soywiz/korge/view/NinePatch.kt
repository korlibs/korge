package com.soywiz.korge.view

import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.*
import kotlin.math.*

inline fun Container.ninePatch(
	tex: BmpSlice, width: Double, height: Double, left: Double, top: Double, right: Double, bottom: Double,
	callback: @ViewsDslMarker NinePatch.() -> Unit = {}
) = NinePatch(tex, width, height, left, top, right, bottom).addTo(this).apply(callback)

class NinePatch(
	var tex: BmpSlice,
	override var width: Double,
	override var height: Double,
	var left: Double,
	var top: Double,
	var right: Double,
	var bottom: Double
) : View() {
	var smoothing = true

	private val sLeft = 0.0
	private val sTop = 0.0

	val posCuts = arrayOf(
		Point(0, 0),
		Point(left, top),
		Point(1.0 - right, 1.0 - bottom),
		Point(1.0, 1.0)
	)

	val texCuts = arrayOf(
		Point(0, 0),
		Point(left, top),
		Point(1.0 - right, 1.0 - bottom),
		Point(1.0, 1.0)
	)

	override fun renderInternal(ctx: RenderContext) {
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
			ctx.getTex(tex),
			sLeft.toFloat(), sTop.toFloat(),
			width.toFloat(), height.toFloat(),
			posCuts = posCuts,
			texCuts = texCuts,
			m = globalMatrix,
			colorMul = renderColorMul,
			colorAdd = renderColorAdd,
			filtering = smoothing,
			blendFactors = renderBlendMode.factors
		)
	}

	override fun getLocalBoundsInternal(out: Rectangle) {
		out.setTo(sLeft, sTop, width, height)
	}

	override fun hitTest(x: Double, y: Double): View? {
		val sRight = sLeft + width
		val sBottom = sTop + height
		return if (checkGlobalBounds(x, y, sLeft, sTop, sRight, sBottom)) this else null
	}
}
