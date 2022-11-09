package com.soywiz.korge.input

/*
import com.soywiz.korge.component.Component
import com.soywiz.korge.view.View
import com.soywiz.korio.async.Signal
import com.soywiz.korma.geom.IPointInt

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

val View.gestures get() = this.getOrCreateComponent<Gestures>(Component) { Gestures(this) }
*/
