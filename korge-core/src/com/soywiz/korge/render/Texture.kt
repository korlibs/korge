package com.soywiz.korge.render

import com.soywiz.korag.AG

class Texture(val base: Base, val x: Int = 0, val y: Int = 0, val width: Int = base.width, val height: Int = base.height) {
	val x0: Float = (x).toFloat() / base.width.toFloat()
	val x1: Float = (x + width).toFloat() / base.width.toFloat()
	val y0: Float = (y).toFloat() / base.width.toFloat()
	val y1: Float = (y + height).toFloat() / base.width.toFloat()

	companion object {
		operator fun invoke(agBase: AG.Texture, width: Int, height: Int): Texture = Texture(Base(agBase, width, height), 0, 0, width, height)
	}

	class Base(val base: AG.Texture, val width: Int, val height: Int) {
	}
}