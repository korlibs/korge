package korlibs.render

import korlibs.datastructure.*
import korlibs.datastructure.closeable.*
import korlibs.datastructure.event.*
import korlibs.datastructure.lock.*
import korlibs.event.*
import korlibs.graphics.*
import korlibs.graphics.log.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.io.async.*
import korlibs.io.concurrent.atomic.*
import korlibs.korge.view.*
import korlibs.logger.*
import korlibs.math.geom.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.properties.*

/** Creates the default [GameWindow] for this platform. Typically, a window/frame or a fullscreen screen depending on the OS */
expect fun CreateDefaultGameWindow(config: GameWindowCreationConfig = GameWindowCreationConfig()): GameWindow

class ContinuousRenderMode {
    var onContinuousRenderModeUpdated: ((Boolean) -> Unit)? = null
    var continuousRenderMode: Boolean by Delegates.observable(true) { prop, old, new ->
        onContinuousRenderModeUpdated?.invoke(new)
    }
    var updatedSinceRender = KorAtomicInt(0)

    val mustTriggerRender: Boolean get() = continuousRenderMode || updatedSinceRender.value > 0

    fun updated() {
        updatedSinceRender.incrementAndGet()
    }

    fun restart() {
        updatedSinceRender.value = 0
    }

    fun shouldRender(): Boolean {
        if (mustTriggerRender) {
            //println("continuousRenderMode=$continuousRenderMode, updatedSinceFrame.value=${updatedSinceFrame.value}")
            restart()
            return true
        } else {
            return false
        }
    }
}

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
 * - [title], [icon], [cursor],
 * - [width], [height], [bufferWidth], [bufferHeight]
 * - [fullscreen], [visible], [backgroundColor], [quality], [alwaysOnTop]
 * - [preferredFps], [fps]
 * - [close]
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
    Closeable,
    DialogInterfaceProvider,
    MenuInterface,
    DeviceDimensionsProvider,
    CoroutineContext.Element,
    AGWindow,
    GameWindowConfig,
    Extra by Extra.Mixin()
{
    open val androidContextAny: Any? get() = null
    override val dialogInterface: DialogInterface get() = DialogInterface.Unsupported

    // SOFT KEYBOARD
    open val isSoftKeyboardVisible: Boolean get() = false
    open fun setInputRectangle(windowRect: Rectangle) = Unit
    open fun showSoftKeyboard(force: Boolean = true, config: ISoftKeyboardConfig? = null) = Unit
    open fun hideSoftKeyboard() = Unit

    // Cursor
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
    }

    //override val ag: AG = LogAG()
    override val ag: AG = AGDummy()

    open fun createEventLoop(): BaseEventLoop = SyncEventLoop(precise = false)

    val eventLoop: BaseEventLoop by lazy { createEventLoop() }
    //val renderEventLoop = SyncEventLoop(precise = false, immediateRun = true)
    open val coroutineDispatcher by lazy {
        EventLoopCoroutineDispatcher(eventLoop).also {
            ensureInitialized()
        }
    }

    var coroutineContext: CoroutineContext = EmptyCoroutineContext

    open fun enrichCoroutineContext() {
    }

    fun getCoroutineDispatcherWithCurrentContext(coroutineContext: CoroutineContext): CoroutineContext = coroutineContext + coroutineDispatcher
    fun getCoroutineDispatcherWithCurrentContext(): CoroutineContext = getCoroutineDispatcherWithCurrentContext(coroutineContext)

    private fun ensureInitialized() {
        updateUpdateInterval()
    }

    // Event Loop
    @PublishedApi internal val _updateRenderLock = Lock()
    inline fun updateRenderLock(block: () -> Unit) = _updateRenderLock(block)
    fun queueSuspend(callback: suspend () -> Unit) {
        coroutineDispatcher
        launchAsap(getCoroutineDispatcherWithCurrentContext()) {
            callback()
        }
    }
    fun queue(callback: () -> Unit) {
        coroutineDispatcher
        eventLoop.setImmediate(callback)
    }
    fun queue(callback: Runnable) {
        coroutineDispatcher
        eventLoop.setImmediate { callback.run() }
    }
    @Deprecated("")
    fun <T> queueBlocking(callback: () -> T): T {
        val deferred = CompletableDeferred<T>()
        queue {
            deferred.completeWith(runCatching { callback() })
        }
        return runBlockingNoJs { deferred.await() }
    }
    //fun queueRender(callback: () -> Unit) = renderEventLoop.setImmediate(callback)
    //fun queueRenderSync(callback: () -> Unit) {
    //    if (Platform.isJs) {
    //        callback()
    //    } else {
    //        val done = CompletableDeferred<Unit>()
    //        renderEventLoop.setImmediate {
    //            try { callback() } finally { done.complete(Unit) }
    //        }
    //        runBlockingNoJs { done.await() }
    //    }
    //}

    internal val events = GameWindowEventInstances()
    internal val gamepadEmitter: GamepadInfoEmitter = GamepadInfoEmitter(this)
    internal val gameWindowInputState = GameWindowInputState()

    open val continuousRenderMode = ContinuousRenderMode()

    /** Happens on the updater thread */
    fun onUpdateEvent(block: (UpdateEvent) -> Unit): Closeable = onEvent(UpdateEvent, block)

    /** Happens on the rendering thread */
    fun onRenderEvent(block: (RenderEvent) -> Unit): Closeable = onEvent(RenderEvent, block)

    // region: FPS
    val counterTimePerFrame: TimeSpan get() = (1_000_000.0 / fps).microseconds
    val timePerFrame: TimeSpan get() = counterTimePerFrame
    open fun computeDisplayRefreshRate(): Int = 60
    private val fpsCached by IntTimedCache(1000.milliseconds) { computeDisplayRefreshRate() }

    /** Update FPS */
    open var preferredFps: Int = DEFAULT_PREFERRED_FPS
    open var fps: Int
        get() = fpsCached
        @Deprecated("Deprecated setting fps")
        set(value) = Unit

    /** Rate at which update events happen in the event loop */
    var updateFps: Frequency = DEFAULT_PREFERRED_FPS.hz
        set(value) {
            field = value
            updateUpdateInterval()
        }

    open val autoUpdateInterval = true

    private var currentUpdateFps: Frequency = 0.hz
    private var currentUpdateInterval: Closeable? = null
    private fun updateUpdateInterval(fps: Frequency = this.updateFps) {
        if (autoUpdateInterval && currentUpdateFps != fps) {
            currentUpdateFps = fps
            currentUpdateInterval?.close()
            currentUpdateInterval = eventLoop.setInterval(fps) {
                dispatchUpdateEvent()
            }
        }
    }

    // PROPS
    open var visible: Boolean = false
    fun hide() = run { visible = false }
    fun show() = run { visible = true }

    open var title: String get() = ""; set(value) = Unit
    open val width: Int = 0
    open val height: Int = 0
    val frameSize get() = SizeInt(width, height)
    val scaledFrameSize get() = SizeInt((width * devicePixelRatio).toInt(), (height * devicePixelRatio).toInt())
    open fun setSize(width: Int, height: Int): Unit = Unit
    open var vsync: Boolean = true
    open var icon: Bitmap? = null
    open var fullscreen: Boolean = false
    open var backgroundColor: RGBA = Colors.BLACK
    var bgcolor: RGBA by this::backgroundColor
    override var quality: GameWindowQuality get() = GameWindowQuality.AUTOMATIC; set(value) = Unit

    // Might be different from width and height for example on high dpi screens
    open val bufferWidth: Int get() = width
    open val bufferHeight: Int get() = height

    val onDebugEnabled = Signal<Unit>()
    val onDebugChanged = Signal<Boolean>()
    open val debugComponent: Any? = null
    open var debug: Boolean = false
        set(value) {
            field = value
            if (value) onDebugEnabled()
            onDebugChanged(value)
        }

    var exitProcessOnClose: Boolean = true
    protected var exitCode = 0; private set
    protected var running = true; protected set
    private var closing = false

    @Deprecated("Use korlibs.render.Cursor", ReplaceWith("korlibs.render.Cursor"))
    object Cursor : korlibs.render.Cursor.Alias

    @Deprecated("Use GameWindowQuality")
    object Quality : GameWindowQuality.Alias
    //typealias Quality = GameWindowQuality

    // Alias for close
    @Deprecated("Use close instead")
    fun exit(exitCode: Int = 0): Unit = close(exitCode)

    override fun close() {
        close(0)
    }

    open fun close(exitCode: Int) {
        if (closing) return
        closing = true
        queue { dispatchDestroyEvent() }
        running = false
        this.exitCode = exitCode
        logger.info { "GameWindow.close" }
        coroutineDispatcher.cancel()
        coroutineDispatcher.close()
    }

    @Deprecated("")
    suspend fun waitClose() = run { while (running) delay(100.milliseconds) }
    @Deprecated("")
    override fun repaint(): Unit = Unit

    @Deprecated("")
    fun startFrame() = run {
        //updatedSinceFrame = 0
    }
    @Deprecated("")
    fun invalidatedView() = run {
        //println("invalidatedView")
        continuousRenderMode.updated()
    }
    @Deprecated("")
    fun handleContextLost() = run { gameWindowInputState.contextLost = true }

    open fun updateGamepads() {
    }

    open val hapticFeedbackGenerateSupport: Boolean get() = false
    open fun hapticFeedbackGenerate(kind: HapticFeedbackKind): Unit = Unit
    open suspend fun clipboardWrite(data: ClipboardData): Unit = Unit
    open suspend fun clipboardRead(): ClipboardData? = null

    //open fun lockMousePointer() = println("WARNING: lockMousePointer not implemented")
    //open fun unlockMousePointer() = Unit
}
