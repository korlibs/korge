package com.soywiz.korge.input

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.klock.nanoseconds
import com.soywiz.kmem.*
import com.soywiz.korag.gl.*
import com.soywiz.korev.*
import com.soywiz.korge.internal.*
import com.soywiz.korma.geom.*

//@Singleton
@OptIn(KorgeInternal::class)
class Input : Extra by Extra.Mixin() {
    companion object {
        const val KEYCODES = 0x100
    }

    val dummyTouch = Touch.dummy

    /** Last [TouchEvent] emitted */
    val touch: TouchEvent = TouchEvent()
    /** All the available touches including the ones that just ended */
    val touches: List<Touch> get() = touch.touches
    /** All the touches that are active (recently created or updated) */
    val activeTouches: List<Touch> get() = touch.activeTouches

    val numActiveTouches get() = activeTouches.size

    @KorgeInternal
    internal var _isTouchDeviceGen = { AGOpenglFactory.isTouchDevice }

    val isTouchDevice: Boolean get() = _isTouchDeviceGen()

    @Deprecated("")
    //fun getTouch(id: Int) = touches.firstOrNull { it.id == id } ?: touches.first { !it.active } ?: dummyTouch
    fun getTouch(id: Int) = touches.firstOrNull { it.id == id } ?: dummyTouch

    @KorgeInternal
    internal fun updateTouches(touchEvent: TouchEvent) {
        touch.copyFrom(touchEvent)
    }

    /** Configures the delay time to consider a mouse up event a click */
    var clickTime = 400.milliseconds
    /** Configures the distance from down to up to consider a finger up event a tap */
    var clickDistance = 20.0 // @TODO: We should take into account pointSize/DPI

    val mouse = Point(-1000.0, -1000.0)
    val mouseDown = Point(-1000.0, -1000.0)

    /** BitField with pressed mouse buttons */
    var mouseButtons = 0

    /** Determine if a mouse button is pressed */
    fun mouseButtonPressed(button: MouseButton) = button.pressedFromFlags(mouseButtons)

    operator fun get(button: MouseButton) = mouseButtonPressed(button)

    var mouseOutside = false
    var mouseInside = true
    var clicked = false

    @KorgeInternal
    fun toggleButton(button: MouseButton, down: Boolean) {
        mouseButtons = mouseButtons.setBits(button.bits, down)
    }

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
    fun startFrame(delta: TimeSpan) {
        this.extra?.clear()
        keys.startFrame(delta)
    }

    @KorgeInternal
    fun endFrame(delta: TimeSpan) {
        this.clicked = false
        keys.endFrame(delta)
        endFrameOldKeys(delta)
    }

    private fun endFrameOldKeys(delta: TimeSpan) {
        for (n in 0 until KEYCODES) {
            val prev = keysRawPrev[n]
            val curr = keysRaw[n]
            keysJustReleased[n] = prev && !curr
            keysJustPressed[n] = !prev && curr
            if (curr) {
                keysPressingTime[n] += delta.nanoseconds
            } else {
                keysPressingTime[n] = 0.0
                keysLastTimeTriggered[n] = 0.0
            }
            var triggerPress = false
            val pressingTime = keysPressingTime[n].nanoseconds
            if (keysPressingTime[n] > 0) {
                val timeBarrier = when (pressingTime.milliseconds) {
                    in 0.0..1.0 -> 0.0.milliseconds
                    in 1.0..300.0 -> 100.0.milliseconds
                    in 300.0..1000.0 -> 50.0.milliseconds
                    else -> 20.0.milliseconds
                }

                val elapsedTime = pressingTime - keysLastTimeTriggered[n].nanoseconds
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
    var shift: Boolean = false ; private set
    var ctrl: Boolean = false ; private set
    var alt: Boolean = false ; private set
    var meta: Boolean = false ; private set

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

        shift = e.shift || this[Key.LEFT_SHIFT] || this[Key.RIGHT_SHIFT]
        ctrl = e.ctrl || this[Key.LEFT_CONTROL] || this[Key.RIGHT_CONTROL]
        alt = e.alt || this[Key.LEFT_ALT] || this[Key.RIGHT_ALT]
        meta = e.meta || this[Key.META]
    }

    internal fun startFrame(delta: TimeSpan) {
    }

    internal fun endFrame(delta: TimeSpan) {
        arraycopy(pressing, 0, pressingPrev, 0, pressing.size)
    }

    fun getDeltaAxis(minus1: Key, plus1: Key): Double =
        if (this[minus1]) -1.0 else if (this[plus1]) +1.0 else 0.0
}
