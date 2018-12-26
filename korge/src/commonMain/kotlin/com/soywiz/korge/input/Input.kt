package com.soywiz.korge.input

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korinject.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.event.*
import com.soywiz.korui.input.*

//@Singleton
class Input : Extra by Extra.Mixin() {
	companion object {
		const val KEYCODES = 0x100
	}

	val dummyTouch = Touch.dummy
	val touches = (0 until 16).map { Touch(it) }.toTypedArray()
	val activeTouches = arrayListOf<Touch>()

	var _isTouchDeviceGen = { false }
	val isTouchDevice: Boolean get() = _isTouchDeviceGen()

	fun getTouch(id: Int) = touches.firstOrNull { it.id == id } ?: touches.first { !it.active } ?: dummyTouch

	fun updateTouches() {
		activeTouches.clear()
		for (touch in touches) if (touch.active) activeTouches.add(touch)
	}

	val mouse = Point(-1000.0, -1000.0)
	var mouseButtons = 0
	var clicked = false
	val keysRaw = BooleanArray(KEYCODES)
	val keysRawPrev = BooleanArray(KEYCODES)
	val keysPressingTime = IntArray(KEYCODES)
	val keysLastTimeTriggered = IntArray(KEYCODES)
	val keys = BooleanArray(KEYCODES)
	val keysJustPressed = BooleanArray(KEYCODES)
	val keysJustReleased = BooleanArray(KEYCODES)
	val gamepads = (0 until 8).map { GamepadInfo(it) }.toTypedArray()
	val connectedGamepads = arrayListOf<GamepadInfo>()

	fun updateConnectedGamepads() {
		connectedGamepads.clear()
		for (gamepad in gamepads) if (gamepad.connected) connectedGamepads += gamepad
	}

	fun setKey(keyCode: Int, b: Boolean) {
		val pKeyCode = keyCode and 0xFF
		if (pKeyCode in keysRaw.indices) keysRaw[pKeyCode] = b
	}

	fun startFrame(dtMs: Int) {
		this.extra?.clear()
	}

	fun endFrame(dtMs: Int) {
		this.clicked = false

		for (n in 0 until KEYCODES) {
			val prev = keysRawPrev[n]
			val curr = keysRaw[n]
			keysJustReleased[n] = prev && !curr
			keysJustPressed[n] = !prev && curr
			if (curr) {
				keysPressingTime[n] += dtMs
			} else {
				keysPressingTime[n] = 0
				keysLastTimeTriggered[n] = 0
			}
			var triggerPress = false
			val pressingTime = keysPressingTime[n]
			if (keysPressingTime[n] > 0) {
				val timeBarrier = when (pressingTime) {
					in 0 until 1 -> 0
					in 1 until 300 -> 100
					in 300 until 1000 -> 50
					else -> 20
				}

				val elapsedTime = pressingTime - keysLastTimeTriggered[n]
				if (elapsedTime >= timeBarrier) {
					triggerPress = true
				}
			}
			if (triggerPress) {
				keysLastTimeTriggered[n] = keysPressingTime[n]
			}
			keys[n] = triggerPress
		}

		arraycopy(keysRaw, 0, keysRawPrev, 0, KEYCODES)
	}
}
