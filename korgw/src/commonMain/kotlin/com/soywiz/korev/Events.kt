package com.soywiz.korev

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import kotlin.jvm.*

data class MouseEvent(
    var type: Type = Type.MOVE,
    var id: Int = 0,
    var x: Int = 0,
    var y: Int = 0,
    var button: MouseButton = MouseButton.NONE,
    var buttons: Int = 0,
    @Deprecated("Use scrollDeltaX variants")
    var scrollDeltaX: Double = 0.0,
    @Deprecated("Use scrollDeltaY variants")
    var scrollDeltaY: Double = 0.0,
    @Deprecated("Use scrollDeltaZ variants")
    var scrollDeltaZ: Double = 0.0,
    var isShiftDown: Boolean = false,
    var isCtrlDown: Boolean = false,
    var isAltDown: Boolean = false,
    var isMetaDown: Boolean = false,
    var scaleCoords: Boolean = true,
    /** Not direct user mouse input. Maybe event generated from touch events? */
    var emulated: Boolean = false
) : Event() {
    var component: Any? = null

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
        this.emulated = other.emulated
        this.scrollDeltaMode = other.scrollDeltaMode
    }

    var scrollDeltaMode: ScrollDeltaMode = ScrollDeltaMode.LINE
    enum class ScrollDeltaMode(val scale: Double) {
        PIXEL(1.0),
        LINE(10.0),
        PAGE(100.0);

        fun convertTo(value: Double, target: ScrollDeltaMode): Double = value * (this.scale / target.scale)
    }

    fun scrollDeltaX(mode: ScrollDeltaMode): Double = this.scrollDeltaMode.convertTo(this.scrollDeltaX, mode)
    fun scrollDeltaY(mode: ScrollDeltaMode): Double = this.scrollDeltaMode.convertTo(this.scrollDeltaY, mode)
    fun scrollDeltaZ(mode: ScrollDeltaMode): Double = this.scrollDeltaMode.convertTo(this.scrollDeltaZ, mode)

    inline val scrollDeltaXPixels: Double get() = scrollDeltaX(ScrollDeltaMode.PIXEL)
    inline val scrollDeltaYPixels: Double get() = scrollDeltaY(ScrollDeltaMode.PIXEL)
    inline val scrollDeltaZPixels: Double get() = scrollDeltaZ(ScrollDeltaMode.PIXEL)

    inline val scrollDeltaXLines: Double get() = scrollDeltaX(ScrollDeltaMode.LINE)
    inline val scrollDeltaYLines: Double get() = scrollDeltaY(ScrollDeltaMode.LINE)
    inline val scrollDeltaZLines: Double get() = scrollDeltaZ(ScrollDeltaMode.LINE)

    inline val scrollDeltaXPages: Double get() = scrollDeltaX(ScrollDeltaMode.PAGE)
    inline val scrollDeltaYPages: Double get() = scrollDeltaY(ScrollDeltaMode.PAGE)
    inline val scrollDeltaZPages: Double get() = scrollDeltaZ(ScrollDeltaMode.PAGE)

    var requestLock: () -> Unit = { }
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
	var id: Int = -1,
    var x: Double = 0.0,
    var y: Double = 0.0,
    var force: Double = 1.0,
    var status: Status = Status.KEEP,
    var kind: Kind = Kind.FINGER,
    var button: MouseButton = MouseButton.LEFT,
) : Extra by Extra.Mixin() {
    enum class Status { ADD, KEEP, REMOVE }
    enum class Kind { FINGER, MOUSE, STYLUS, ERASER, UNKNOWN }

    val isActive: Boolean get() = status != Status.REMOVE

	companion object {
		val dummy = Touch(-1)
	}

    fun copyFrom(other: Touch) {
        this.id = other.id
        this.x = other.x
        this.y = other.y
        this.force = other.force
        this.status = other.status
        this.kind = other.kind
        this.button = other.button
    }

    override fun hashCode(): Int = index
    override fun equals(other: Any?): Boolean = other is Touch && this.index == other.index

    fun toStringNice() = "Touch[${id}][${status}](${x.niceStr},${y.niceStr})"
}

// On JS: each event contains the active down touches (ontouchend simply don't include the touch that has been removed)
// On Android: ...
// On iOS: each event contains partial touches with things that have changed for that specific event
class TouchBuilder {
    val old = TouchEvent()
    val new = TouchEvent()
    var mode = Mode.JS

    enum class Mode { JS, Android, IOS }

    fun startFrame(type: TouchEvent.Type, scaleCoords: Boolean = false) {
        new.scaleCoords = scaleCoords
        new.startFrame(type)
        when (mode) {
            Mode.IOS -> {
                new.copyFrom(old)
                new.type = type
                new._touches.fastIterateRemove {
                    if (it.isActive) {
                        it.status = Touch.Status.KEEP
                        false
                    } else {
                        new._touchesById.remove(it.id)
                        true
                    }
                }
            }
        }
    }

    fun endFrame(): TouchEvent {
        when (mode) {
            Mode.JS -> {
                old.touches.fastForEach { oldTouch ->
                    if (new.getTouchById(oldTouch.id) == null) {
                        if (oldTouch.isActive) {
                            oldTouch.status = Touch.Status.REMOVE
                            new.touch(oldTouch)
                        }
                    }
                }
            }
            Mode.IOS -> {

            }
        }
        new.endFrame()
        old.copyFrom(new)
        return new
    }

    inline fun frame(mode: Mode, type: TouchEvent.Type, scaleCoords: Boolean = false, block: TouchBuilder.() -> Unit): TouchEvent {
        this.mode = mode
        startFrame(type, scaleCoords)
        try {
            block()
        } finally {
            endFrame()
        }
        return new
    }

    fun touch(id: Int, x: Double, y: Double, force: Double = 1.0, kind: Touch.Kind = Touch.Kind.FINGER, button: MouseButton = MouseButton.LEFT) {
        val touch = new.getOrAllocTouchById(id)
        touch.x = x
        touch.y = y
        touch.force = force
        touch.kind = kind
        touch.button = button

        when (mode) {
            Mode.IOS -> {
                touch.status = when (new.type) {
                    TouchEvent.Type.START -> Touch.Status.ADD
                    TouchEvent.Type.END -> Touch.Status.REMOVE
                    else -> Touch.Status.KEEP
                }
            }
            else -> {
                val oldTouch = old.getTouchById(id)
                touch.status = if (oldTouch == null) Touch.Status.ADD else Touch.Status.KEEP
            }
        }
    }
}

data class TouchEvent(
    var type: Type = Type.START,
    var screen: Int = 0,
    var currentTime: DateTime = DateTime.EPOCH,
    var scaleCoords: Boolean = true,
    var emulated: Boolean = false
) : Event() {
    companion object {
        val MAX_TOUCHES = 10
    }
    private val bufferTouches = Array(MAX_TOUCHES) { Touch(it) }
    internal val _touches = FastArrayList<Touch>()
    internal val _activeTouches = FastArrayList<Touch>()
    internal val _touchesById = FastIntMap<Touch>()
    val touches: List<Touch> get() = _touches
    val activeTouches: List<Touch> get() = _activeTouches
    val numTouches get() = touches.size
    val numActiveTouches get() = activeTouches.size

    fun getTouchById(id: Int) = _touchesById[id]

    override fun toString(): String = "TouchEvent[$type][$numTouches](${touches.joinToString(", ") { it.toString() }})"

    fun startFrame(type: Type) {
        this.type = type
        this.currentTime = DateTime.now()
        _touches.clear()
        _touchesById.clear()
    }

    fun endFrame() {
        _activeTouches.clear()
        touches.fastForEach {
            if (it.isActive) _activeTouches.add(it)
        }
    }

    fun getOrAllocTouchById(id: Int): Touch {
        return _touchesById[id] ?: allocTouchById(id)
    }

    fun allocTouchById(id: Int): Touch {
        val touch = bufferTouches[_touches.size]
        touch.id = id
        _touches.add(touch)
        _touchesById[touch.id] = touch
        return touch
    }

    fun touch(id: Int, x: Double, y: Double, status: Touch.Status = Touch.Status.KEEP, force: Double = 1.0, kind: Touch.Kind = Touch.Kind.FINGER, button: MouseButton = MouseButton.LEFT) {
        val touch = getOrAllocTouchById(id)
        touch.x = x
        touch.y = y
        touch.status = status
        touch.force = force
        touch.kind = kind
        touch.button = button
    }

    fun touch(touch: Touch) {
        touch(touch.id, touch.x, touch.y, touch.status, touch.force, touch.kind, touch.button)
    }

    fun copyFrom(other: TouchEvent) {
        this.type = other.type
        this.screen = other.screen
        this.currentTime = other.currentTime
        this.scaleCoords = other.scaleCoords
        this.emulated = other.emulated
        for (n in 0 until MAX_TOUCHES) {
            bufferTouches[n].copyFrom(other.bufferTouches[n])
        }
        this._touches.clear()
        this._activeTouches.clear()
        this._touchesById.clear()
        other.touches.fastForEach { otherTouch ->
            val touch = bufferTouches[otherTouch.index]
            this._touches.add(touch)
            if (touch.isActive) {
                this._activeTouches.add(touch)
            }
            this._touchesById[touch.id] = touch
        }
    }

    fun clone() = TouchEvent().also { it.copyFrom(this) }

    val isStart get() = type == Type.START
    val isEnd get() = type == Type.END

    enum class Type { START, END, MOVE, HOVER, UNKNOWN }
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
    var deltaTime = TimeSpan.ZERO

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
        this.deltaTime = other.deltaTime
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
    val gamepads: Array<GamepadInfo> = Array(8) { GamepadInfo(it) }
) : Event() {
    fun copyFrom(that: GamePadUpdateEvent) {
        this.gamepadsLength = that.gamepadsLength
        for (n in 0 until gamepads.size) {
            this.gamepads[n].copyFrom(that.gamepads[n])
        }
    }

    override fun toString(): String = "GamePadUpdateEvent(${gamepads.filter { it.connected }})"
}

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

data class DropFileEvent(var type: Type = Type.START, var files: List<VfsFile>? = null) : Event() {
	enum class Type { START, END, DROP }

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
