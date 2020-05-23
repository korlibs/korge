package com.soywiz.korge.input

import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*

class Gestures(override val view: View) : Component {
	class Direction(val point: IPointInt) {
		constructor(x: Int, y: Int) : this(IPointInt(x, y))

		val x get() = point.x
		val y get() = point.y

		companion object {
			val Up = Direction(0, -1)
			val Down = Direction(0, +1)
			val Left = Direction(-1, 0)
			val Right = Direction(+1, 0)
		}
	}

	val onSwipe = Signal<Direction>()
}

val View.gestures get() = this.getOrCreateComponent<Gestures> { Gestures(this) }
