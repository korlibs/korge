package com.soywiz.korge.input

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korma.geom.*
import com.soywiz.korev.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.time.HRTimeSpan

//@Singleton
@OptIn(KorgeInternal::class)
class Input : Extra by Extra.Mixin() {
	companion object {
		const val KEYCODES = 0x100
	}

	val dummyTouch = Touch.dummy
	val touches = (0 until 16).map { Touch(it) }.toTypedArray()
	val activeTouches = arrayListOf<Touch>()

    @KorgeInternal
	var _isTouchDeviceGen = { AGOpenglFactory.isTouchDevice }

	val isTouchDevice: Boolean get() = _isTouchDeviceGen()

	fun getTouch(id: Int) = touches.firstOrNull { it.id == id } ?: touches.first { !it.active } ?: dummyTouch

    @KorgeInternal
	fun updateTouches() {
		activeTouches.clear()
		touches.fastForEach { touch ->
			if (touch.active) activeTouches.add(touch)
		}
	}

	val mouse = Point(-1000.0, -1000.0)
	var mouseButtons = 0
    var mouseInside = true
	var clicked = false

    val keys = InputKeys()
    @KorgeInternal
	val keysRaw = BooleanArray(KEYCODES)
    @KorgeInternal
    val keysRawPrev = BooleanArray(KEYCODES)
    @KorgeInternal
	val keysPressingTime = DoubleArray(KEYCODES)
    @KorgeInternal
	val keysLastTimeTriggered = DoubleArray(KEYCODES)
    @KorgeInternal
	val keysPressing = BooleanArray(KEYCODES)
    @KorgeInternal
	val keysJustPressed = BooleanArray(KEYCODES)
    @KorgeInternal
	val keysJustReleased = BooleanArray(KEYCODES)

	val gamepads = (0 until 8).map { GamepadInfo(it) }.toTypedArray()
	val connectedGamepads = arrayListOf<GamepadInfo>()

	fun updateConnectedGamepads() {
		connectedGamepads.clear()
		gamepads.fastForEach { gamepad ->
			if (gamepad.connected) connectedGamepads += gamepad
		}
	}

    @KorgeInternal
	fun setKey(keyCode: Int, b: Boolean) {
		val pKeyCode = keyCode and 0xFF
		if (pKeyCode in keysRaw.indices) keysRaw[pKeyCode] = b
	}

    @KorgeInternal
	fun startFrame(delta: HRTimeSpan) {
		this.extra?.clear()
        keys.startFrame(delta)
	}

    @KorgeInternal
    fun endFrame(delta: HRTimeSpan) {
		this.clicked = false
        keys.endFrame(delta)
        endFrameOldKeys(delta)
	}

    private fun endFrameOldKeys(delta: HRTimeSpan) {
        for (n in 0 until KEYCODES) {
            val prev = keysRawPrev[n]
            val curr = keysRaw[n]
            keysJustReleased[n] = prev && !curr
            keysJustPressed[n] = !prev && curr
            if (curr) {
                keysPressingTime[n] += delta.nanosecondsDouble
            } else {
                keysPressingTime[n] = 0.0
                keysLastTimeTriggered[n] = 0.0
            }
            var triggerPress = false
            val pressingTime = HRTimeSpan.fromNanoseconds(keysPressingTime[n])
            if (keysPressingTime[n] > 0) {
                val timeBarrier = when (pressingTime.millisecondsDouble) {
                    in 0.0 .. 1.0 -> HRTimeSpan.fromMilliseconds(0)
                    in 1.0 .. 300.0 -> HRTimeSpan.fromMilliseconds(100)
                    in 300.0 .. 1000.0 -> HRTimeSpan.fromMilliseconds(50)
                    else -> HRTimeSpan.fromMilliseconds(20)
                }

                val elapsedTime = pressingTime - HRTimeSpan.fromNanoseconds(keysLastTimeTriggered[n])
                if (elapsedTime >= timeBarrier) {
                    triggerPress = true
                }
            }
            if (triggerPress) {
                keysLastTimeTriggered[n] = keysPressingTime[n]
            }
            keysPressing[n] = triggerPress
        }

        arraycopy(keysRaw, 0, keysRawPrev, 0, KEYCODES)
    }

    @KorgeInternal
    internal fun triggerOldKeyEvent(e: KeyEvent) {
        when (e.type) {
            KeyEvent.Type.DOWN -> {
                setKey(e.keyCode, true)
            }
            KeyEvent.Type.UP -> {
                setKey(e.keyCode, false)
            }
            KeyEvent.Type.TYPE -> {
                //println("onKeyTyped: $it")
            }
        }
    }
}

class InputKeys {
    private val pressing = BooleanArray(Key.MAX)
    private val pressingPrev = BooleanArray(Key.MAX)

    operator fun get(key: Key) = pressing(key)
    fun pressing(key: Key): Boolean = pressing[key.ordinal]
    fun justPressed(key: Key): Boolean = pressing[key.ordinal] && !pressingPrev[key.ordinal]
    fun justReleased(key: Key): Boolean = !pressing[key.ordinal] && pressingPrev[key.ordinal]

    @KorgeInternal
    fun triggerKeyEvent(e: KeyEvent) {
        when (e.type) {
            KeyEvent.Type.UP -> pressing[e.key.ordinal] = false
            KeyEvent.Type.DOWN -> pressing[e.key.ordinal] = true
            else -> Unit
        }
    }

    internal fun startFrame(delta: HRTimeSpan) {
    }
    internal fun endFrame(delta: HRTimeSpan) {
        arraycopy(pressing, 0, pressingPrev, 0, pressing.size)
    }
}
