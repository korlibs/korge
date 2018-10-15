package com.soywiz.korge.view

import com.soywiz.korim.color.*

inline fun Container.solidRect(
	width: Number, height: Number, color: RGBA, callback: @ViewsDslMarker SolidRect.() -> Unit = {}
) = SolidRect(width.toDouble(), height.toDouble(), color).addTo(this).apply(callback)

class SolidRect(width: Double, height: Double, color: Int) : RectBase() {
	companion object {
		inline operator fun invoke(width: Number, height: Number, color: RGBA) =
			SolidRect(width.toDouble(), height.toDouble(), color.rgba)
	}

	override var width: Double = width; set(v) = run { field = v }.also { dirtyVertices = true }
	override var height: Double = height; set(v) = run { field = v }.also { dirtyVertices = true }

	init {
		this.colorMulInt = color
	}

	override fun createInstance(): View = SolidRect(width, height, colorMulInt)
}
