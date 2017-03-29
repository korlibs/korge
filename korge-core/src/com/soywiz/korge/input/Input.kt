package com.soywiz.korge.input

import com.soywiz.korio.inject.Singleton
import com.soywiz.korio.util.Extra
import com.soywiz.korma.geom.Point2d

@Singleton
class Input {
	val frame = Frame()
	val mouse = Point2d(-1000.0, -1000.0)
	var mouseButtons = 0

	class Frame : Extra by Extra.Mixin() {
		fun reset() {
			this.extra?.clear()
		}
	}
}
