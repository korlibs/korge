package com.soywiz.korev

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import kotlin.jvm.*

data class MouseEvent(
    var type: Type = Type.MOVE,
    var id: Int = 0,
    var x: Int = 0,
    var y: Int = 0,
    var button: MouseButton = MouseButton.LEFT,
    var buttons: Int = 0,
    var scrollDeltaX: Double = 0.0,
    var scrollDeltaY: Double = 0.0,
    var scrollDeltaZ: Double = 0.0,
    var isShiftDown: Boolean = false,
    var isCtrlDown: Boolean = false,
    var isAltDown: Boolean = false,
    var isMetaDown: Boolean = false,
    var scaleCoords: Boolean = true
) : Event() {
	enum class Type { MOVE, DRAG, UP, DOWN, CLICK, ENTER, EXIT, SCROLL }

    val typeMove get() = type == Type.MOVE
    val typeDrag get() = type == Type.DRAG
    val typeUp get() = type == Type.UP
    val typeDown get() = type == Type.DOWN
    val typeClick get() = type == Type.CLICK
    val typeEnter get() = type == Type.ENTER
    val typeExit get() = type == Type.EXIT
    val typeScroll get() = type == Type.SCROLL

    fun copyFrom(other: MouseEvent) {
        this.type = other.type
        this.id = other.id
        this.x = other.x
        this.y = other.y
        this.button = other.button
        this.buttons = other.buttons
        this.scrollDeltaX = other.scrollDeltaX
        this.scrollDeltaY = other.scrollDeltaY
        this.scrollDeltaZ = other.scrollDeltaZ
        this.isShiftDown = other.isShiftDown
        this.isCtrlDown = other.isCtrlDown
        this.isAltDown = other.isAltDown
        this.isMetaDown = other.isMetaDown
        this.scaleCoords = other.scaleCoords
    }
}

data class FocusEvent(
    var type: Type = Type.FOCUS
) {
    enum class Type { FOCUS, BLUR }
    val typeFocus get() = type == Type.FOCUS
    val typeBlur get() = type == Type.BLUR
}

data class Touch(
	val index: Int = -1,
	var active: Boolean = false,
	var id: Int = -1,
	var startTime: DateTime = DateTime.EPOCH,
    var currentTime: DateTime = DateTime.EPOCH,
	val start: Point = Point(),
	val current: Point = Point()
) : Extra by Extra.Mixin() {
	companion object {
		val dummy = Touch(-1)
	}

    fun copyFrom(other: Touch) {
        this.active = other.active
        this.id = other.id
        this.startTime = other.startTime
        this.start.copyFrom(other.start)
        this.current.copyFrom(other.current)
    }
}

data class TouchEvent(
    var type: Type = Type.START,
    var screen: Int = 0,
    var startTime: DateTime = DateTime.EPOCH,
    var currentTime: DateTime = DateTime.EPOCH,
    var scaleCoords: Boolean = true
) : Event() {
    companion object {
        val MAX_TOUCHES = 10
    }
    private val bufferTouches = Array(MAX_TOUCHES) { Touch(it) }
    private val _touches = LinkedHashSet<Touch>()
    val touches: Set<Touch> get() = _touches

    fun startFrame(type: Type) {
        this.type = type
        if (type == com.soywiz.korev.TouchEvent.Type.START) {
            startTime = DateTime.now()
            for (touch in bufferTouches) touch.id = -1
        }
        currentTime = DateTime.now()
        if (type != Type.END) {
            for (touch in bufferTouches) touch.active = false
            _touches.clear()
        }
    }

    fun getTouchById(id: Int) = bufferTouches.firstOrNull { it.id == id }
        ?: bufferTouches.firstOrNull { it.id == -1 }
        ?: bufferTouches.firstOrNull { !it.active }
        ?: bufferTouches[MAX_TOUCHES - 1]

    fun touch(id: Int, x: Double, y: Double) {
        val touch = getTouchById(id)
        touch.id = id
        touch.active = true
        touch.currentTime = currentTime
        touch.current.x = x
        touch.current.y = y
        if (type == Type.START) {
            touch.startTime = currentTime
            touch.start.x = x
            touch.start.y = y
        }
        _touches.add(touch)
    }

    fun copyFrom(other: TouchEvent) {
        this.type = other.type
        this.screen = other.screen
        this.startTime = other.startTime
        this.currentTime = other.currentTime
        this.scaleCoords = other.scaleCoords
        for (n in 0 until MAX_TOUCHES) {
            bufferTouches[n].copyFrom(other.bufferTouches[n])
        }
    }

    enum class Type { START, END, MOVE }
}

data class KeyEvent constructor(
    var type: Type = Type.UP,
    var id: Int = 0,
    var key: Key = Key.UP,
    var keyCode: Int = 0,
    //var char: Char = '\u0000' // @TODO: This caused problem on Kotlin/Native because it is a keyword (framework H)
    var character: Char = '\u0000',
    var shift: Boolean = false,
    var ctrl: Boolean = false,
    var alt: Boolean = false,
    var meta: Boolean = false,
) : Event() {
    val typeType get() = type == Type.TYPE
    val typeDown get() = type == Type.DOWN
    val typeUp get() = type == Type.UP

    val ctrlOrMeta: Boolean get() = if (OS.isMac) meta else ctrl

	enum class Type { UP, DOWN, TYPE }

    fun copyFrom(other: KeyEvent) {
        this.type = other.type
        this.id = other.id
        this.key = other.key
        this.keyCode = other.keyCode
        this.character = other.character
        this.shift = other.shift
        this.ctrl = other.ctrl
        this.alt = other.alt
        this.meta = other.meta
    }
}

data class GamePadConnectionEvent(var type: Type = Type.CONNECTED, var gamepad: Int = 0) : Event() {
	enum class Type { CONNECTED, DISCONNECTED }

    fun copyFrom(other: GamePadConnectionEvent) {
        this.type = other.type
        this.gamepad = other.gamepad
    }
}

@Suppress("ArrayInDataClass")
data class GamePadUpdateEvent @JvmOverloads constructor(
    var gamepadsLength: Int = 0,
    val gamepads: Array<GamepadInfo> = Array(8) { GamepadInfo() }
) : Event() {
    fun copyFrom(that: GamePadUpdateEvent) {
        this.gamepadsLength = that.gamepadsLength
        for (n in 0 until gamepads.size) {
            this.gamepads[n].copyFrom(that.gamepads[n])
        }
    }

    override fun toString(): String = "GamePadUpdateEvent(${gamepads.filter { it.connected }})"
}

//@Deprecated("")
data class GamePadButtonEvent @JvmOverloads constructor(
    var type: Type = Type.DOWN,
    var gamepad: Int = 0,
    var button: GameButton = GameButton.BUTTON0,
    var value: Double = 0.0
) : Event() {
	enum class Type { UP, DOWN }

    fun copyFrom(other: GamePadButtonEvent) {
        this.type = other.type
        this.gamepad = other.gamepad
        this.button = other.button
        this.value = other.value
    }
}

//@Deprecated("")
data class GamePadStickEvent(
    var gamepad: Int = 0,
    var stick: GameStick = GameStick.LEFT,
    var x: Double = 0.0,
    var y: Double = 0.0
) : Event() {
    fun copyFrom(other: GamePadStickEvent) {
        this.gamepad = other.gamepad
        this.stick = other.stick
        this.x = other.x
        this.y = other.y
    }
}

data class ChangeEvent(var oldValue: Any? = null, var newValue: Any? = null) : Event() {
    fun copyFrom(other: ChangeEvent) {
        this.oldValue = other.oldValue
        this.newValue = other.newValue
    }
}

data class ReshapeEvent(var x: Int = 0, var y: Int = 0, var width: Int = 0, var height: Int = 0) : Event() {
    fun copyFrom(other: ReshapeEvent) {
        this.x = other.x
        this.y = other.y
        this.width = other.width
        this.height = other.height
    }
}

data class FullScreenEvent(var fullscreen: Boolean = false) : Event() {
    fun copyFrom(other: FullScreenEvent) {
        this.fullscreen = other.fullscreen
    }
}

class RenderEvent() : Event() {
    var update: Boolean = true
    fun copyFrom(other: RenderEvent) {
        this.update = other.update
    }
}

class InitEvent() : Event() {
    fun copyFrom(other: InitEvent) {
    }
}

class ResumeEvent() : Event() {
    fun copyFrom(other: ResumeEvent) {
    }
}

class PauseEvent() : Event() {
    fun copyFrom(other: PauseEvent) {
    }
}

class StopEvent() : Event() {
    fun copyFrom(other: StopEvent) {
    }
}

class DestroyEvent() : Event() {
    fun copyFrom(other: DestroyEvent) {
    }
}

class DisposeEvent() : Event() {
    fun copyFrom(other: DisposeEvent) {
    }
}

data class DropFileEvent(var type: Type = Type.ENTER, var files: List<VfsFile>? = null) : Event() {
	enum class Type { ENTER, EXIT, DROP }

    fun copyFrom(other: DropFileEvent) {
        this.type = other.type
        this.files = other.files?.toList()
    }
}

class MouseEvents(val ed: EventDispatcher) : Closeable {
	fun click(callback: MouseEvent.() -> Unit) = ed.addEventListener<MouseEvent> { if (it.type == MouseEvent.Type.CLICK) callback(it) }
	fun up(callback: MouseEvent.() -> Unit) = ed.addEventListener<MouseEvent> { if (it.type == MouseEvent.Type.UP) callback(it) }
	fun down(callback: MouseEvent.() -> Unit) = ed.addEventListener<MouseEvent> { if (it.type == MouseEvent.Type.DOWN) callback(it) }
	fun move(callback: MouseEvent.() -> Unit) = ed.addEventListener<MouseEvent> { if (it.type == MouseEvent.Type.MOVE) callback(it) }
	fun drag(callback: MouseEvent.() -> Unit) = ed.addEventListener<MouseEvent> { if (it.type == MouseEvent.Type.DRAG) callback(it) }
	fun enter(callback: MouseEvent.() -> Unit) = ed.addEventListener<MouseEvent> { if (it.type == MouseEvent.Type.ENTER) callback(it) }
    fun scroll(callback: MouseEvent.() -> Unit) = ed.addEventListener<MouseEvent> { if (it.type == MouseEvent.Type.SCROLL) callback(it) }
	fun exit(callback: MouseEvent.() -> Unit) = ed.addEventListener<MouseEvent> { if (it.type == MouseEvent.Type.EXIT) callback(it) }
	override fun close() {
	}
}

class KeysEvents(val ed: EventDispatcher) : Closeable {
	fun down(callback: KeyEvent.() -> Unit) =
		ed.addEventListener<KeyEvent> { if (it.type == KeyEvent.Type.DOWN) callback(it) }

	fun up(callback: KeyEvent.() -> Unit) =
		ed.addEventListener<KeyEvent> { if (it.type == KeyEvent.Type.UP) callback(it) }

	fun press(callback: KeyEvent.() -> Unit) =
		ed.addEventListener<KeyEvent> { if (it.type == KeyEvent.Type.TYPE) callback(it) }

	fun down(key: Key, callback: KeyEvent.() -> Unit) =
		ed.addEventListener<KeyEvent> { if (it.type == KeyEvent.Type.DOWN && it.key == key) callback(it) }

	fun up(key: Key, callback: KeyEvent.() -> Unit) =
		ed.addEventListener<KeyEvent> { if (it.type == KeyEvent.Type.UP && it.key == key) callback(it) }

	fun press(key: Key, callback: KeyEvent.() -> Unit) =
		ed.addEventListener<KeyEvent> { if (it.type == KeyEvent.Type.TYPE && it.key == key) callback(it) }

	override fun close() {
	}
}

/*
@Deprecated("")
fun EventDispatcher.mouse(callback: MouseEvents.() -> Unit) = MouseEvents(this).apply(callback)
@Deprecated("")
fun EventDispatcher.keys(callback: KeysEvents.() -> Unit) = KeysEvents(this).apply(callback)
*/
