package com.soywiz.korgw

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.kmem.setBits
import com.soywiz.korag.*
import com.soywiz.korag.log.*
import com.soywiz.korev.*
import com.soywiz.korgw.internal.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.localCurrentDirVfs
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.native.concurrent.*

@ThreadLocal
var GLOBAL_CHECK_GL = false

expect fun CreateDefaultGameWindow(): GameWindow

/**
 * @example FileFilter("All files" to listOf("*.*"), "Image files" to listOf("*.png", "*.jpg", "*.jpeg", "*.gif"))
 */
data class FileFilter(val entries: List<Pair<String, List<String>>>) {
    private val regexps = entries.flatMap { it.second }.map { Regex.fromGlob(it) }

    constructor(vararg entries: Pair<String, List<String>>) : this(entries.toList())
    fun matches(fileName: String): Boolean = entries.isEmpty() || regexps.any { it.matches(fileName) }
}

interface DialogInterface : Closeable {
    suspend fun browse(url: URL): Unit = unsupported()
    suspend fun alert(message: String): Unit = unsupported()
    suspend fun confirm(message: String): Boolean = unsupported()
    suspend fun prompt(message: String, default: String = ""): String = unsupported()
    // @TODO: Provide current directory
    suspend fun openFileDialog(filter: FileFilter? = null, write: Boolean = false, multi: Boolean = false, currentDir: VfsFile? = null): List<VfsFile> =
        unsupported()
    override fun close(): Unit = unsupported()
}

suspend fun DialogInterface.openFileDialog(filter: String? = null, write: Boolean = false, multi: Boolean = false): List<VfsFile> {
    return openFileDialog(null, write, multi)
}

suspend fun DialogInterface.alertError(e: Throwable) {
    alert(e.stackTraceToString().lines().take(16).joinToString("\n"))
}

open class GameWindowCoroutineDispatcherSetNow : GameWindowCoroutineDispatcher() {
    var currentTime: TimeSpan = PerformanceCounter.reference
    override fun now() = currentTime
}

@UseExperimental(InternalCoroutinesApi::class)
open class GameWindowCoroutineDispatcher : CoroutineDispatcher(), Delay, Closeable {
    override fun dispatchYield(context: CoroutineContext, block: Runnable): Unit = dispatch(context, block)

    class TimedTask(val time: TimeSpan, val continuation: CancellableContinuation<Unit>?, val callback: Runnable?) {
        var exception: Throwable? = null
    }

    val tasks = Queue<Runnable>()
    val timedTasks = PriorityQueue<TimedTask> { a, b -> a.time.compareTo(b.time) }

    fun queue(block: () -> Unit) {
        //println("queue: $block")
        tasks.enqueue(Runnable { block() })
    }

    fun queue(block: Runnable?) {
        if (block != null) {
            tasks.enqueue(block)
        }
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        //println("dispatch: $block")
        tasks.enqueue(block)
    }

    open fun now() = PerformanceCounter.reference

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        scheduleResumeAfterDelay(timeMillis.toDouble().milliseconds, continuation)
    }

    fun scheduleResumeAfterDelay(time: TimeSpan, continuation: CancellableContinuation<Unit>) {
        val task = TimedTask(now() + time, continuation, null)
        continuation.invokeOnCancellation {
            task.exception = it
        }
        timedTasks.add(task)
    }

    override fun invokeOnTimeout(timeMillis: Long, block: Runnable, context: CoroutineContext): DisposableHandle {
        val task = TimedTask(now() + timeMillis.toDouble().milliseconds, null, block)
        timedTasks.add(task)
        return object : DisposableHandle {
            override fun dispose() {
                timedTasks.remove(task)
            }
        }
    }

    var timedTasksTime = 0.milliseconds
    var tasksTime = 0.milliseconds

    fun executePending(availableTime: TimeSpan) {
        try {
            val startTime = now()

            timedTasksTime = measureTime {
                while (timedTasks.isNotEmpty() && startTime >= timedTasks.head.time) {
                    val item = timedTasks.removeHead()
                    if (item.exception != null) {
                        item.continuation?.resumeWithException(item.exception!!)
                        if (item.callback != null) {
                            item.exception?.printStackTrace()
                        }
                    } else {
                        item.continuation?.resume(Unit)
                        item.callback?.run()
                    }
                    if ((now() - startTime) >= availableTime) {
                        informTooMuchCallbacksToHandleInThisFrame()
                        break
                    }
                }
            }
            tasksTime = measureTime {
                while (tasks.isNotEmpty()) {
                    val task = tasks.dequeue()
                    val time = measureTime {
                        task?.run()
                    }
                    //println("task=$time, task=$task")
                    if ((now() - startTime) >= availableTime) {
                        informTooMuchCallbacksToHandleInThisFrame()
                        break
                    }
                }
            }
        } catch (e: Throwable) {
            println("Error in GameWindowCoroutineDispatcher.executePending:")
            e.printStackTrace()
        }
    }

    open fun informTooMuchCallbacksToHandleInThisFrame() {
        //Console.warn("Too much callbacks to handle in this frame")
    }

    override fun close() {
        executePending(1.seconds)
        println("GameWindowCoroutineDispatcher.close")
        while (timedTasks.isNotEmpty()) {
            timedTasks.removeHead().continuation?.resume(Unit)
        }
        while (tasks.isNotEmpty()) {
            tasks.dequeue()?.run()
        }
    }

    override fun toString(): String = "GameWindowCoroutineDispatcher"
}

open class GameWindow : EventDispatcher.Mixin(), DialogInterface, Closeable, CoroutineContext.Element, AGWindow, Extra by Extra.Mixin() {
    interface ICursor

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

    open fun showSoftKeyboard(force: Boolean = true) {
    }

    open fun hideSoftKeyboard() {
    }

    open var cursor: ICursor = Cursor.DEFAULT

    override val key: CoroutineContext.Key<*> get() = CoroutineKey
    companion object CoroutineKey : CoroutineContext.Key<GameWindow> {
        val MenuItemSeparatror = MenuItem(null)
    }

    override val ag: AG = LogAG()
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
    protected val touchBuilder = TouchBuilder()
    protected val touchEvent get() = touchBuilder.new
    protected val dropFileEvent = DropFileEvent()
    protected val gamePadUpdateEvent = GamePadUpdateEvent()
    protected val gamePadConnectionEvent = GamePadConnectionEvent()

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
    open var quality: Quality get() = Quality.AUTOMATIC; set(value) = Unit

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
    fun exit(): Unit = close()

    var running = true; protected set
    private var closing = false
    override fun close() {
        if (closing) return
        closing = true
        running = false
        println("GameWindow.close")
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
            val start = PerformanceCounter.reference
            frame()
            val elapsed = PerformanceCounter.reference - start
            val available = counterTimePerFrame - elapsed
            delay(available)
        }
    }

    // Referenced from korge-plugins repo
    fun frame() {
        frame(true)
    }

    var renderTime = 0.milliseconds
    var updateTime = 0.milliseconds

    fun frame(doUpdate: Boolean, startTime: TimeSpan = PerformanceCounter.reference) {
        renderTime = measureTime {
            frameRender()
        }
        //println("renderTime=$renderTime")
        if (doUpdate) {
            updateTime = measureTime {
                frameUpdate(startTime)
            }
            //println("updateTime=$updateTime")
        }
    }

    fun frameRender() {
        if (contextLost) {
            contextLost = false
            ag.contextLost()
        }
        if (surfaceChanged) {
            surfaceChanged = false
            ag.mainRenderBuffer.setSize(surfaceX, surfaceY, surfaceWidth, surfaceHeight)
            dispatchReshapeEvent(surfaceX, surfaceY, surfaceWidth, surfaceHeight)
        }
        if (doInitialize) {
            doInitialize = false
            //ag.mainRenderBuffer.setSize(0, 0, width, height)
            println("---------------- Trigger AG.initialized ag.mainRenderBuffer.setSize ($width, $height) --------------")
            dispatch(initEvent)
        }
        try {
            dispatchRenderEvent(update = false)
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

    private var lastTime = PerformanceCounter.reference
    fun frameUpdate(startTime: TimeSpan = lastTime) {
        try {
            val now = PerformanceCounter.reference
            val elapsed = now - startTime
            lastTime = now
            val available = counterTimePerFrame - elapsed
            coroutineDispatcher.executePending(available)
        } catch (e: Throwable) {
            println("ERROR GameWindow.frameRender:")
            println(e)
        }
    }

    fun executePending() {
        coroutineDispatcher.executePending(1.seconds)
    }

    fun dispatchInitEvent() = dispatch(initEvent)
    fun dispatchPauseEvent() = dispatch(pauseEvent)
    fun dispatchResumeEvent() = dispatch(resumeEvent)
    fun dispatchStopEvent() = dispatch(stopEvent)
    fun dispatchDestroyEvent() = dispatch(destroyEvent)
    fun dispatchDisposeEvent() = dispatch(disposeEvent)
    fun dispatchRenderEvent() = dispatchRenderEvent(true)
    fun dispatchRenderEvent(update: Boolean) = dispatch(renderEvent.also { it.update = update })
    fun dispatchDropfileEvent(type: DropFileEvent.Type, files: List<VfsFile>?) = dispatch(dropFileEvent.also {
        it.type = type
        it.files = files
    })
    fun dispatchFullscreenEvent(fullscreen: Boolean) = dispatch(fullScreenEvent.also { it.fullscreen = fullscreen })

    fun dispatchReshapeEvent(x: Int, y: Int, width: Int, height: Int) {
        dispatchReshapeEventEx(x, y, width, height, width, height)
    }

    fun dispatchReshapeEventEx(x: Int, y: Int, width: Int, height: Int, fullWidth: Int, fullHeight: Int) {
        ag.resized(x, y, width, height, fullWidth, fullHeight)
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

    fun dispatchKeyEvent(type: KeyEvent.Type, id: Int, character: Char, key: Key, keyCode: Int) {
        dispatchKeyEventEx(type, id, character, key, keyCode)
    }

    fun dispatchKeyEventEx(
        type: KeyEvent.Type, id: Int, character: Char, key: Key, keyCode: Int,

        shift: Boolean = this.shift, ctrl: Boolean = this.ctrl, alt: Boolean = this.alt, meta: Boolean = this.meta
    ) {
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
        })
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
        scaleCoords: Boolean = this.scaleCoords, simulateClickOnUp: Boolean = false
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

    //open fun lockMousePointer() = println("WARNING: lockMousePointer not implemented")
    //open fun unlockMousePointer() = Unit
}

open class EventLoopGameWindow : GameWindow() {
    override val coroutineDispatcher: GameWindowCoroutineDispatcherSetNow = GameWindowCoroutineDispatcherSetNow()

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
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
            doHandleEvents()
            if (mustPerformRender()) {
                coroutineDispatcher.currentTime = PerformanceCounter.reference
                render(doUpdate = true)
            }
            // Here we can trigger a GC if we have enough time, and we can try to disable GC all the other times.
            if (!vsync) {
                sleepNextFrame()
            }
        }
        dispatchStopEvent()
        dispatchDestroyEvent()

        doDestroy()
    }

    fun mustPerformRender(): Boolean = if (vsync) true else elapsedSinceLastRenderTime() >= counterTimePerFrame

    var lastRenderTime = PerformanceCounter.reference
    fun elapsedSinceLastRenderTime() = PerformanceCounter.reference - lastRenderTime
    fun render(doUpdate: Boolean) {
        lastRenderTime = PerformanceCounter.reference
        doInitRender()
        frame(doUpdate, lastRenderTime)
        doSwapBuffers()
    }
    fun update() {
        lastRenderTime = PerformanceCounter.reference
        frameUpdate(lastRenderTime)
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

open class ZenityDialogs : DialogInterface {
    open suspend fun exec(vararg args: String): String = localCurrentDirVfs.execToString(args.toList())
    override suspend fun browse(url: URL): Unit = run { exec("xdg-open", url.toString()) }
    override suspend fun alert(message: String): Unit = run { exec("zenity", "--warning", "--text=$message") }
    override suspend fun confirm(message: String): Boolean =
        try {
            exec("zenity", "--question", "--text=$message")
            true
        } catch (e: Throwable) {
            false
        }

    override suspend fun prompt(message: String, default: String): String = try {
        exec(
            "zenity",
            "--question",
            "--text=$message",
            "--entry-text=$default"
        )
    } catch (e: Throwable) {
        e.printStackTrace()
        ""
    }

    override suspend fun openFileDialog(filter: FileFilter?, write: Boolean, multi: Boolean, currentDir: VfsFile?): List<VfsFile> {
        return exec(*com.soywiz.korio.util.buildList<String> {
            add("zenity")
            add("--file-selection")
            if (multi) add("--multiple")
            if (write) add("--save")
            if (filter != null) {
                //add("--file-filter=$filter")
            }
        }.toTypedArray())
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { localVfs(it.trim()) }
    }
}

fun GameWindow.mainLoop(entry: suspend GameWindow.() -> Unit) = Korio { loop(entry) }

fun GameWindow.toggleFullScreen() = run { fullscreen = !fullscreen }

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
