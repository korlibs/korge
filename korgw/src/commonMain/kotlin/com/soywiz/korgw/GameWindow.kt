package com.soywiz.korgw

import com.soywiz.kds.*
import com.soywiz.kds.lock.*
import com.soywiz.klock.*
import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.annotation.*
import com.soywiz.korag.log.*
import com.soywiz.korev.*
import com.soywiz.korgw.GameWindow.Quality.*
import com.soywiz.korgw.internal.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.native.concurrent.*
import kotlin.properties.*

@ThreadLocal
var GLOBAL_CHECK_GL = false

data class GameWindowCreationConfig(
    val multithreaded: Boolean? = null,
    val hdr: Boolean? = null,
    val msaa: Int? = null,
    val checkGl: Boolean = false,
    val logGl: Boolean = false,
    val cacheGl: Boolean = false,
)

expect fun CreateDefaultGameWindow(config: GameWindowCreationConfig): GameWindow
fun CreateDefaultGameWindow() = CreateDefaultGameWindow(GameWindowCreationConfig())

/**
 * @example FileFilter("All files" to listOf("*.*"), "Image files" to listOf("*.png", "*.jpg", "*.jpeg", "*.gif"))
 */
open class GameWindowCoroutineDispatcherSetNow : GameWindowCoroutineDispatcher() {
    var currentTime: TimeSpan = PerformanceCounter.reference
    override fun now() = currentTime
}

@OptIn(InternalCoroutinesApi::class)
open class GameWindowCoroutineDispatcher : CoroutineDispatcher(), Delay, Closeable {
    override fun dispatchYield(context: CoroutineContext, block: Runnable): Unit = dispatch(context, block)

    class TimedTask(val time: TimeSpan, val continuation: CancellableContinuation<Unit>?, val callback: Runnable?) {
        var exception: Throwable? = null
    }

    @PublishedApi internal val tasks = Queue<Runnable>()
    @PublishedApi internal val timedTasks = PriorityQueue<TimedTask> { a, b -> a.time.compareTo(b.time) }

    protected val _tasks: Queue<Runnable> get() = tasks
    protected val _timedTasks: PriorityQueue<TimedTask> get() = timedTasks
    val lock = NonRecursiveLock()

    fun hasTasks() = tasks.isNotEmpty()

    fun queue(block: Runnable?) {
        if (block == null) return
        lock { tasks.enqueue(block) }
    }

    fun queue(block: () -> Unit) = queue(Runnable { block() })

    override fun dispatch(context: CoroutineContext, block: Runnable) = queue(block) // @TODO: We are not using the context

    open fun now() = PerformanceCounter.reference

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        scheduleResumeAfterDelay(timeMillis.toDouble().milliseconds, continuation)
    }

    fun scheduleResumeAfterDelay(time: TimeSpan, continuation: CancellableContinuation<Unit>) {
        val task = TimedTask(now() + time, continuation, null)
        continuation.invokeOnCancellation {
            task.exception = it
        }
        lock { timedTasks.add(task) }
    }

    override fun invokeOnTimeout(timeMillis: Long, block: Runnable, context: CoroutineContext): DisposableHandle {
        val task = TimedTask(now() + timeMillis.toDouble().milliseconds, null, block)
        lock { timedTasks.add(task) }
        return DisposableHandle { lock { timedTasks.remove(task) } }
    }

    var timedTasksTime = 0.milliseconds
    var tasksTime = 0.milliseconds

    /**
     * Allows to configure how much time per frame is available to execute pending tasks,
     * despite time available in the frame.
     * When not set it uses the remaining available time in frame
     **/
    var maxAllocatedTimeForTasksPerFrame: TimeSpan? = null

    /** On JS this cannot work, because it requires the real event loop to be reached */
    @KoragExperimental
    open fun <T> runBlockingNoJs(coroutineContext: CoroutineContext, block: suspend () -> T): T {
        var completed = false
        var finalException: Throwable? = null
        var finalResult: T? = null

        block.startCoroutine(object : Continuation<T> {
            override val context: CoroutineContext get() = coroutineContext
            override fun resumeWith(result: Result<T>) {
                finalResult = result.getOrNull()
                finalException = result.exceptionOrNull()
                completed = true
            }
        })
        while (!completed) {
            executePending(256.milliseconds)
            if (completed) break
            blockingSleep(1.milliseconds)
        }
        if (finalException != null) {
            throw finalException!!
        }
        return finalResult as T
    }

    open fun executePending(availableTime: TimeSpan) {
        try {
            val startTime = now()

            var processedTimedTasks = 0
            var processedTasks = 0

            timedTasksTime = measureTime {
                while (true) {
                    val item = lock {
                        if (timedTasks.isNotEmpty() && startTime >= timedTasks.head.time) timedTasks.removeHead() else null
                    } ?: break
                    try {
                        if (item.exception != null) {
                            item.continuation?.resumeWithException(item.exception!!)
                            if (item.callback != null) {
                                item.exception?.printStackTrace()
                            }
                        } else {
                            item.continuation?.resume(Unit)
                            item.callback?.run()
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    } finally {
                        processedTimedTasks++
                    }
                    val elapsedTime = now() - startTime
                    if (elapsedTime >= availableTime) {
                        informTooManyCallbacksToHandleInThisFrame(elapsedTime, availableTime, processedTimedTasks, processedTasks)
                        break
                    }
                }
            }
            tasksTime = measureTime {
                while (true) {
                    val task = lock { (if (tasks.isNotEmpty()) tasks.dequeue() else null) } ?: break
                    val time = measureTime {
                        try {
                            task.run()
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        } finally {
                            processedTasks++
                        }
                    }
                    //println("task=$time, task=$task")
                    val elapsed = now() - startTime
                    if (elapsed >= availableTime) {
                        informTooManyCallbacksToHandleInThisFrame(elapsed, availableTime, processedTimedTasks, processedTasks)
                        break
                    }
                }
            }
        } catch (e: Throwable) {
            println("Error in GameWindowCoroutineDispatcher.executePending:")
            e.printStackTrace()
        }
    }

    val tooManyCallbacksLogger = Logger("Korgw.GameWindow.TooManyCallbacks")

    open fun informTooManyCallbacksToHandleInThisFrame(elapsedTime: TimeSpan, availableTime: TimeSpan, processedTimedTasks: Int, processedTasks: Int) {
        tooManyCallbacksLogger.warn { "Too many callbacks to handle in this frame elapsedTime=${elapsedTime.roundMilliseconds()}, availableTime=${availableTime.roundMilliseconds()} pending timedTasks=${timedTasks.size}, tasks=${tasks.size}, processedTimedTasks=$processedTimedTasks, processedTasks=$processedTasks" }
    }

    override fun close() {
        executePending(2.seconds)
        logger.info { "GameWindowCoroutineDispatcher.close" }
        while (timedTasks.isNotEmpty()) {
            timedTasks.removeHead().continuation?.resume(Unit)
        }
        while (tasks.isNotEmpty()) {
            tasks.dequeue().run()
        }
    }

    override fun toString(): String = "GameWindowCoroutineDispatcher"

    companion object {
        val logger = Logger("GameWindow")
    }
}

interface GameWindowConfig {
    val quality: GameWindow.Quality

    class Impl(
        override val quality: GameWindow.Quality = GameWindow.Quality.AUTOMATIC,
    ) : GameWindowConfig
}

typealias GameWindowQuality = GameWindow.Quality

open class GameWindow :
    EventDispatcher.Mixin(),
    DialogInterfaceProvider,
    DeviceDimensionsProvider,
    CoroutineContext.Element,
    AGWindow,
    GameWindowConfig,
    Extra by Extra.Mixin()
{
    sealed interface ICursor

    override val dialogInterface: DialogInterface get() = DialogInterface.Unsupported

    data class CustomCursor(val shape: Shape, val name: String = "custom") : ICursor, Extra by Extra.Mixin() {
        val bounds: IRectangle = this.shape.bounds
        fun createBitmap(size: ISize? = null, native: Boolean = true) = shape.renderWithHotspot(fit = size, native = native)
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

    open fun setInputRectangle(windowRect: MRectangle) {
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

    override val key: CoroutineContext.Key<*> get() = CoroutineKey
    companion object CoroutineKey : CoroutineContext.Key<GameWindow> {
        val logger = Logger("GameWindow")

        val MenuItemSeparatror = MenuItem(null)
    }

    //override val ag: AG = LogAG()
    override val ag: AG = AGDummy()

    open val coroutineDispatcher: GameWindowCoroutineDispatcher = GameWindowCoroutineDispatcher()

    fun getCoroutineDispatcherWithCurrentContext(coroutineContext: CoroutineContext): CoroutineContext = coroutineContext + coroutineDispatcher
    suspend fun getCoroutineDispatcherWithCurrentContext(): CoroutineContext = getCoroutineDispatcherWithCurrentContext(coroutineContext)

    fun queue(callback: () -> Unit) = coroutineDispatcher.queue(callback)
    fun queue(callback: Runnable) = coroutineDispatcher.queue(callback)

    protected val pauseEvent = PauseEvent()
    protected val resumeEvent = ResumeEvent()
    protected val stopEvent = StopEvent()
    protected val destroyEvent = DestroyEvent()
    protected val renderEvent = RenderEvent()
    protected val initEvent = InitEvent()
    protected val disposeEvent = DisposeEvent()
    protected val fullScreenEvent = FullScreenEvent()
    private val reshapeEvent = ReshapeEvent()
    protected val keyEvent = KeyEvent()
    protected val mouseEvent = MouseEvent()
    protected val gestureEvent = GestureEvent()
    protected val touchBuilder = TouchBuilder()
    protected val touchEvent get() = touchBuilder.new
    protected val dropFileEvent = DropFileEvent()

    @KoragExperimental
    suspend fun <T> runBlockingNoJs(block: suspend () -> T): T {
        return runBlockingNoJs(coroutineContext, block)
    }

    /** On JS this cannot work, because it requires the real event loop to be reached */
    @KoragExperimental
    open fun <T> runBlockingNoJs(coroutineContext: CoroutineContext, block: suspend () -> T): T {
        return coroutineDispatcher.runBlockingNoJs(coroutineContext, block)
    }

    fun onRenderEvent(block: (RenderEvent) -> Unit) {
        addEventListener<RenderEvent>(block)
    }

    val counterTimePerFrame: TimeSpan get() = (1_000_000.0 / fps).microseconds
    val timePerFrame: TimeSpan get() = counterTimePerFrame

    open fun computeDisplayRefreshRate(): Int {
        return 60
    }

    open fun registerTime(name: String, time: TimeSpan) {
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
    enum class Quality {
        /** Will render to lower resolutions, ignoring devicePixelRatio on retina-like screens */
        PERFORMANCE,
        /** Will render to higher resolutions, using devicePixelRatio on retina-like screens */
        QUALITY,
        /** Will choose [PERFORMANCE] or [QUALITY] based on some heuristics */
        AUTOMATIC;

        private val UPPER_BOUND_RENDERED_PIXELS = 4_000_000

        fun computeTargetScale(
            width: Int,
            height: Int,
            devicePixelRatio: Double,
            targetPixels: Int = UPPER_BOUND_RENDERED_PIXELS
        ): Double = when (this) {
            PERFORMANCE -> 1.0
            QUALITY -> devicePixelRatio
            AUTOMATIC -> {
                listOf(devicePixelRatio, 2.0, 1.0)
                    .firstOrNull { width * height * it <= targetPixels }
                    ?: 1.0
            }
        }
    }

    open fun setSize(width: Int, height: Int): Unit = Unit
    // Alias for close
    fun exit(exitCode: Int = 0): Unit = close(exitCode)

    var exitProcessOnClose: Boolean = true
    @Deprecated("Use exitProcessOnClose instead")
    var exitProcessOnExit: Boolean by ::exitProcessOnClose
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
        launchImmediately(getCoroutineDispatcherWithCurrentContext()) {
            entry()
        }
        while (running) {
            val elapsed = frame()
            val available = counterTimePerFrame - elapsed
            if (available > TimeSpan.ZERO) delay(available)
        }
    }

    // Referenced from korge-plugins repo
    var renderTime = 0.milliseconds
    var updateTime = 0.milliseconds

    var onContinuousRenderModeUpdated: ((Boolean) -> Unit)? = null
    open var continuousRenderMode: Boolean by Delegates.observable(true) { prop, old, new ->
        onContinuousRenderModeUpdated?.invoke(new)
    }

    fun frame(doUpdate: Boolean = true, doRender: Boolean = true, frameStartTime: TimeSpan = PerformanceCounter.reference): TimeSpan {
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
        val endTime = PerformanceCounter.reference
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
            dispatch(initEvent)
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

    var gamePadTime: TimeSpan = TimeSpan.ZERO
    fun frameUpdate(startTime: TimeSpan) {
        gamePadTime = measureTime {
            updateGamepads()
        }
        try {
            val now = PerformanceCounter.reference
            val consumed = now - startTime
            //val remaining = (counterTimePerFrame - consumed) - 2.milliseconds // Do not push too much so give two extra milliseconds just in case
            //val timeForTasks = coroutineDispatcher.maxAllocatedTimeForTasksPerFrame ?: (remaining * 10) // We would be skipping up to 10 frames by default
            val timeForTasks = coroutineDispatcher.maxAllocatedTimeForTasksPerFrame ?: (counterTimePerFrame * 10) // We would be skipping up to 10 frames by default
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

    fun executePending(availableTime: TimeSpan) {
        coroutineDispatcher.executePending(availableTime)
    }

    fun dispatchInitEvent() = dispatch(initEvent)
    fun dispatchPauseEvent() = dispatch(pauseEvent)
    fun dispatchResumeEvent() = dispatch(resumeEvent)
    fun dispatchStopEvent() = dispatch(stopEvent)
    fun dispatchDestroyEvent() = dispatch(destroyEvent)
    fun dispatchDisposeEvent() = dispatch(disposeEvent)
    fun dispatchRenderEvent(update: Boolean = true, render: Boolean = true) = dispatch(renderEvent.also {
        it.update = update
        it.render = render
    })
    fun dispatchDropfileEvent(type: DropFileEvent.Type, files: List<VfsFile>?) = dispatch(dropFileEvent.also {
        it.type = type
        it.files = files
    })
    fun dispatchFullscreenEvent(fullscreen: Boolean) = dispatch(fullScreenEvent.also { it.fullscreen = fullscreen })

    fun dispatchReshapeEvent(x: Int, y: Int, width: Int, height: Int) {
        dispatchReshapeEventEx(x, y, width, height, width, height)
    }

    fun dispatchReshapeEventEx(x: Int, y: Int, width: Int, height: Int, fullWidth: Int, fullHeight: Int) {
        ag.mainFrameBuffer.setSize(x, y, width, height, fullWidth, fullHeight)
        dispatch(reshapeEvent.apply {
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
    private val scrollDeltaX = 0.0
    private val scrollDeltaY = 0.0
    private val scrollDeltaZ = 0.0
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
        dispatch(keyEvent.apply {
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
        return keyEvent._stopPropagation
    }

    fun dispatchSimpleMouseEvent(
        type: MouseEvent.Type, id: Int, x: Int, y: Int, button: MouseButton, simulateClickOnUp: Boolean = false
    ) {
        dispatchMouseEvent(type, id, x, y, button, simulateClickOnUp = simulateClickOnUp)
    }

    fun dispatchMouseEvent(
        type: MouseEvent.Type, id: Int, x: Int, y: Int, button: MouseButton, buttons: Int = this.mouseButtons,
        scrollDeltaX: Double = this.scrollDeltaX, scrollDeltaY: Double = this.scrollDeltaY, scrollDeltaZ: Double = this.scrollDeltaZ,
        isShiftDown: Boolean = this.shift, isCtrlDown: Boolean = this.ctrl, isAltDown: Boolean = this.alt, isMetaDown: Boolean = this.meta,
        scaleCoords: Boolean = this.scaleCoords, simulateClickOnUp: Boolean = false,
        scrollDeltaMode: MouseEvent.ScrollDeltaMode = MouseEvent.ScrollDeltaMode.LINE
    ) {
        if (type != MouseEvent.Type.DOWN && type != MouseEvent.Type.UP) {
            this.mouseButtons = this.mouseButtons.setBits(if (button != null) 1 shl button.ordinal else 0, type == MouseEvent.Type.DOWN)
        }
        dispatch(mouseEvent.apply {
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

    // iOS tools
    fun dispatchTouchEventModeIos() { touchBuilder.mode = TouchBuilder.Mode.IOS }
    fun dispatchTouchEventStartStart() = touchBuilder.startFrame(TouchEvent.Type.START)
    fun dispatchTouchEventStartMove() = touchBuilder.startFrame(TouchEvent.Type.MOVE)
    fun dispatchTouchEventStartEnd() = touchBuilder.startFrame(TouchEvent.Type.END)
    fun dispatchTouchEventAddTouch(id: Int, x: Double, y: Double) = touchBuilder.touch(id, x, y)
    fun dispatchTouchEventEnd() = dispatch(touchBuilder.endFrame())

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
    override val coroutineDispatcher: GameWindowCoroutineDispatcherSetNow = GameWindowCoroutineDispatcherSetNow()

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
        val frameStartTime: TimeSpan = PerformanceCounter.reference
        val mustRender = doRender()
        if (mustRender) renderInternal(doUpdate = doUpdate, frameStartTime = frameStartTime)
    }

    @PublishedApi
    internal fun renderInternal(doUpdate: Boolean, frameStartTime: TimeSpan = PerformanceCounter.reference) {
        coroutineDispatcher.currentTime = PerformanceCounter.reference
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

    fun sleepNextFrame() {
        val now = PerformanceCounter.reference
        val frameTime = (1.toDouble() / fps.toDouble()).seconds
        val delay = frameTime - (now % frameTime)
        if (delay > 0.0.milliseconds) {
            //println(delayNanos / 1_000_000)
            blockingSleep(delay)
        }
    }

    protected fun sleep(time: TimeSpan) {
        // Reimplement: Spinlock!
        val start = PerformanceCounter.reference
        while ((PerformanceCounter.reference - start) < time) {
            doSmallSleep()
        }
    }

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
    width: Int,
    height: Int,
    title: String? = "GameWindow",
    icon: Bitmap? = null,
    fullscreen: Boolean? = null,
    bgcolor: RGBA = Colors.BLACK,
) {
    this.setSize(width, height)
    if (title != null) this.title = title
    this.icon = icon
    if (fullscreen != null) this.fullscreen = fullscreen
    this.bgcolor = bgcolor
    this.visible = true
}

fun GameWindow.onDragAndDropFileEvent(block: suspend (DropFileEvent) -> Unit) {
    addEventListener<DropFileEvent> { event ->
        launchImmediately(coroutineDispatcher) {
            block(event)
        }
    }
}
