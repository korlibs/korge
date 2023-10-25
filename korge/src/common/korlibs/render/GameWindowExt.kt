package korlibs.render

import korlibs.datastructure.*
import korlibs.event.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.vector.*
import korlibs.io.async.*
import korlibs.io.file.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.memory.*
import korlibs.render.GameWindowQuality.*

class GameWindowEventInstances {
    var requestLock: () -> Unit = { }
    val pauseEvent = PauseEvent()
    val resumeEvent = ResumeEvent()
    val stopEvent = StopEvent()
    val destroyEvent = DestroyEvent()
    val disposeEvent = DisposeEvent()
    val updateEvent = UpdateEvent()
    val renderEvent = RenderEvent()
    val updateEvents = ConcurrentPool { UpdateEvent() }
    val fullScreenEvents = ConcurrentPool { FullScreenEvent() }
    val reshapeEvents = ConcurrentPool { ReshapeEvent() }
    val keyEvents = ConcurrentPool { KeyEvent() }
    val mouseEvent = MouseEvent()
    val mouseEvents = ConcurrentPool(reset = { it.requestLock = requestLock }) { MouseEvent().also { it.requestLock = requestLock } }
    val gestureEvents = ConcurrentPool { GestureEvent() }
    val dropFileEvents = ConcurrentPool { DropFileEvent() }
}

class GameWindowInputState {
    val keysPresing = BooleanArray(Key.MAX)
    fun pressing(key: Key) = keysPresing[key.ordinal]
    val shift get() = pressing(Key.SHIFT)
    val ctrl get() = pressing(Key.CONTROL)
    val alt get() = pressing(Key.ALT)
    val meta get() = pressing(Key.META) || pressing(Key.SUPER)
    var mouseButtons = 0
    val scrollDeltaX = 0f
    val scrollDeltaY = 0f
    val scrollDeltaZ = 0f
    val scaleCoords = false

    var surfaceChanged = false
    var surfaceX = -1
    var surfaceY = -1
    var surfaceWidth = -1
    var surfaceHeight = -1
    var doInitialize = false
    var initialized = false
}

fun GameWindow.dispatchKeyEvent(type: KeyEvent.Type, id: Int, character: Char, key: Key, keyCode: Int, str: String? = null): Boolean {
    return dispatchKeyEventExQueued(type, id, character, key, keyCode, str = str)
}

fun GameWindow.dispatchKeyEventDownUp(id: Int, character: Char, key: Key, keyCode: Int, str: String? = null): Boolean {
    val cancel1 = dispatchKeyEvent(KeyEvent.Type.DOWN, id, character, key, keyCode, str)
    val cancel2 = dispatchKeyEvent(KeyEvent.Type.UP, id, character, key, keyCode, str)
    return cancel1 || cancel2
}

//private val gamePadUpdateEvent = GamePadUpdateEvent()
fun GameWindow.dispatchGamepadUpdateStart() {
    gamepadEmitter.dispatchGamepadUpdateStart()
}

fun GameWindow.dispatchGamepadUpdateAdd(info: GamepadInfo) {
    gamepadEmitter.dispatchGamepadUpdateAdd(info)
}

/**
 * Triggers an update envent and potential CONNECTED/DISCONNECTED events.
 *
 * Returns a list of disconnected gamepads.
 */
fun GameWindow.dispatchGamepadUpdateEnd(out: IntArrayList = IntArrayList()): IntArrayList =
    gamepadEmitter.dispatchGamepadUpdateEnd(out)

fun GameWindow.dispatchKeyEventExQueued(
    type: KeyEvent.Type, id: Int, character: Char, key: Key, keyCode: Int,
    shift: Boolean = gameWindowInputState.shift, ctrl: Boolean = gameWindowInputState.ctrl, alt: Boolean = gameWindowInputState.alt, meta: Boolean = gameWindowInputState.meta,
    str: String? = null
): Boolean {
    if (type != KeyEvent.Type.TYPE) {
        gameWindowInputState.keysPresing[key.ordinal] = (type == KeyEvent.Type.DOWN)
    }
    dispatchQueued(events.keyEvents) {
        this.id = id
        this.character = character
        this.key = key
        this.keyCode = keyCode
        this.type = type
        this.shift = shift
        this.ctrl = ctrl
        this.alt = alt
        this.meta = meta
        if (str != null && str.length == 1) {
            this.str = null
            this.character = str[0]
            this.keyCode = this.character.code
        } else {
            this.str = str
        }
    }
    //return events.keyEvents.defaultPrevented
    return false
}

fun GameWindow.dispatchSimpleMouseEventQueued(
    type: MouseEvent.Type, id: Int, x: Int, y: Int, button: MouseButton, simulateClickOnUp: Boolean = false
) {
    dispatchMouseEventQueued(type, id, x, y, button, simulateClickOnUp = simulateClickOnUp)
}

fun GameWindow.dispatchMouseEventQueued(
    type: MouseEvent.Type, id: Int, x: Int, y: Int, button: MouseButton, buttons: Int = gameWindowInputState.mouseButtons,
    scrollDeltaX: Float = gameWindowInputState.scrollDeltaX, scrollDeltaY: Float = gameWindowInputState.scrollDeltaY, scrollDeltaZ: Float = gameWindowInputState.scrollDeltaZ,
    isShiftDown: Boolean = gameWindowInputState.shift, isCtrlDown: Boolean = gameWindowInputState.ctrl, isAltDown: Boolean = gameWindowInputState.alt, isMetaDown: Boolean = gameWindowInputState.meta,
    scaleCoords: Boolean = gameWindowInputState.scaleCoords, simulateClickOnUp: Boolean = false,
    scrollDeltaMode: MouseEvent.ScrollDeltaMode = MouseEvent.ScrollDeltaMode.LINE
) {
    if (type != MouseEvent.Type.DOWN && type != MouseEvent.Type.UP) {
        gameWindowInputState.mouseButtons = gameWindowInputState.mouseButtons.setBits(1 shl button.ordinal, type == MouseEvent.Type.DOWN)
    }
    dispatchQueued(events.mouseEvents) {
        this.type = type
        this.id = id
        this.x = x
        this.y = y
        this.button = button
        this.buttons = buttons
        this.scrollDeltaX = scrollDeltaX
        this.scrollDeltaY = scrollDeltaY
        this.scrollDeltaZ = scrollDeltaZ
        this.scrollDeltaMode = scrollDeltaMode
        this.isShiftDown = isShiftDown
        this.isCtrlDown = isCtrlDown
        this.isAltDown = isAltDown
        this.isMetaDown = isMetaDown
        this.scaleCoords = scaleCoords
    }
    //if (simulateClickOnUp && type == MouseEvent.Type.UP) {
    //    dispatchMouseEvent(MouseEvent.Type.CLICK, id, x, y, button, buttons, scrollDeltaX, scrollDeltaY, scrollDeltaZ, isShiftDown, isCtrlDown, isAltDown, isMetaDown, scaleCoords, simulateClickOnUp = false)
    //}
}

fun <T : BEvent> GameWindow.dispatchQueued(event: T) {
    queue { dispatch(event) }
}

fun <T : BEvent> GameWindow.dispatchQueued(pool: Pool<T>, update: T.() -> Unit) {
    pool.alloc {
        update(it)
        dispatch(it)
    }
    //val item = pool.alloc()
    //update(item)
    //queue {
    //    try {
    //        dispatch(item)
    //    } finally {
    //        pool.free(item)
    //    }
    //}
}

fun GameWindow.dispatchPauseEventQueued() = dispatchQueued(events.pauseEvent)
fun GameWindow.dispatchResumeEventQueued() = dispatchQueued(events.resumeEvent)
fun GameWindow.dispatchStopEventQueued() = dispatchQueued(events.stopEvent)
fun GameWindow.dispatchDestroyEventQueued() = dispatchQueued(events.destroyEvent)
fun GameWindow.dispatchDisposeEventQueued() = dispatchQueued(events.disposeEvent)

fun GameWindow.dispatchUpdateEvent() {
    updateRenderLock { dispatch(events.updateEvent) }
}

fun GameWindow.dispatchRenderEvent() {
    updateRenderLock { dispatch(events.renderEvent) }
    //ag.finish()
}
fun GameWindow.dispatchDropfileEventQueued(type: DropFileEvent.Type, files: List<VfsFile>?) = dispatchQueued(events.dropFileEvents) {
    this.type = type
    this.files = files
}
fun GameWindow.dispatchFullscreenEventQueued(fullscreen: Boolean) = dispatchQueued(events.fullScreenEvents) { this.fullscreen = fullscreen }

fun GameWindow.dispatchReshapeEventQueued(x: Int, y: Int, width: Int, height: Int) {
    dispatchReshapeEventExQueued(x, y, width, height, width, height)
}

fun GameWindow.dispatchReshapeEventExQueued(x: Int, y: Int, width: Int, height: Int, fullWidth: Int, fullHeight: Int) {
    ag.mainFrameBuffer.setSize(x, y, width, height, fullWidth, fullHeight)
    dispatchQueued(events.reshapeEvents) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
    }
}

fun GameWindow.handleInitEventIfRequired() {
    if (gameWindowInputState.initialized) return
    gameWindowInputState.initialized = true
    gameWindowInputState.doInitialize = true
}

fun GameWindow.handleReshapeEventIfRequired(x: Int, y: Int, width: Int, height: Int) {
    if (gameWindowInputState.surfaceX == x && gameWindowInputState.surfaceY == y && gameWindowInputState.surfaceWidth == width && gameWindowInputState.surfaceHeight == height) return
    println("handleReshapeEventIfRequired: $x, $y, $width, $height")
    gameWindowInputState.surfaceChanged = true
    gameWindowInputState.surfaceX = x
    gameWindowInputState.surfaceY = y
    gameWindowInputState.surfaceWidth = width
    gameWindowInputState.surfaceHeight = height
}

data class GameWindowCreationConfig(
    val multithreaded: Boolean? = null,
    val hdr: Boolean? = null,
    val msaa: Int? = null,
    val checkGl: Boolean = false,
    val logGl: Boolean = false,
    val cacheGl: Boolean = false,
    val fullscreen: Boolean? = null,
    val title: String = "App",
) {
    companion object {
        val DEFAULT = GameWindowCreationConfig()
    }
}

interface GameWindowConfig {
    val quality: GameWindowQuality

    class Impl(
        override val quality: GameWindowQuality = GameWindowQuality.AUTOMATIC,
    ) : GameWindowConfig
}


/**
 * Describes if the rendering should focus on performance or quality.
 * [PERFORMANCE] will use lower resolutions, while [QUALITY] will use the devicePixelRatio
 * to render high quality images.
 */
enum class GameWindowQuality(override val level: Float) : korlibs.image.Quality {
    /** Will render to lower resolutions, ignoring devicePixelRatio on retina-like screens */
    PERFORMANCE(0f),
    /** Will render to higher resolutions, using devicePixelRatio on retina-like screens */
    QUALITY(1f),
    /** Will choose [PERFORMANCE] or [QUALITY] based on some heuristics */
    AUTOMATIC(.5f);

    private val UPPER_BOUND_RENDERED_PIXELS = 4_000_000

    fun computeTargetScale(
        width: Int,
        height: Int,
        devicePixelRatio: Float,
        targetPixels: Int = UPPER_BOUND_RENDERED_PIXELS
    ): Float = when (this) {
        PERFORMANCE -> 1f
        QUALITY -> devicePixelRatio
        AUTOMATIC -> {
            listOf(devicePixelRatio, 2f, 1f)
                .firstOrNull { width * height * it <= targetPixels }
                ?: 1f
        }
    }

    interface Alias {
        val PERFORMANCE get() = GameWindowQuality.PERFORMANCE
        val QUALITY get() = GameWindowQuality.QUALITY
        val AUTOMATIC get() = GameWindowQuality.AUTOMATIC
    }
}

interface ClipboardData
data class TextClipboardData(val text: String, val contentType: String? = null) : ClipboardData

fun GameWindow.toggleFullScreen() { fullscreen = !fullscreen }

fun <T : GameWindow> T.configure(
    size: Size,
    title: String? = "GameWindow",
    icon: Bitmap? = null,
    fullscreen: Boolean? = null,
    bgcolor: RGBA = Colors.BLACK,
): T {
    this.setSize(size.width.toInt(), size.height.toInt())
    if (title != null) this.title = title
    this.icon = icon
    if (fullscreen != null) this.fullscreen = fullscreen
    this.backgroundColor = bgcolor
    this.visible = true
    return this
}

@Deprecated("")
fun GameWindow.onDragAndDropFileEvent(block: suspend (DropFileEvent) -> Unit) {
    onEvents(*DropFileEvent.Type.ALL) { event ->
        launchImmediately(coroutineDispatcher) {
            block(event)
        }
    }
}

enum class HapticFeedbackKind { GENERIC, ALIGNMENT, LEVEL_CHANGE }

sealed interface ICursor

data class CustomCursor(val shape: Shape, val name: String = "custom") : ICursor, Extra by Extra.Mixin() {
    val bounds: Rectangle = this.shape.bounds
    fun createBitmap(size: Size? = null, native: Boolean = true) = shape.renderWithHotspot(fit = size, native = native)
}

enum class Cursor : ICursor {
    DEFAULT, CROSSHAIR, TEXT, HAND, MOVE, WAIT,
    RESIZE_EAST, RESIZE_WEST, RESIZE_SOUTH, RESIZE_NORTH,
    RESIZE_NORTH_EAST, RESIZE_NORTH_WEST, RESIZE_SOUTH_EAST, RESIZE_SOUTH_WEST;

    interface Alias {
        val DEFAULT get() = Cursor.DEFAULT
        val CROSSHAIR get() = Cursor.CROSSHAIR
        val TEXT get() = Cursor.TEXT
        val HAND get() = Cursor.HAND
        val MOVE get() = Cursor.MOVE
        val WAIT get() = Cursor.WAIT
        val RESIZE_EAST get() = Cursor.RESIZE_EAST
        val RESIZE_WEST get() = Cursor.RESIZE_WEST
        val RESIZE_SOUTH get() = Cursor.RESIZE_SOUTH
        val RESIZE_NORTH get() = Cursor.RESIZE_NORTH
        val RESIZE_NORTH_EAST get() = Cursor.RESIZE_NORTH_EAST
        val RESIZE_NORTH_WEST get() = Cursor.RESIZE_NORTH_WEST
        val RESIZE_SOUTH_EAST get() = Cursor.RESIZE_SOUTH_EAST
        val RESIZE_SOUTH_WEST get() = Cursor.RESIZE_SOUTH_WEST
        val ANGLE_TO_CURSOR get() = Cursor.ANGLE_TO_CURSOR
        fun fromAngleResize(angle: Angle?): ICursor? = Cursor.fromAngleResize(angle)
        fun fromAnchorResize(anchor: Anchor): ICursor? = Cursor.fromAnchorResize(anchor)
    }

    companion object {
        val ANGLE_TO_CURSOR: Map<Angle, Cursor> = mapOf(
            (45.degrees * 0) to RESIZE_EAST,
            (45.degrees * 1) to RESIZE_SOUTH_EAST,
            (45.degrees * 2) to RESIZE_SOUTH,
            (45.degrees * 3) to RESIZE_SOUTH_WEST,
            (45.degrees * 4) to RESIZE_WEST,
            (45.degrees * 5) to RESIZE_NORTH_WEST,
            (45.degrees * 6) to RESIZE_NORTH,
            (45.degrees * 7) to RESIZE_NORTH_EAST,
        )

        fun fromAngleResize(angle: Angle?): ICursor? {
            var minDistance = 360.degrees
            var cursor: ICursor? = null
            if (angle != null) {
                for ((cangle, ccursor) in ANGLE_TO_CURSOR) {
                    val cdistance = (angle - cangle).absoluteValue
                    if (cdistance <= minDistance) {
                        minDistance = cdistance
                        cursor = ccursor
                    }
                }
            }
            return cursor
        }

        fun fromAnchorResize(anchor: Anchor): ICursor? {
            return when (anchor) {
                Anchor.TOP_LEFT -> RESIZE_NORTH_WEST
                Anchor.TOP -> RESIZE_NORTH
                Anchor.TOP_RIGHT -> RESIZE_NORTH_EAST
                Anchor.LEFT -> RESIZE_WEST
                Anchor.RIGHT -> RESIZE_EAST
                Anchor.BOTTOM_LEFT -> RESIZE_SOUTH_WEST
                Anchor.BOTTOM -> RESIZE_SOUTH
                Anchor.BOTTOM_RIGHT -> RESIZE_SOUTH_EAST
                else -> null
            }
        }
    }
}
