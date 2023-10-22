package korlibs.korge.tests

import korlibs.event.*
import korlibs.graphics.*
import korlibs.graphics.log.*
import korlibs.inject.*
import korlibs.io.async.*
import korlibs.io.lang.*
import korlibs.korge.*
import korlibs.korge.input.*
import korlibs.korge.internal.*
import korlibs.korge.render.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.platform.*
import korlibs.render.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.jvm.*
import kotlin.time.Duration.Companion.milliseconds

expect fun enrichTestGameWindow(window: ViewsForTesting.TestGameWindow)

open class ViewsForTesting(
    val frameTime: TimeSpan = 10.milliseconds,
    val windowSize: Size = DefaultViewport.SIZE,
    val virtualSize: Size = windowSize,
    val defaultDevicePixelRatio: Double = 1.0,
    val log: Boolean = false,
) {
	val startTime = DateTime(0.0)
	var time = startTime
	val elapsed get() = time - startTime
    var devicePixelRatio = defaultDevicePixelRatio

	val timeProvider = object : TimeProvider {
        override fun now(): DateTime = time
    }
    inner class TestGameWindow(initialSize: Size) : GameWindowLog() {
        init {
            eventLoop.immediateRun = true
            eventLoop.nowProvider = { time.unixMillisDouble.milliseconds }
        }
        override val autoUpdateInterval = false
        override var androidContextAny: Any? = null
        override val devicePixelRatio: Double get() = this@ViewsForTesting.devicePixelRatio
        override var width: Int = initialSize.width.toInt()
        override var height: Int = initialSize.height.toInt()
    }
    open fun filterLogDraw(str: String, kind: AGBaseLog.Kind): Boolean {
        return kind != AGBaseLog.Kind.SHADER
    }

	val gameWindow = TestGameWindow(windowSize).also {
        enrichTestGameWindow(it)
    }
    val ag: AG by lazy {
        createAg().also {
            it.mainFrameBuffer.setSize(0, 0, windowSize.width.toInt(), windowSize.height.toInt())
        }
    }

    open fun createAg(): AG {
        return object : AGLog(windowSize) {
            override fun log(str: String, kind: Kind) {
                if (this@ViewsForTesting.log && filterLogDraw(str, kind)) {
                    super.log(str, kind)
                }
            }
            override fun toString(): String = "ViewsForTesting.LogAG"
        }
    }

	val viewsLog by lazy { ViewsLog(gameWindow, ag = ag, gameWindow = gameWindow, timeProvider = timeProvider).also { viewsLog ->
        viewsLog.views.virtualWidth = virtualSize.width.toInt()
        viewsLog.views.virtualHeight = virtualSize.height.toInt()
        viewsLog.views.resized(windowSize.width.toInt(), windowSize.height.toInt())
    } }

	val injector get() = viewsLog.injector
    val logAgOrNull get() = ag as? AGLog?
    val logAg get() = logAgOrNull ?: error("Must call ViewsForTesting(log = true) to access logAg")
    val dummyAg get() = ag as? AGDummy?
	val input get() = viewsLog.input
	val views get() = viewsLog.views
    val stage get() = views.stage
	val stats get() = views.stats
	val mouse: MPoint get() = input.mousePos.mutable

    fun resizeGameWindow(width: Int, height: Int, scaleMode: ScaleMode = views.scaleMode, scaleAnchor: Anchor = views.scaleAnchor) {
        ag.mainFrameBuffer.setSize(0, 0, width, height)
        gameWindow.width = width
        gameWindow.height = height
        views.scaleAnchor = scaleAnchor
        views.scaleMode = scaleMode
        gameWindow.dispatchReshapeEvent(0, 0, width, height)
    }

    suspend fun <T> deferred(block: suspend (CompletableDeferred<T>) -> Unit): T {
        val deferred = CompletableDeferred<T>()
        block(deferred)
        return deferred.await()
    }

    @JvmName("deferredUnit")
    suspend fun deferred(block: suspend CompletableDeferred<Unit>.() -> Unit) = deferred<Unit>(block)

    suspend inline fun mouseMoveAndClickTo(x: Number, y: Number, button: MouseButton = MouseButton.LEFT) {
        mouseMoveTo(x, y)
        mouseClick(button)
    }

    suspend fun mouseMoveTo(point: MPoint) = mouseMoveTo(point.x, point.y)

    /**
     * x, y in global/virtual coordinates
     */
    suspend fun mouseMoveTo(x: Int, y: Int) {
        val pos = views.globalToWindowMatrix.transform(Point(x, y))
        gameWindow.dispatch(MouseEvent(type = MouseEvent.Type.MOVE, id = 0, x = pos.x.toInt(), y = pos.y.toInt()))
        //views.update(frameTime)
        simulateFrame(count = 2)
    }

    suspend fun mouseMoveTo(x: Number, y: Number) = mouseMoveTo(x.toInt(), y.toInt())

    private var mouseButtons = 0

    suspend fun mouseDown(button: MouseButton = MouseButton.LEFT) {
        mouseEvent(MouseEvent.Type.DOWN, button, false)
		simulateFrame(count = 2)
	}

	suspend fun mouseUp(button: MouseButton = MouseButton.LEFT) {
        mouseEvent(MouseEvent.Type.UP, button, false)
        simulateFrame(count = 2)
	}

    suspend fun mouseClick(button: MouseButton = MouseButton.LEFT) {
        mouseDown(button)
        simulateFrame(count = 2)
        mouseUp(button)
        //mouseEvent(MouseEvent.Type.CLICK, button, false)
        //simulateFrame(count = 2)
    }

    private fun mouseEvent(type: MouseEvent.Type, button: MouseButton, set: Boolean?) {
        mouseButtons = when (set) {
            true -> mouseButtons or (1 shl button.id)
            false -> mouseButtons and (1 shl button.id).inv()
            else -> mouseButtons
        }
        gameWindow.dispatch(
            MouseEvent(
                type = type,
                id = 0,
                x = views.windowMousePos.x.toInt(),
                y = views.windowMousePos.y.toInt(),
                button = button,
                buttons = mouseButtons
            )
        )
    }

    suspend fun keyType(chars: WString, shift: Boolean = simulatedShift, ctrl: Boolean = simulatedCtrl, alt: Boolean = simulatedAlt, meta: Boolean = simulatedMeta) {
        chars.forEachCodePoint { _, codePoint, _ -> keyType(WChar(codePoint), shift, ctrl, alt, meta) }
    }
    suspend fun keyType(chars: String, shift: Boolean = simulatedShift, ctrl: Boolean = simulatedCtrl, alt: Boolean = simulatedAlt, meta: Boolean = simulatedMeta) {
        chars.forEachCodePoint { _, codePoint, _ -> keyType(WChar(codePoint), shift, ctrl, alt, meta) }
    }

    suspend fun keyType(char: Char, shift: Boolean = simulatedShift, ctrl: Boolean = simulatedCtrl, alt: Boolean = simulatedAlt, meta: Boolean = simulatedMeta) = keyType(WChar(char.code), shift, ctrl, alt, meta)

    suspend fun keyType(char: WChar, shift: Boolean = simulatedShift, ctrl: Boolean = simulatedCtrl, alt: Boolean = simulatedAlt, meta: Boolean = simulatedMeta) {
        gameWindow.dispatch(
            KeyEvent(
                type = KeyEvent.Type.TYPE,
                id = 0, key = Key.NONE, keyCode = char.toInt(), character = char.toInt().toChar(),
                shift = shift, ctrl = ctrl, alt = alt, meta = meta
            )
        )
        simulateFrame(count = 2)
    }

    var simulatedShift: Boolean = false
    var simulatedCtrl: Boolean = false
    var simulatedAlt: Boolean = false
    var simulatedMeta: Boolean = false

    var simulatedCtrlOrMeta: Boolean
        get() = if (Platform.isApple) simulatedMeta else simulatedCtrl
        set(value) {
            if (Platform.isApple) simulatedMeta = value else simulatedCtrl = value
        }

    inline fun <T> cmdOrMeta(cmdOrMeta: Boolean = simulatedCtrlOrMeta, block: () -> T): T {
        return controlKeys(
            ctrl = if (Platform.isApple) simulatedCtrl else cmdOrMeta,
            meta = if (Platform.isApple) cmdOrMeta else simulatedMeta,
        ) {
            block()
        }
    }

    inline fun <T> controlKeys(shift: Boolean = simulatedShift, ctrl: Boolean = simulatedCtrl, alt: Boolean = simulatedAlt, meta: Boolean = simulatedMeta, block: () -> T): T {
        val oldShift = this.simulatedShift
        val oldCtrl = this.simulatedCtrl
        val oldAlt = this.simulatedAlt
        val oldMeta = this.simulatedMeta
        try {
            this.simulatedShift = shift
            this.simulatedCtrl = ctrl
            this.simulatedAlt = alt
            this.simulatedMeta = meta
            return block()
        } finally {
            this.simulatedShift = oldShift
            this.simulatedCtrl = oldCtrl
            this.simulatedAlt = oldAlt
            this.simulatedMeta = oldMeta
        }
    }

    suspend fun keyDownThenUp(key: Key, shift: Boolean = simulatedShift, ctrl: Boolean = simulatedCtrl, alt: Boolean = simulatedAlt, meta: Boolean = simulatedMeta) {
        keyDown(key, shift = shift, ctrl = ctrl, alt = alt, meta = meta)
        keyUp(key, shift = shift, ctrl = ctrl, alt = alt, meta = meta)
    }

    suspend fun keyDown(key: Key, shift: Boolean = simulatedShift, ctrl: Boolean = simulatedCtrl, alt: Boolean = simulatedAlt, meta: Boolean = simulatedMeta) {
        keyEvent(KeyEvent.Type.DOWN, key, shift = shift, ctrl = ctrl, alt = alt, meta = meta)
        simulateFrame(count = 2)
    }

    suspend fun keyUp(key: Key, shift: Boolean = simulatedShift, ctrl: Boolean = simulatedCtrl, alt: Boolean = simulatedAlt, meta: Boolean = simulatedMeta) {
        keyEvent(KeyEvent.Type.UP, key, shift = shift, ctrl = ctrl, alt = alt, meta = meta)
        simulateFrame(count = 2)
    }

    private fun keyEvent(type: KeyEvent.Type, key: Key, keyCode: Int = 0, shift: Boolean = simulatedShift, ctrl: Boolean = simulatedCtrl, alt: Boolean = simulatedAlt, meta: Boolean = simulatedMeta) {
        gameWindow.dispatch(
            KeyEvent(
                type = type,
                id = 0,
                key = key,
                keyCode = keyCode,
                shift = shift,
                ctrl = ctrl,
                alt = alt,
                meta = meta
            )
        )
    }
    val View.viewMouse: MouseEvents get() {
        this.mouse.views = views
        return this.mouse
    }

	suspend fun View.simulateClick() {
        viewMouse.click(viewMouse)
		simulateFrame()
	}

	suspend fun View.simulateOver() {
        viewMouse.over(viewMouse)
		simulateFrame()
	}

	suspend fun View.simulateOut() {
        viewMouse.out(viewMouse)
		simulateFrame()
	}

	suspend fun View.isVisibleToUser(): Boolean {
		if (!this.visible) return false
		if (this.alphaF <= 0.0) return false
		val bounds = this.getGlobalBounds()
		if (bounds.area <= 0.0) return false
		val module = injector.get<KorgeConfig>()
		val visibleBounds = Rectangle(Point.ZERO, module.windowSize)
        return bounds.intersects(visibleBounds)
    }

    var currentFrameTime = this.frameTime

    fun viewsTest(
        timeout: TimeSpan? = DEFAULT_SUSPEND_TEST_TIMEOUT,
        frameTime: TimeSpan = this.frameTime,
        cond: () -> Boolean = { Platform.isJvm && !Platform.isAndroid },
        //devicePixelRatio: Double = defaultDevicePixelRatio,
        forceRenderEveryFrame: Boolean = true,
        block: suspend Stage.() -> Unit
    ): AsyncEntryPointResult = suspendTest(timeout = timeout, cond = cond) {
        currentFrameTime = frameTime
        viewsLog.init()
        this@ViewsForTesting.devicePixelRatio = devicePixelRatio
        //suspendTest(timeout = timeout, cond = { !OS.isAndroid && !OS.isJs && !OS.isNative }) {
        views.prepareViewsBase(gameWindow, forceRenderEveryFrame = forceRenderEveryFrame)

		injector.mapInstance<KorgeConfig>(KorgeConfig(
			title = "KorgeViewsForTesting",
            windowSize = this@ViewsForTesting.windowSize,
			virtualSize = this@ViewsForTesting.virtualSize,
        ))

		var completed = false
		var completedException: Throwable? = null

        gameWindow.queueSuspend {
            try {
                block(views.stage)
            } catch (e: Throwable) {
                completedException = e
            } finally {
                completed = true
            }
        }

        //println("[a0]")
        try {
            withTimeoutNullable(timeout ?: TimeSpan.NIL) {
                //println("[a1]")
                var nframes = 0
                while (!completed) {
                    //println("[1]")
                    simulateFrame()
                    //println("[2]")
                    //val ntasks = gameWindow.eventLoop.runAvailableNextTask(10)
                    val ntasks = gameWindow.eventLoop.runAvailableNextTasks()
                    //println("[3]")

                    if (ntasks == 0) {
                        nframes++
                        if (nframes >= 10000) {
                            completedException = Exception("No tasks ran during frames=$nframes")
                            break
                        }
                    } else {
                        nframes = 0
                    }
                    //if (ntasks > 0) println("Ran ntasks=$ntasks")
                    //println("Ran ntasks=$ntasks")
                }

                //println("[a2]")
                if (completedException != null) throw completedException!!
            }
        } finally {
            gameWindow.close()
        }
        //println("[a3]")
	}

    @Suppress("UNCHECKED_CAST")
    inline fun <reified S : Scene> sceneTest(
        config: Korge? = null,
        noinline configureInjector: Injector.() -> Unit = {},
        timeout: TimeSpan? = DEFAULT_SUSPEND_TEST_TIMEOUT,
        frameTime: TimeSpan = this.frameTime,
        crossinline block: suspend S.() -> Unit
    ): AsyncEntryPointResult = viewsTest(timeout, frameTime) {
        config?.apply { injector.configInjector() }
        injector.configureInjector()
        val container = sceneContainer(views)
        container.changeTo<S>()
        with(container.currentScene as S) { block() }
    }


    private var simulatedFrames = 0
    private var lastDelay = PerformanceCounter.reference
	private suspend fun simulateFrame(count: Int = 1) {
		repeat(count) {
            //println("SIMULATE: $frameTime")
            time += currentFrameTime
            gameWindow.dispatchUpdateEvent()
            gameWindow.dispatchNewRenderEvent()
            simulatedFrames++
            val now = PerformanceCounter.reference
            val elapsedSinceLastDelay = now - lastDelay
            if (elapsedSinceLastDelay >= 1.seconds) {
                lastDelay = now
                delay(1.milliseconds)
            }
		}
	}

    suspend fun delayFrame() {
        simulateFrame()
    }

    inline fun <T : AG> testRenderContext(ag: T, block: (RenderContext) -> Unit): T {
        val ctx = RenderContext(ag, views)
        block(ctx)
        ctx.flush()
        return ag
    }
}
