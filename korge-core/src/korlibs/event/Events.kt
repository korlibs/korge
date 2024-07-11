package korlibs.event

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.io.file.*
import korlibs.math.geom.*
import korlibs.number.*
import korlibs.platform.*
import korlibs.time.*

open class TypedEvent<T : BEvent>(open override var type: EventType<T>) : Event(), TEvent<T>

// @TODO: No override required!
//open class Demo { var a: Int = 10 }
//interface BDemo { val a: Int }
//interface TBDemo<T : BDemo> : BDemo {}
//class Test : Demo(), TBDemo<Test> {}

abstract class Event {
    var target: Any? = null
    fun stopPropagation(reason: Any? = null) {
        throw StopPropagatingException(reason)
    }
    var defaultPrevented: Boolean = false
    fun preventDefault(reason: Any? = null) {
        defaultPrevented = true
    }
}

inline fun <T : Event> T.reset(block: T.() -> Unit = {}): T {
    this.defaultPrevented = false
    block(this)
    return this
}

operator fun <T : Event> T.invoke(block: T.() -> Unit): T {
    block(this)
    return this
}

@Deprecated("") fun Event.preventDefault(reason: Any? = null): Nothing = throw PreventDefaultException(reason)
@Deprecated("") fun preventDefault(reason: Any? = null): Nothing = throw PreventDefaultException(reason)
@Deprecated("") class PreventDefaultException(val reason: Any? = null) : Exception()
class StopPropagatingException(val reason: Any? = null) : Exception()

interface BEvent {
    var target: Any?
    val type: EventType<out BEvent>
    var defaultPrevented: Boolean
}

interface TEvent<T : BEvent> : BEvent {
    override val type: EventType<T>
}

interface EventType<T : BEvent>

data class GestureEvent(
    override var type: Type = Type.MAGNIFY,
    var id: Int = 0,
    var amountX: Float = 0f,
    var amountY: Float = 0f,
) : Event(), TEvent<GestureEvent> {
    var amount: Float
        get() = amountX
        set(value) {
            amountX = value
            amountY = value
        }

    enum class Type : EventType<GestureEvent> {
        MAGNIFY, ROTATE, SWIPE, SMART_MAGNIFY;
        companion object {
            val ALL = values()
        }
    }

    fun copyFrom(other: GestureEvent) {
        this.type = other.type
        this.id = other.id
        this.amountX = other.amountX
        this.amountY = other.amountY
    }
}

/** [x] and [y] positions are window-based where 0,0 is the top-left position in the window client area */
data class MouseEvent(
    override var type: Type = Type.MOVE,
    var id: Int = 0,
    var x: Int = 0,
    var y: Int = 0,
    var button: MouseButton = MouseButton.NONE,
    var buttons: Int = 0,
    @Deprecated("Use scrollDeltaX variants")
    var scrollDeltaX: Float = 0f,
    @Deprecated("Use scrollDeltaY variants")
    var scrollDeltaY: Float = 0f,
    @Deprecated("Use scrollDeltaZ variants")
    var scrollDeltaZ: Float = 0f,
    var isShiftDown: Boolean = false,
    var isCtrlDown: Boolean = false,
    var isAltDown: Boolean = false,
    var isMetaDown: Boolean = false,
    var scaleCoords: Boolean = true,
    /** Not direct user mouse input. Maybe event generated from touch events? */
    var emulated: Boolean = false,
    var scrollDeltaMode: ScrollDeltaMode = ScrollDeltaMode.LINE
) : Event(), TEvent<MouseEvent> {
    //companion object : EventType<MouseEvent>
    val pos: Vector2I get() = Vector2I(x, y)

    var component: Any? = null

	enum class Type : EventType<MouseEvent> {
        MOVE, DRAG, UP, DOWN, CLICK, ENTER, EXIT, SCROLL;
        companion object {
            val ALL = values()
        }
    }

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

    enum class ScrollDeltaMode(val scale: Float) {
        PIXEL(1f),
        LINE(10f),
        PAGE(100f);

        fun convertTo(value: Float, target: ScrollDeltaMode): Float = value * (this.scale / target.scale)
    }

    fun scrollDeltaX(mode: ScrollDeltaMode): Float = this.scrollDeltaMode.convertTo(this.scrollDeltaX, mode)
    fun scrollDeltaY(mode: ScrollDeltaMode): Float = this.scrollDeltaMode.convertTo(this.scrollDeltaY, mode)
    fun scrollDeltaZ(mode: ScrollDeltaMode): Float = this.scrollDeltaMode.convertTo(this.scrollDeltaZ, mode)

    fun setScrollDelta(mode: ScrollDeltaMode, x: Float, y: Float, z: Float) {
        this.scrollDeltaMode = mode
        this.scrollDeltaX = x
        this.scrollDeltaY = y
        this.scrollDeltaZ = z
    }

    inline val scrollDeltaXPixels: Float get() = scrollDeltaX(ScrollDeltaMode.PIXEL)
    inline val scrollDeltaYPixels: Float get() = scrollDeltaY(ScrollDeltaMode.PIXEL)
    inline val scrollDeltaZPixels: Float get() = scrollDeltaZ(ScrollDeltaMode.PIXEL)

    inline val scrollDeltaXLines: Float get() = scrollDeltaX(ScrollDeltaMode.LINE)
    inline val scrollDeltaYLines: Float get() = scrollDeltaY(ScrollDeltaMode.LINE)
    inline val scrollDeltaZLines: Float get() = scrollDeltaZ(ScrollDeltaMode.LINE)

    inline val scrollDeltaXPages: Float get() = scrollDeltaX(ScrollDeltaMode.PAGE)
    inline val scrollDeltaYPages: Float get() = scrollDeltaY(ScrollDeltaMode.PAGE)
    inline val scrollDeltaZPages: Float get() = scrollDeltaZ(ScrollDeltaMode.PAGE)

    var requestLock: () -> Unit = { }
}

data class FocusEvent(
    override var type: Type = Type.FOCUS
) : Event(), TEvent<FocusEvent> {
    enum class Type : EventType<FocusEvent> { FOCUS, BLUR }
    val typeFocus get() = type == Type.FOCUS
    val typeBlur get() = type == Type.BLUR
}

data class Touch(
	val index: Int = -1,
	var id: Int = -1,
    var p: Point = Point.ZERO,
    var force: Float = 1f,
    var status: Status = Status.KEEP,
    var kind: Kind = Kind.FINGER,
    var button: MouseButton = MouseButton.LEFT,
) : Extra by Extra.Mixin() {
    val x: Double get() = p.x
    val y: Double get() = p.y

    enum class Status { ADD, KEEP, REMOVE }
    enum class Kind { FINGER, MOUSE, STYLUS, ERASER, UNKNOWN }

    val isActive: Boolean get() = status != Status.REMOVE

	companion object {
		val dummy = Touch(-1)
	}

    fun copyFrom(other: Touch) {
        this.id = other.id
        this.p = other.p
        this.force = other.force
        this.status = other.status
        this.kind = other.kind
        this.button = other.button
    }

    override fun hashCode(): Int = index
    override fun equals(other: Any?): Boolean = other is Touch && this.index == other.index

    fun toStringNice() = "Touch[${id}][${status}](${p.x.niceStr},${p.y.niceStr})"
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
            else -> Unit
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
            else -> Unit
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

    fun touch(id: Int, p: Point, force: Float = 1f, kind: Touch.Kind = Touch.Kind.FINGER, button: MouseButton = MouseButton.LEFT) {
        val touch = new.getOrAllocTouchById(id)
        touch.p = p
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
    override var type: Type = Type.START,
    var screen: Int = 0,
    var currentTime: DateTime = DateTime.EPOCH,
    var scaleCoords: Boolean = true,
    var emulated: Boolean = false
) : Event(), TEvent<TouchEvent> {
    enum class Type : EventType<TouchEvent> {
        START, END, MOVE, HOVER, UNKNOWN;
        companion object {
            val ALL = values()
        }
    }

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

    fun touch(id: Int, p: Point, status: Touch.Status = Touch.Status.KEEP, force: Float = 1f, kind: Touch.Kind = Touch.Kind.FINGER, button: MouseButton = MouseButton.LEFT) {
        val touch = getOrAllocTouchById(id)
        touch.p = p
        touch.status = status
        touch.force = force
        touch.kind = kind
        touch.button = button
    }

    fun touch(touch: Touch) {
        touch(touch.id, touch.p, touch.status, touch.force, touch.kind, touch.button)
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
}

class GenericEvent(
    override var type: Type,
    var kind: String,
    var data: ByteArray
) : Event(), TEvent<GenericEvent> {
    enum class Type : EventType<GenericEvent> {
        PROJECTOR_TO_GAME, GAME_TO_PROJECTOR;
        companion object {
            val ALL = Type.entries
        }
    }

    fun copyFrom(other: GenericEvent) {
        this.type = other.type
        this.kind = other.kind
        this.data = other.data
    }
}

data class KeyEvent constructor(
    override var type: Type = Type.UP,
    var id: Int = 0,
    var key: Key = Key.UP,
    var keyCode: Int = 0,
    //var char: Char = '\u0000' // @TODO: This caused problem on Kotlin/Native because it is a keyword (framework H)
    var character: Char = '\u0000',
    var shift: Boolean = false,
    var ctrl: Boolean = false,
    var alt: Boolean = false,
    var meta: Boolean = false,
    var str: String? = null,
) : Event(), TEvent<KeyEvent> {
    //companion object : EventType<KeyEvent>
    enum class Type : EventType<KeyEvent> {
        UP, DOWN, TYPE;
        companion object {
            val ALL = values()
        }
    }

    var deltaTime = TimeSpan.ZERO

    val typeType get() = type == Type.TYPE
    val typeDown get() = type == Type.DOWN
    val typeUp get() = type == Type.UP

    val ctrlOrMeta: Boolean get() = if (Platform.isMac) meta else ctrl

    fun characters(): String = str ?: "$character"

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
        this.str = other.str
    }

    /** On MacOS CMD, on Linux and Windows CTRL */
    val metaOrCtrl: Boolean get() = if (Platform.os.isApple) this.meta else this.ctrl
}

data class ChangeEvent(var oldValue: Any? = null, var newValue: Any? = null) : TypedEvent<ChangeEvent>(ChangeEvent) {
    companion object : EventType<ChangeEvent>

    fun copyFrom(other: ChangeEvent) {
        this.oldValue = other.oldValue
        this.newValue = other.newValue
    }
}

data class ReshapeEvent(var x: Int = 0, var y: Int = 0, var width: Int = 0, var height: Int = 0, var setPos: Boolean = true) : TypedEvent<ReshapeEvent>(ReshapeEvent) {
    companion object : EventType<ReshapeEvent>

    fun copyFrom(other: ReshapeEvent) {
        this.x = other.x
        this.y = other.y
        this.width = other.width
        this.height = other.height
        this.setPos = other.setPos
    }
}

data class FullScreenEvent(var fullscreen: Boolean = false) : TypedEvent<FullScreenEvent>(FullScreenEvent) {
    companion object : EventType<FullScreenEvent>

    fun copyFrom(other: FullScreenEvent) {
        this.fullscreen = other.fullscreen
    }
}

open class RenderEvent : TypedEvent<RenderEvent>(RenderEvent) {
    companion object : RenderEvent(), EventType<RenderEvent>

    var update: Boolean = true
    var render: Boolean = true
    fun copyFrom(other: RenderEvent) {
        this.update = other.update
        this.render = other.render
    }

    override fun toString(): String = "RenderEvent(update=$update, render=$render)"
}

open class InitEvent : TypedEvent<InitEvent>(InitEvent) {
    companion object : InitEvent(), EventType<InitEvent>

    fun copyFrom(other: InitEvent) {
    }
}

class ResumeEvent() : TypedEvent<ResumeEvent>(ResumeEvent) {
    companion object : EventType<ResumeEvent>

    fun copyFrom(other: ResumeEvent) {
    }
}

class PauseEvent() : TypedEvent<PauseEvent>(PauseEvent) {
    companion object : EventType<PauseEvent>

    fun copyFrom(other: PauseEvent) {
    }
}

class StopEvent() : TypedEvent<StopEvent>(StopEvent) {
    companion object : EventType<StopEvent>

    fun copyFrom(other: StopEvent) {
    }
}

class DestroyEvent() : TypedEvent<DestroyEvent>(DestroyEvent) {
    companion object : EventType<DestroyEvent>

    fun copyFrom(other: DestroyEvent) {
    }
}

class DisposeEvent() : TypedEvent<DisposeEvent>(DisposeEvent) {
    companion object : EventType<DisposeEvent>

    fun copyFrom(other: DisposeEvent) {
    }
}

data class DropFileEvent(override var type: Type = Type.START, var files: List<VfsFile>? = null) : Event(), TEvent<DropFileEvent> {
	enum class Type : EventType<DropFileEvent> {
       START, END, DROP;
       companion object {
           val ALL = values()
       }
    }

    fun copyFrom(other: DropFileEvent) {
        this.type = other.type
        this.files = other.files?.toList()
    }
}
