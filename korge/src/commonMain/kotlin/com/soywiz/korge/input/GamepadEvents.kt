package com.soywiz.korge.input

import com.soywiz.kds.*
import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korev.*

class GamePadEvents(override val view: View) : GamepadComponent {
	val stick = Signal<GamePadStickEvent>()
	val button = Signal<GamePadButtonEvent>()
	val connection = Signal<GamePadConnectionEvent>()

	fun stick(playerId: Int, stick: GameStick, callback: (x: Double, y: Double) -> Unit) {
		stick { e -> if (e.gamepad == playerId && e.stick == stick) callback(e.x, e.y) }
	}

	fun down(playerId: Int, button: GameButton, callback: () -> Unit) {
		button { e ->
			if (e.gamepad == playerId && e.button == button && e.type == GamePadButtonEvent.Type.DOWN) callback()
		}
	}

	fun up(playerId: Int, button: GameButton, callback: () -> Unit) {
		button { e ->
			if (e.gamepad == playerId && e.button == button && e.type == GamePadButtonEvent.Type.UP) callback()
		}
	}

	fun connected(playerId: Int, callback: () -> Unit) {
		connection { e ->
			if (e.gamepad == playerId && e.type == GamePadConnectionEvent.Type.CONNECTED) callback()
		}
	}

	fun disconnected(playerId: Int, callback: () -> Unit) {
		connection { e ->
			if (e.gamepad == playerId && e.type == GamePadConnectionEvent.Type.DISCONNECTED) callback()
		}
	}

	override fun onGamepadEvent(views: Views, event: GamePadButtonEvent) {
		button(event)
	}

	override fun onGamepadEvent(views: Views, event: GamePadStickEvent) {
		stick(event)
	}

	override fun onGamepadEvent(views: Views, event: GamePadConnectionEvent) {
		connection(event)
	}
}

val View.gamepad by Extra.PropertyThis<View, GamePadEvents> { this.getOrCreateComponent { GamePadEvents(this) } }
inline fun <T> View.gamepad(callback: GamePadEvents.() -> T): T = gamepad.run(callback)
