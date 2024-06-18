package korlibs.korge.input

import korlibs.datastructure.Extra
import korlibs.datastructure.clear
import korlibs.datastructure.iterators.fastForEach
import korlibs.memory.arraycopy
import korlibs.memory.setBits
import korlibs.graphics.gl.AGOpenglFactory
import korlibs.event.GamepadInfo
import korlibs.event.Key
import korlibs.event.KeyEvent
import korlibs.event.MouseButton
import korlibs.event.Touch
import korlibs.event.TouchEvent
import korlibs.korge.internal.KorgeInternal
import korlibs.math.geom.*
import korlibs.time.*
import kotlin.time.*

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
    fun updateTouches(touchEvent: TouchEvent) {
        touch.copyFrom(touchEvent)
    }

    /** Configures the delay time to consider a mouse up event a click */
    var clickTime = 400.milliseconds
    /** Configures the distance from down to up to consider a finger up event a tap */
    var clickDistance = 20.0 // @TODO: We should take into account pointSize/DPI

    // Mouse coordinates relative to the Stage
    private var _mouse: Point = Point(-1000.0, -1000.0)
    private var _mouseDown: Point = Point(-1000.0, -1000.0)
    val mousePos: Point get() = _mouse
    val mouseDownPos: Point get() = _mouseDown

    @KorgeInternal fun setMouseGlobalPos(p: Point, down: Boolean = false) {
        if (down) _mouseDown = p else _mouse = p
        //println("setMouseGlobalXY: x=$x, y=$y, down=$down")
        //if (x == 5.0) println("-----")
    }

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
    fun startFrame(delta: FastDuration) {
        this.extra?.clear()
        keys.startFrame(delta)
    }

    @KorgeInternal
    fun endFrame(delta: FastDuration) {
        this.clicked = false
        keys.endFrame(delta)
        endFrameOldKeys(delta)
    }

    private fun endFrameOldKeys(delta: FastDuration) {
        for (n in 0 until KEYCODES) {
            val prev = keysRawPrev[n]
            val curr = keysRaw[n]
            keysJustReleased[n] = prev && !curr
            keysJustPressed[n] = !prev && curr
            if (curr) {
                keysPressingTime[n] += delta.fastNanoseconds
            } else {
                keysPressingTime[n] = 0.0
                keysLastTimeTriggered[n] = 0.0
            }
            var triggerPress = false
            val pressingTime = keysPressingTime[n].fastNanoseconds
            if (keysPressingTime[n] > 0) {
                val timeBarrier = when (pressingTime.fastMilliseconds) {
                    in 0.0..1.0 -> 0.0.fastMilliseconds
                    in 1.0..300.0 -> 100.0.fastMilliseconds
                    in 300.0..1000.0 -> 50.0.fastMilliseconds
                    else -> 20.0.fastMilliseconds
                }

                val elapsedTime = pressingTime - keysLastTimeTriggered[n].fastNanoseconds
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
    fun triggerOldKeyEvent(e: KeyEvent) {
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

    fun triggerKeyEvent(key: Key, up: Boolean, shift: Boolean = false, ctrl: Boolean = false, alt: Boolean = false, meta: Boolean = false) {
        when (up) {
            true -> pressing[key.ordinal] = false
            false -> pressing[key.ordinal] = true
        }

        this.shift = shift || this[Key.LEFT_SHIFT] || this[Key.RIGHT_SHIFT]
        this.ctrl = ctrl || this[Key.LEFT_CONTROL] || this[Key.RIGHT_CONTROL]
        this.alt = alt || this[Key.LEFT_ALT] || this[Key.RIGHT_ALT]
        this.meta = meta || this[Key.META]
    }

    @KorgeInternal
    fun triggerKeyEvent(e: KeyEvent) {
        when (e.type) {
            KeyEvent.Type.UP,  KeyEvent.Type.DOWN -> {
                triggerKeyEvent(e.key, e.type == KeyEvent.Type.UP, shift, ctrl, alt, meta)
            }
            else -> Unit
        }
    }

    internal fun startFrame(delta: FastDuration) {
    }

    internal fun endFrame(delta: FastDuration) {
        arraycopy(pressing, 0, pressingPrev, 0, pressing.size)
    }

    fun getDeltaAxis(minus1: Key, plus1: Key): Double =
        if (this[minus1]) -1.0 else if (this[plus1]) +1.0 else 0.0
}
