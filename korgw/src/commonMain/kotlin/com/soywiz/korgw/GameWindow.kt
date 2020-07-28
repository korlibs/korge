package com.soywiz.korgw

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.klock.hr.*
import com.soywiz.korag.*
import com.soywiz.korag.log.*
import com.soywiz.korev.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.localCurrentDirVfs
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

expect fun CreateDefaultGameWindow(): GameWindow

interface DialogInterface {
    suspend fun browse(url: URL): Unit = unsupported()
    suspend fun alert(message: String): Unit = unsupported()
    suspend fun confirm(message: String): Boolean = unsupported()
    suspend fun prompt(message: String, default: String = ""): String = unsupported()
    // @TODO: Provide current directory
    suspend fun openFileDialog(filter: String? = null, write: Boolean = false, multi: Boolean = false): List<VfsFile> =
        unsupported()
}

open class GameWindowCoroutineDispatcherSetNow : GameWindowCoroutineDispatcher() {
    var currentTime: HRTimeSpan = PerformanceCounter.hr
    override fun now() = currentTime
}

@UseExperimental(InternalCoroutinesApi::class)
open class GameWindowCoroutineDispatcher : CoroutineDispatcher(), Delay, Closeable {
    override fun dispatchYield(context: CoroutineContext, block: Runnable): Unit = dispatch(context, block)

    class TimedTask(val time: HRTimeSpan, val continuation: CancellableContinuation<Unit>?, val callback: Runnable?) {
        var exception: Throwable? = null
    }

    val tasks = Queue<Runnable>()
    val timedTasks = PriorityQueue<TimedTask> { a, b -> a.time.compareTo(b.time) }

    fun queue(block: () -> Unit) {
        tasks.enqueue(Runnable { block() })
    }

    fun queue(block: Runnable?) {
        if (block != null) {
            tasks.enqueue(block)
        }
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        tasks.enqueue(block)
    }

    open fun now() = PerformanceCounter.hr

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        scheduleResumeAfterDelay(timeMillis.toDouble().hrMilliseconds, continuation)
    }

    fun scheduleResumeAfterDelay(time: HRTimeSpan, continuation: CancellableContinuation<Unit>) {
        val task = TimedTask(now() + time, continuation, null)
        continuation.invokeOnCancellation {
            task.exception = it
        }
        timedTasks.add(task)
    }

    override fun invokeOnTimeout(timeMillis: Long, block: Runnable): DisposableHandle {
        val task = TimedTask(now() + timeMillis.toDouble().hrMilliseconds, null, block)
        timedTasks.add(task)
        return object : DisposableHandle {
            override fun dispose() {
                timedTasks.remove(task)
            }
        }
    }

    @Deprecated("")
    open fun executePending() {
        executePending(1.hrSeconds)
    }

    fun executePending(availableTime: HRTimeSpan) {
        try {
            val startTime = now()
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
                if ((now() - startTime) >= availableTime) break
            }

            while (tasks.isNotEmpty()) {
                val task = tasks.dequeue()
                task?.run()
                if ((now() - startTime) >= availableTime) break
            }
        } catch (e: Throwable) {
            println("Error in GameWindowCoroutineDispatcher.executePending:")
            e.printStackTrace()
        }
    }

    override fun close() {
        executePending(1.hrSeconds)
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

open class GameWindow : EventDispatcher.Mixin(), DialogInterface, Closeable, CoroutineContext.Element, AGWindow {
    override val key: CoroutineContext.Key<*> get() = CoroutineKey
    companion object CoroutineKey : CoroutineContext.Key<GameWindow>

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
    protected val touchEvent = TouchEvent()
    protected val dropFileEvent = DropFileEvent()
    @Deprecated("") protected val gamePadButtonEvent = GamePadButtonEvent()
    @Deprecated("") protected val gamePadStickEvent = GamePadStickEvent()
    protected val gamePadUpdateEvent = GamePadUpdateEvent()
    protected val gamePadConnectionEvent = GamePadConnectionEvent()

    protected open fun _setFps(fps: Int): Int {
        return if (fps <= 0) 60 else fps
    }

    var counterTimePerFrame: HRTimeSpan = 0.0.hrNanoseconds; private set
    val timePerFrame: TimeSpan get() = counterTimePerFrame.timeSpan

    var fps: Int = 60
        set(value) {
            val value = _setFps(value)
            field = value
            counterTimePerFrame = (1_000_000.0 / value).hrMicroseconds
        }

    init {
        fps = 60
    }

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
    open var quality: Quality get() = Quality.AUTOMATIC; set(value) = Unit

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
    override fun close() = run {
        running = false
        println("GameWindow.close")
        coroutineDispatcher.close()
        coroutineDispatcher.cancelChildren()
    }

    suspend fun waitClose() {
        while (running) {
            delay(100.hrMilliseconds)
        }
    }

    override fun repaint() {
    }

    open suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        launchImmediately(getCoroutineDispatcherWithCurrentContext()) {
            entry()
        }
        while (running) {
            val start = PerformanceCounter.hr
            frame()
            val elapsed = PerformanceCounter.hr - start
            val available = counterTimePerFrame - elapsed
            delay(available)
        }
    }

    // Referenced from korge-plugins repo
    fun frame() {
        frame(true)
    }

    fun frame(doUpdate: Boolean, startTime: HRTimeSpan = PerformanceCounter.hr) {
        frameRender(doUpdate)
        if (doUpdate) {
            frameUpdate(startTime)
        }
    }

    fun frameRender(doUpdate: Boolean) {
        try {
            ag.onRender(ag)
            dispatchRenderEvent(update = false)
        } catch (e: Throwable) {
            println("ERROR GameWindow.frameRender:")
            println(e)
        }
    }

    private var lastTime = PerformanceCounter.hr
    fun frameUpdate(startTime: HRTimeSpan = lastTime) {
        try {
            val now = PerformanceCounter.hr
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
        coroutineDispatcher.executePending()
    }

    fun dispatchInitEvent() = dispatch(initEvent)
    fun dispatchPauseEvent() = dispatch(pauseEvent)
    fun dispatchResumeEvent() = dispatch(resumeEvent)
    fun dispatchStopEvent() = dispatch(stopEvent)
    fun dispatchDestroyEvent() = dispatch(destroyEvent)
    fun dispatchDisposeEvent() = dispatch(disposeEvent)
    fun dispatchRenderEvent() = dispatchRenderEvent(true)
    fun dispatchRenderEvent(update: Boolean) = dispatch(renderEvent.also { it.update = update })
    fun dispatchDropfileEvent(type: DropFileEvent.Type, files: List<VfsFile>?) = dispatch(dropFileEvent.also { it.type = type }.also { it.files = files })
    fun dispatchFullscreenEvent(fullscreen: Boolean) = dispatch(fullScreenEvent.also { it.fullscreen = fullscreen })

    fun dispatchReshapeEvent(x: Int, y: Int, width: Int, height: Int) {
        ag.resized(width, height)
        dispatch(reshapeEvent.apply {
            this.x = x
            this.y = y
            this.width = width
            this.height = height
        })
    }

    fun dispatchKeyEvent(type: KeyEvent.Type, id: Int, character: Char, key: Key, keyCode: Int) {
        dispatch(keyEvent.apply {
            this.id = id
            this.character = character
            this.key = key
            this.keyCode = keyCode
            this.type = type
        })
    }

    fun dispatchSimpleMouseEvent(
        type: MouseEvent.Type, id: Int, x: Int, y: Int, button: MouseButton, simulateClickOnUp: Boolean = false
    ) {
        val buttons = 0
        val scrollDeltaX = 0.0
        val scrollDeltaY = 0.0
        val scrollDeltaZ = 0.0
        val isShiftDown = false
        val isCtrlDown = false
        val isAltDown = false
        val isMetaDown = false
        val scaleCoords = false
        dispatchMouseEvent(type, id, x, y, button, buttons, scrollDeltaX, scrollDeltaY, scrollDeltaZ, isShiftDown, isCtrlDown, isAltDown, isMetaDown, scaleCoords, simulateClickOnUp = simulateClickOnUp)
    }

    fun dispatchMouseEvent(
        type: MouseEvent.Type, id: Int, x: Int, y: Int, button: MouseButton, buttons: Int,
        scrollDeltaX: Double, scrollDeltaY: Double, scrollDeltaZ: Double,
        isShiftDown: Boolean, isCtrlDown: Boolean, isAltDown: Boolean, isMetaDown: Boolean,
        scaleCoords: Boolean, simulateClickOnUp: Boolean = false
    ) {
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
        if (simulateClickOnUp && type == MouseEvent.Type.UP) {
            dispatchMouseEvent(MouseEvent.Type.CLICK, id, x, y, button, buttons, scrollDeltaX, scrollDeltaY, scrollDeltaZ, isShiftDown, isCtrlDown, isAltDown, isMetaDown, scaleCoords, simulateClickOnUp = false)
        }
    }

    fun dispatchTouchEventStartStart() = dispatchTouchEventStart(TouchEvent.Type.START)
    fun dispatchTouchEventStartMove() = dispatchTouchEventStart(TouchEvent.Type.MOVE)
    fun dispatchTouchEventStartEnd() = dispatchTouchEventStart(TouchEvent.Type.END)
    fun dispatchTouchEventStart(type: TouchEvent.Type) = touchEvent.startFrame(type)
    fun dispatchTouchEventAddTouch(id: Int, x: Double, y: Double) = touchEvent.touch(id, x, y)
    fun dispatchTouchEventEnd() = dispatch(touchEvent)

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
                coroutineDispatcher.currentTime = PerformanceCounter.hr
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

    var lastRenderTime = PerformanceCounter.hr
    fun elapsedSinceLastRenderTime() = PerformanceCounter.hr - lastRenderTime
    fun render(doUpdate: Boolean) {
        lastRenderTime = PerformanceCounter.hr
        doInitRender()
        frame(doUpdate, lastRenderTime)
        doSwapBuffers()
    }
    fun update() {
        lastRenderTime = PerformanceCounter.hr
        frameUpdate(lastRenderTime)
    }

    fun sleepNextFrame() {
        val now = PerformanceCounter.hr
        val frameTime = (1.toDouble() / fps.toDouble()).hrSeconds
        val delay = frameTime - (now % frameTime)
        if (delay > 0.hrNanoseconds) {
            //println(delayNanos / 1_000_000)
            blockingSleep(delay)
        }
    }

    protected fun sleep(time: HRTimeSpan) {
        // Reimplement: Spinlock!
        val start = PerformanceCounter.hr
        while ((PerformanceCounter.hr - start) < time) {
            doSmallSleep()
        }
    }

    protected fun doSmallSleep() {
        if (!vsync) {
            blockingSleep(0.1.hrMilliseconds)
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

    override suspend fun openFileDialog(filter: String?, write: Boolean, multi: Boolean): List<VfsFile> {
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
    fullscreen: Boolean? = null
) {
    this.setSize(width, height)
    if (title != null) this.title = title
    this.icon = icon
    if (fullscreen != null) this.fullscreen = fullscreen
    this.visible = true
}
