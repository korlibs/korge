package korlibs.render

import korlibs.concurrent.lock.*
import korlibs.concurrent.thread.*
import korlibs.datastructure.*
import korlibs.event.*
import korlibs.graphics.*
import korlibs.graphics.log.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.vector.*
import korlibs.io.*
import korlibs.io.async.*
import korlibs.io.file.*
import korlibs.logger.*
import korlibs.math.geom.*
import korlibs.memory.*
import korlibs.render.GameWindow.Quality.*
import korlibs.render.event.*
import korlibs.time.*
import korlibs.time.measureTime
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.native.concurrent.*
import kotlin.properties.*
import kotlin.time.*

@ThreadLocal
var GLOBAL_CHECK_GL = false

data class GameWindowCreationConfig(
    val multithreaded: Boolean? = null,
    val hdr: Boolean? = null,
    val msaa: Int? = null,
    val checkGl: Boolean = false,
    val logGl: Boolean = false,
    val cacheGl: Boolean = false,
    /** Makes the window to start full screen. If null, the default configuration will be used. */
    val fullscreen: Boolean? = null,
    /** Allows to make the window undecorated */
    val decorated: Boolean = true,
    /** Allows to make window transparent */
    val transparent: Boolean = false,
    /** Allows window to be resizable */
    val resizable: Boolean = true,
    /** */
    val title: String = "",
    //val allowMinimize: Boolean = true,
    //val allowMaximize: Boolean = true,
) {
    companion object {
        val DEFAULT = GameWindowCreationConfig()
    }
}

expect fun CreateDefaultGameWindow(config: GameWindowCreationConfig): GameWindow
fun CreateDefaultGameWindow() = CreateDefaultGameWindow(GameWindowCreationConfig())

interface GameWindowConfig {
    val quality: GameWindow.Quality

    class Impl(
        override val quality: GameWindow.Quality = GameWindow.Quality.AUTOMATIC,
    ) : GameWindowConfig
}

typealias GameWindowQuality = GameWindow.Quality
typealias CustomCursor = GameWindow.CustomCursor
typealias Cursor = GameWindow.Cursor
typealias ICursor = GameWindow.ICursor

/**
 * A [GameWindow] represents a window, canvas or headless virtual frame where a game is displayed and can receive user events, it provides:
 *
 * Updating and Rendering:
 * - An [EventLoop] and a [coroutineDispatcher] where run code in a single thread like on JavaScript
 * - Provides an [onUpdateEvent] that will be executed in the [EventLoop] at a fixed rate determined by [fps] used for updating the game, and having a lock to prevent rendering while running
 * - Provides an [onRenderEvent] that will be executed in the Rendering thread at vsync or whenever a rendering is required. It has a lock to prevent executing at the same time as the onUpdateEvent.
 * - If needing to update the game state outside the provided EventLoop, you can use the [updateRenderLock] function
 *
 * Events:
 * - It implements the EventListener interface.
 * - Dispatches Window and User Input events:
 *   - Update and rendering events: [UpdateEvent], [RenderEvent]
 *   - Window/App lifecycle event: [PauseEvent], [ResumeEvent], [StopEvent], [InitEvent], [DestroyEvent], [DisposeEvent], [FullScreenEvent], [ReshapeEvent]
 *   - Input events: [KeyEvent], [MouseEvent], [GestureEvent], [DropFileEvent]
 *
 * Window properties:
 * - [title], [icon], [cursor], [width], [height], [preferredFps], [fullscreen], [visible], [bgcolor], [quality], [alwaysOnTop]
 *
 * Dialogs [DialogInterface]:
 * - [browse], [alert], [confirm], [prompt], [openFileDialog]
 * - [showContextMenu]
 *
 * Virtual Keyboard:
 * - [showSoftKeyboard], [hideSoftKeyboard]
 *
 * Device Dimensions provider:
 * - [devicePixelRatio], [pixelsPerInch], [pixelsPerCm]
 */
open class GameWindow :
    BaseEventListener(),
    DialogInterfaceProvider,
    DeviceDimensionsProvider,
    CoroutineContext.Element,
    AGWindow,
    GameWindowConfig,
    Extra by Extra.Mixin() {
    open val androidContextAny: Any? get() = null

    sealed interface ICursor

    override val dialogInterface: DialogInterface get() = DialogInterface.Unsupported

    data class CustomCursor(val shape: Shape, val name: String = "custom") : ICursor, Extra by Extra.Mixin() {
        val bounds: Rectangle = this.shape.bounds
        fun createBitmap(size: Size? = null, native: Boolean = true) = shape.renderWithHotspot(fit = size, native = native)
    }

    enum class Cursor : ICursor {
        DEFAULT, CROSSHAIR, TEXT, HAND, MOVE, WAIT,
        RESIZE_EAST, RESIZE_WEST, RESIZE_SOUTH, RESIZE_NORTH,
        RESIZE_NORTH_EAST, RESIZE_NORTH_WEST, RESIZE_SOUTH_EAST, RESIZE_SOUTH_WEST;

        companion object {
            val ANGLE_TO_CURSOR = mapOf(
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

    data class MenuItem(val text: String?, val enabled: Boolean = true, val children: List<MenuItem>? = null, val action: () -> Unit = {})

    open fun setMainMenu(items: List<MenuItem>) {
    }

    open fun showContextMenu(items: List<MenuItem>) {
    }

    class MenuItemBuilder(private var text: String? = null, private var enabled: Boolean = true, private var action: () -> Unit = {}) {
        @PublishedApi internal val children = arrayListOf<MenuItem>()

        inline fun separator() {
            item(null)
        }

        inline fun item(text: String?, enabled: Boolean = true, noinline action: () -> Unit = {}, block: MenuItemBuilder.() -> Unit = {}): MenuItem {
            val mib = MenuItemBuilder(text, enabled, action)
            block(mib)
            val item = mib.toItem()
            children.add(item)
            return item
        }

        fun toItem() = MenuItem(text, enabled, children.ifEmpty { null }, action)
    }

    fun showContextMenu(block: MenuItemBuilder.() -> Unit) {
        showContextMenu(MenuItemBuilder().also(block).toItem().children ?: listOf())
    }

    open val isSoftKeyboardVisible: Boolean get() = false

    open fun setInputRectangle(windowRect: Rectangle) {
    }

    open fun showSoftKeyboard(force: Boolean = true, config: ISoftKeyboardConfig? = null) {
    }

    open fun hideSoftKeyboard() {
    }

    open var cursor: ICursor = Cursor.DEFAULT

    /**
     * Flag to keep the screen on, even when there is no user input and the user is idle
     */
    open var keepScreenOn: Boolean
        set(value) = Unit
        get() = false

    open var alwaysOnTop: Boolean = false

    override val key: CoroutineContext.Key<*> get() = CoroutineKey

    companion object CoroutineKey : CoroutineContext.Key<GameWindow> {
        const val DEFAULT_PREFERRED_FPS = 60
        val logger = Logger("GameWindow")

        val MenuItemSeparatror = MenuItem(null)
    }

    //override val ag: AG = LogAG()
    override val ag: AG = AGDummy()

    open val myCoroutineDispatcher = SyncEventLoopCoroutineDispatcher()
    open val coroutineDispatcher: GameWindowCoroutineDispatcher = GameWindowCoroutineDispatcher()

    fun getCoroutineDispatcherWithCurrentContext(coroutineContext: CoroutineContext): CoroutineContext = coroutineContext + coroutineDispatcher
    suspend fun getCoroutineDispatcherWithCurrentContext(): CoroutineContext = getCoroutineDispatcherWithCurrentContext(coroutineContext)

    fun queue(callback: () -> Unit) = coroutineDispatcher.queue(callback)
    fun queue(callback: Runnable) = coroutineDispatcher.queue(callback)
    fun <T> queueBlocking(callback: () -> T) = coroutineDispatcher.queueBlocking(callback)

    protected val pauseEvent = PauseEvent()
    protected val resumeEvent = ResumeEvent()
    protected val stopEvent = StopEvent()
    protected val destroyEvent = DestroyEvent()
    protected val updateEvent = UpdateEvent()
    protected val renderEvent = RenderEvent()
    protected val initEvent = InitEvent()
    protected val disposeEvent = DisposeEvent()
    protected val fullScreenEvent = FullScreenEvent()
    private val reshapeEvent = ReshapeEvent()
    protected val keyEvent = KeyEvent()
    protected val mouseEvent = MouseEvent()
    protected val gestureEvent = GestureEvent()
    protected val dropFileEvent = DropFileEvent()

    /** Happens on the updater thread */
    fun onUpdateEvent(block: (UpdateEvent) -> Unit): AutoCloseable {
        return onEvent(UpdateEvent, block)
    }

    /** Happens on the rendering thread */
    fun onRenderEvent(block: (RenderEvent) -> Unit): AutoCloseable {
        return onEvent(RenderEvent, block)
    }

    val fastCounterTimePerFrame: FastDuration get() = (1_000_000.0 / fps).fastMicroseconds
    val fastTimePerFrame: FastDuration get() = fastCounterTimePerFrame

    val counterTimePerFrame: Duration get() = (1_000_000.0 / fps).microseconds
    val timePerFrame: Duration get() = counterTimePerFrame

    open fun computeDisplayRefreshRate(): Int {
        return 60
    }

    open fun registerTime(name: String, time: Duration) {
        //println("registerTime: $name=$time")
    }

    inline fun <T> registerTime(name: String, block: () -> T): T {
        val start = PerformanceCounter.microseconds
        try {
            return block()
        } finally {
            val end = PerformanceCounter.microseconds
            registerTime(name, (end - start).microseconds)
        }
    }

    private val fpsCached by IntTimedCache(1000.milliseconds) { computeDisplayRefreshRate() }

    open var preferredFps: Int = DEFAULT_PREFERRED_FPS

    open var fps: Int
        get() = fpsCached
        @Deprecated("Deprecated setting fps")
        set(value) = Unit

    open var title: String get() = ""; set(value) = Unit
    open val width: Int = 0
    open val height: Int = 0
    open var vsync: Boolean = true

    // Might be different than width and height for example on high dpi screens
    open val bufferWidth: Int get() = width
    open val bufferHeight: Int get() = height

    open var icon: Bitmap? = null
    open var fullscreen: Boolean = false
    open var visible: Boolean = false
    open var bgcolor: RGBA = Colors.BLACK
    override var quality: Quality get() = Quality.AUTOMATIC; set(value) = Unit

    fun hide() {
        visible = false
    }

    fun show() {
        visible = true
    }

    val onDebugEnabled = Signal<Unit>()
    val onDebugChanged = Signal<Boolean>()
    open val debugComponent: Any? = null
    open var debug: Boolean = false
        set(value) {
            field = value
            onDebugChanged(value)
            if (value) onDebugEnabled()
        }

    /**
     * Describes if the rendering should focus on performance or quality.
     * [PERFORMANCE] will use lower resolutions, while [QUALITY] will use the devicePixelRatio
     * to render high quality images.
     */
    enum class Quality(override val level: Float) : korlibs.image.Quality {
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
    }

    open fun setSize(width: Int, height: Int): Unit = Unit

    // Alias for close
    fun exit(exitCode: Int = 0): Unit = close(exitCode)

    var exitProcessOnClose: Boolean = true
    var exitCode = 0; private set
    var running = true; protected set
    private var closing = false

    open fun close(exitCode: Int = 0) {
        if (closing) return
        closing = true
        running = false
        this.exitCode = exitCode
        logger.info { "GameWindow.close" }
        coroutineDispatcher.close()
        coroutineDispatcher.cancelChildren()
    }

    suspend fun waitClose() {
        while (running) {
            delay(100.milliseconds)
        }
    }

    override fun repaint() {
    }

    open suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        launchImmediately(getCoroutineDispatcherWithCurrentContext() + CoroutineName("GameWindow.loop")) {
            entry()
        }
        //withContext(getCoroutineDispatcherWithCurrentContext()) {
            //delay(1L)
            while (running) {
                var elapsed: FastDuration
                val realElapsed = measureTime {
                    elapsed = frame()
                }
                val available = fastCounterTimePerFrame - elapsed
                //val available = fastCounterTimePerFrame - realElapsed
                if (available > FastDuration.ZERO) {
                    //println("delay=$available, elapsed=$elapsed, realElapsed=$realElapsed, fastCounterTimePerFrame=$fastCounterTimePerFrame")
                    loopDelay(available)
                    //NativeThread.sleepExact(available)
                    //NativeThread.sleepExact(available)
                }
            }
        //}
    }

    open suspend fun loopDelay(time: FastDuration) {
        delay(time)
    }

    // Referenced from korge-plugins repo
    var renderTime = 0.milliseconds
    var updateTime = 0.milliseconds

    var onContinuousRenderModeUpdated: ((Boolean) -> Unit)? = null
    open var continuousRenderMode: Boolean by Delegates.observable(true) { prop, old, new ->
        onContinuousRenderModeUpdated?.invoke(new)
    }

    fun frame(doUpdate: Boolean = true, doRender: Boolean = true, frameStartTime: FastDuration = PerformanceCounter.fastReference): FastDuration {
        val startTime = PerformanceCounter.reference
        if (doRender) {
            renderTime = measureTime {
                frameRender(doUpdate = doUpdate, doRender = true)
            }
        }
        //println("renderTime=$renderTime")
        if (doUpdate) {
            updateTime = measureTime {
                if (!doRender) {
                    frameRender(doUpdate = true, doRender = false)
                }
                frameUpdate(frameStartTime)
            }
            //println("updateTime=$updateTime")
        }
        val endTime = PerformanceCounter.fastReference
        return endTime - startTime
    }

    fun frameRender(doUpdate: Boolean = true, doRender: Boolean = true) {
        if (doRender) {
            if (contextLost) {
                contextLost = false
                ag.contextLost()
            }
            if (surfaceChanged) {
                surfaceChanged = false
                ag.mainFrameBuffer.setSize(surfaceX, surfaceY, surfaceWidth, surfaceHeight)
                dispatchReshapeEvent(surfaceX, surfaceY, surfaceWidth, surfaceHeight)
            }
        }
        if (doInitialize) {
            doInitialize = false
            //ag.mainRenderBuffer.setSize(0, 0, width, height)
            println("---------------- Trigger AG.initialized ag.mainFrameBuffer.setSize ($width, $height) --------------")
            dispatch(initEvent.reset())
        }
        try {
            dispatchRenderEvent(update = doUpdate, render = doRender)
        } catch (e: Throwable) {
            println("ERROR GameWindow.frameRender:")
            println(e)
            e.printStackTrace()
        }
    }

    private var surfaceChanged = false
    private var surfaceX = -1
    private var surfaceY = -1
    private var surfaceWidth = -1
    private var surfaceHeight = -1

    private var doInitialize = false
    private var initialized = false
    private var contextLost = false

    var updatedSinceFrame: Int = 0

    val mustTriggerRender: Boolean get() = continuousRenderMode || updatedSinceFrame > 0

    fun startFrame() {
        updatedSinceFrame = 0
    }

    fun invalidatedView() {
        updatedSinceFrame++
    }

    fun handleContextLost() {
        println("---------------- handleContextLost --------------")
        contextLost = true
    }

    fun handleInitEventIfRequired() {
        if (initialized) return
        initialized = true
        doInitialize = true
    }

    fun handleReshapeEventIfRequired(x: Int, y: Int, width: Int, height: Int) {
        if (surfaceX == x && surfaceY == y && surfaceWidth == width && surfaceHeight == height) return
        println("handleReshapeEventIfRequired: $x, $y, $width, $height")
        surfaceChanged = true
        surfaceX = x
        surfaceY = y
        surfaceWidth = width
        surfaceHeight = height
    }

    var gamePadTime: FastDuration = FastDuration.ZERO
    fun frameUpdate(startTime: FastDuration) {
        gamePadTime = fastMeasureTime {
            updateGamepads()
        }
        try {
            val now = PerformanceCounter.reference
            val consumed = now - startTime
            //val remaining = (counterTimePerFrame - consumed) - 2.milliseconds // Do not push too much so give two extra milliseconds just in case
            //val timeForTasks = coroutineDispatcher.maxAllocatedTimeForTasksPerFrame ?: (remaining * 10) // We would be skipping up to 10 frames by default
            val timeForTasks =
                coroutineDispatcher.maxAllocatedTimeForTasksPerFrame ?: (counterTimePerFrame * 10) // We would be skipping up to 10 frames by default
            val finalTimeForTasks = max(timeForTasks, 4.milliseconds) // Avoid having 0 milliseconds or even negative
            //println("         - frameUpdate: finalTimeForTasks=$finalTimeForTasks, startTime=$startTime, now=$now")
            coroutineDispatcher.executePending(finalTimeForTasks)
        } catch (e: Throwable) {
            println("ERROR GameWindow.frameRender:")
            println(e)
        }
    }

    open fun updateGamepads() {
    }

    fun executePending(availableTime: Duration) {
        coroutineDispatcher.executePending(availableTime)
    }
    fun executePending(availableTime: FastDuration) {
        coroutineDispatcher.executePending(availableTime)
    }

    fun dispatchInitEvent() = dispatch(initEvent.reset())
    fun dispatchPauseEvent() = dispatch(pauseEvent.reset())
    fun dispatchResumeEvent() = dispatch(resumeEvent.reset())
    fun dispatchStopEvent() = dispatch(stopEvent.reset())
    fun dispatchDestroyEvent() = dispatch(destroyEvent.reset())
    fun dispatchDisposeEvent() = dispatch(disposeEvent.reset())
    @PublishedApi internal val _updateRenderLock = Lock()
    inline fun updateRenderLock(block: () -> Unit) {
        _updateRenderLock(block)
    }

    fun dispatchUpdateEvent() {
        updateRenderLock { dispatch(updateEvent) }
    }

    fun dispatchRenderEvent(update: Boolean = true, render: Boolean = true) {
        updateRenderLock {
            dispatch(renderEvent.reset {
                this.update = update
                this.render = render
            })
        }
    }

    fun dispatchNewRenderEvent() {
        dispatchRenderEvent(update = false, render = true)
    }

    fun dispatchDropfileEvent(type: DropFileEvent.Type, files: List<VfsFile>?) = dispatch(dropFileEvent.reset {
        this.type = type
        this.files = files
    })

    fun dispatchFullscreenEvent(fullscreen: Boolean) = dispatch(fullScreenEvent.reset { this.fullscreen = fullscreen })

    fun dispatchReshapeEvent(x: Int, y: Int, width: Int, height: Int) {
        dispatchReshapeEventEx(x, y, width, height, width, height)
    }

    fun dispatchReshapeEventEx(x: Int, y: Int, width: Int, height: Int, fullWidth: Int = width, fullHeight: Int = height) {
        ag.mainFrameBuffer.setSize(x, y, width, height, fullWidth, fullHeight)
        dispatch(reshapeEvent.reset {
            this.x = x
            this.y = y
            this.width = width
            this.height = height
        })
    }

    private val keysPresing = BooleanArray(Key.MAX)
    private fun pressing(key: Key) = keysPresing[key.ordinal]
    private val shift get() = pressing(Key.LEFT_SHIFT) || pressing(Key.RIGHT_SHIFT)
    private val ctrl get() = pressing(Key.LEFT_CONTROL) || pressing(Key.RIGHT_CONTROL)
    private val alt get() = pressing(Key.LEFT_ALT) || pressing(Key.RIGHT_ALT)
    private val meta get() = pressing(Key.META) || pressing(Key.LEFT_SUPER) || pressing(Key.RIGHT_SUPER)
    private var mouseButtons = 0
    private val scrollDeltaX = 0f
    private val scrollDeltaY = 0f
    private val scrollDeltaZ = 0f
    private val scaleCoords = false

    fun dispatchKeyEvent(type: KeyEvent.Type, id: Int, character: Char, key: Key, keyCode: Int, str: String? = null): Boolean {
        return dispatchKeyEventEx(type, id, character, key, keyCode, str = str)
    }

    fun dispatchKeyEventDownUp(id: Int, character: Char, key: Key, keyCode: Int, str: String? = null): Boolean {
        val cancel1 = dispatchKeyEvent(KeyEvent.Type.DOWN, id, character, key, keyCode, str)
        val cancel2 = dispatchKeyEvent(KeyEvent.Type.UP, id, character, key, keyCode, str)
        return cancel1 || cancel2
    }

    val gamepadEmitter: GamepadInfoEmitter = GamepadInfoEmitter(this)

    //private val gamePadUpdateEvent = GamePadUpdateEvent()
    fun dispatchGamepadUpdateStart() {
        gamepadEmitter.dispatchGamepadUpdateStart()
    }

    fun dispatchGamepadUpdateAdd(info: GamepadInfo) {
        gamepadEmitter.dispatchGamepadUpdateAdd(info)
    }

    /**
     * Triggers an update envent and potential CONNECTED/DISCONNECTED events.
     *
     * Returns a list of disconnected gamepads.
     */
    fun dispatchGamepadUpdateEnd(out: IntArrayList = IntArrayList()): IntArrayList =
        gamepadEmitter.dispatchGamepadUpdateEnd(out)

    fun dispatchKeyEventEx(
        type: KeyEvent.Type, id: Int, character: Char, key: Key, keyCode: Int,
        shift: Boolean = this.shift, ctrl: Boolean = this.ctrl, alt: Boolean = this.alt, meta: Boolean = this.meta,
        str: String? = null
    ): Boolean {
        if (type != KeyEvent.Type.TYPE) {
            keysPresing[key.ordinal] = (type == KeyEvent.Type.DOWN)
        }
        dispatch(keyEvent.reset {
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
        })
        return keyEvent.defaultPrevented
    }

    fun dispatchSimpleMouseEvent(
        type: MouseEvent.Type, id: Int, x: Int, y: Int, button: MouseButton, simulateClickOnUp: Boolean = false
    ) {
        dispatchMouseEvent(type, id, x, y, button, simulateClickOnUp = simulateClickOnUp)
    }

    fun dispatchMouseEvent(
        type: MouseEvent.Type, id: Int, x: Int, y: Int, button: MouseButton, buttons: Int = this.mouseButtons,
        scrollDeltaX: Float = this.scrollDeltaX, scrollDeltaY: Float = this.scrollDeltaY, scrollDeltaZ: Float = this.scrollDeltaZ,
        isShiftDown: Boolean = this.shift, isCtrlDown: Boolean = this.ctrl, isAltDown: Boolean = this.alt, isMetaDown: Boolean = this.meta,
        scaleCoords: Boolean = this.scaleCoords, simulateClickOnUp: Boolean = false,
        scrollDeltaMode: MouseEvent.ScrollDeltaMode = MouseEvent.ScrollDeltaMode.LINE
    ) {
        if (type != MouseEvent.Type.DOWN && type != MouseEvent.Type.UP) {
            this.mouseButtons = this.mouseButtons.setBits(if (button != null) 1 shl button.ordinal else 0, type == MouseEvent.Type.DOWN)
        }
        dispatch(mouseEvent.reset {
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
        })
        //if (simulateClickOnUp && type == MouseEvent.Type.UP) {
        //    dispatchMouseEvent(MouseEvent.Type.CLICK, id, x, y, button, buttons, scrollDeltaX, scrollDeltaY, scrollDeltaZ, isShiftDown, isCtrlDown, isAltDown, isMetaDown, scaleCoords, simulateClickOnUp = false)
        //}
    }

    // @TODO: Is this used?
    fun entry(callback: suspend () -> Unit) {
        launch(coroutineDispatcher) {
            try {
                callback()
            } catch (e: Throwable) {
                println("ERROR GameWindow.entry:")
                e.printStackTrace()
                running = false
            }
        }
    }

    enum class HapticFeedbackKind { GENERIC, ALIGNMENT, LEVEL_CHANGE }

    open val hapticFeedbackGenerateSupport: Boolean get() = false
    open fun hapticFeedbackGenerate(kind: HapticFeedbackKind) {
    }

    open suspend fun clipboardWrite(data: ClipboardData) {
    }

    open suspend fun clipboardRead(): ClipboardData? {
        return null
    }

    //open fun lockMousePointer() = println("WARNING: lockMousePointer not implemented")
    //open fun unlockMousePointer() = Unit
}

interface ClipboardData {
}

data class TextClipboardData(val text: String, val contentType: String? = null) : ClipboardData

open class EventLoopGameWindow : GameWindow() {
    var fixedTime = PerformanceCounter.fastReference
    override val coroutineDispatcher = GameWindowCoroutineDispatcher(nowProvider = { fixedTime })

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        try {
            // Required here so setSize is called
            launchImmediately(getCoroutineDispatcherWithCurrentContext()) {
                try {
                    entry()
                } catch (e: Throwable) {
                    println("Error initializing application")
                    println(e)
                    running = false
                }
            }

            doInitialize()
            dispatchInitEvent()

            while (running) {
                render(doUpdate = true) {
                    doHandleEvents()
                    mustPerformRender()
                }
                // Here we can trigger a GC if we have enough time, and we can try to disable GC all the other times.
                if (!vsync) {
                    sleepNextFrame()
                }
            }
        } finally {
            try {
                dispatchStopEvent()
                dispatchDestroyEvent()
            } finally {
                doDestroy()
            }
        }
    }

    fun mustPerformRender(): Boolean = if (vsync) true else elapsedSinceLastRenderTime() >= counterTimePerFrame

    var lastRenderTime = PerformanceCounter.reference
    fun elapsedSinceLastRenderTime() = PerformanceCounter.reference - lastRenderTime

    inline fun render(doUpdate: Boolean, doRender: () -> Boolean = { true }) {
        val frameStartTime: FastDuration = PerformanceCounter.fastReference
        val mustRender = doRender()
        if (mustRender) renderInternal(doUpdate = doUpdate, frameStartTime = frameStartTime)
    }

    @PublishedApi
    internal fun renderInternal(doUpdate: Boolean, frameStartTime: FastDuration = PerformanceCounter.fastReference) {
        fixedTime = PerformanceCounter.fastReference
        doInitRender()

        var doRender = !doUpdate
        if (doUpdate) {
            frame(doUpdate = true, doRender = false, frameStartTime = frameStartTime)
            if (mustTriggerRender) {
                doRender = true
            }
        }
        if (doRender) {
            frame(doUpdate = false, doRender = true, frameStartTime = frameStartTime)
        }
        lastRenderTime = PerformanceCounter.reference
        if (doRender) {
            doSwapBuffers()
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun sleepNextFrame() {
        val now = PerformanceCounter.reference
        val frameTime = (1.toDouble() / fps.toDouble()).seconds
        val delay = frameTime - (now % frameTime)
        if (delay > 0.0.milliseconds) {
            //println(delayNanos / 1_000_000)
            blockingSleep(delay)
        }
    }

    protected fun sleep(time: Duration) {
        // Reimplement: Spinlock!
        val start = PerformanceCounter.reference
        while ((PerformanceCounter.reference - start) < time) {
            doSmallSleep()
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    protected fun doSmallSleep() {
        if (!vsync) {
            blockingSleep(0.1.milliseconds)
        }
    }
    protected open fun doHandleEvents() = Unit
    protected open fun doInitRender() = Unit
    protected open fun doSwapBuffers() = Unit
    protected open fun doInitialize() = Unit
    protected open fun doDestroy() = Unit
}

fun GameWindow.mainLoop(entry: suspend GameWindow.() -> Unit) = Korio { loop(entry) }

fun GameWindow.toggleFullScreen()  { fullscreen = !fullscreen }

fun GameWindow.configure(
    size: Size,
    title: String? = "GameWindow",
    icon: Bitmap? = null,
    fullscreen: Boolean? = null,
    bgcolor: RGBA = Colors.BLACK,
) {
    this.setSize(size.width.toInt(), size.height.toInt())
    if (title != null) this.title = title
    this.icon = icon
    if (fullscreen != null) this.fullscreen = fullscreen
    this.bgcolor = bgcolor
    this.visible = true
}

fun GameWindow.onDragAndDropFileEvent(block: suspend (DropFileEvent) -> Unit) {
    onEvents(*DropFileEvent.Type.ALL) { event ->
        launchImmediately(coroutineDispatcher) {
            block(event)
        }
    }
}

