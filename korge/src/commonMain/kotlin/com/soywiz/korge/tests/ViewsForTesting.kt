package com.soywiz.korge.tests

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.klock.milliseconds
import com.soywiz.korag.log.*
import com.soywiz.korev.*
import com.soywiz.korge.*
import com.soywiz.korge.input.*
import com.soywiz.korge.input.MouseEvents
import com.soywiz.korge.internal.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korinject.AsyncInjector
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.jvm.*

open class ViewsForTesting(
    val frameTime: TimeSpan = 10.milliseconds,
    val windowSize: SizeInt = SizeInt(DefaultViewport.WIDTH, DefaultViewport.HEIGHT),
    val virtualSize: SizeInt = SizeInt(windowSize.size.clone()),
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
	val dispatcher = FastGameWindowCoroutineDispatcher()
    class TestGameWindow(initialSize: SizeInt, val dispatcher: FastGameWindowCoroutineDispatcher) : GameWindowLog() {
        override var width: Int = initialSize.width
        override var height: Int = initialSize.height
        override val coroutineDispatcher = dispatcher
    }
    open fun filterLogDraw(str: String, kind: LogBaseAG.Kind): Boolean {
        return kind != LogBaseAG.Kind.SHADER
    }

	val gameWindow = TestGameWindow(windowSize, dispatcher)
    val ag = object : LogAG(windowSize.width, windowSize.height) {
        override val devicePixelRatio: Double get() = this@ViewsForTesting.devicePixelRatio
        override fun log(str: String, kind: Kind) {
            if (this@ViewsForTesting.log && filterLogDraw(str, kind)) {
                super.log(str, kind)
            }
        }
    }
	val viewsLog = ViewsLog(gameWindow, ag = ag, gameWindow = gameWindow, timeProvider = timeProvider).also { viewsLog ->
        viewsLog.views.virtualWidth = virtualSize.width
        viewsLog.views.virtualHeight = virtualSize.height
    }
	val injector get() = viewsLog.injector
    val logAgOrNull get() = ag as? LogAG?
    val logAg get() = logAgOrNull ?: error("Must call ViewsForTesting(log = true) to access logAg")
    val dummyAg get() = ag as? DummyAG?
	val input get() = viewsLog.input
	val views get() = viewsLog.views
    val stage get() = views.stage
	val stats get() = views.stats
	val mouse get() = input.mouse

    fun resizeGameWindow(width: Int, height: Int, scaleMode: ScaleMode = views.scaleMode, scaleAnchor: Anchor = views.scaleAnchor) {
        logAg?.backWidth = width
        logAg?.backHeight = height
        dummyAg?.backWidth = width
        dummyAg?.backHeight = height
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

    suspend fun mouseMoveTo(point: IPoint) = mouseMoveTo(point.x, point.y)

    suspend fun mouseMoveTo(x: Int, y: Int) {
        gameWindow.dispatch(MouseEvent(type = MouseEvent.Type.MOVE, id = 0, x = x, y = y))
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
                x = input.mouse.x.toInt(),
                y = input.mouse.y.toInt(),
                button = button,
                buttons = mouseButtons
            )
        )
    }

    suspend fun keyDown(key: Key) {
        keyEvent(KeyEvent.Type.DOWN, key)
        simulateFrame(count = 2)
    }

    suspend fun keyUp(key: Key) {
        keyEvent(KeyEvent.Type.UP, key)
        simulateFrame(count = 2)
    }

    private fun keyEvent(type: KeyEvent.Type, key: Key) {
        gameWindow.dispatch(
            KeyEvent(
                type = type,
                id = 0,
                key = key,
                keyCode = 0,
                shift = false,
                ctrl = false,
                alt = false,
                meta = false
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
		if (this.alpha <= 0.0) return false
		val bounds = this.getGlobalBounds()
		if (bounds.area <= 0.0) return false
		val module = injector.get<Module>()
		val visibleBounds = Rectangle(0, 0, module.windowSize.width, module.windowSize.height)
		if (!bounds.intersects(visibleBounds)) return false
		return true
	}

	// @TODO: Run a faster eventLoop where timers happen much faster
    fun viewsTest(
        timeout: TimeSpan? = DEFAULT_SUSPEND_TEST_TIMEOUT,
        frameTime: TimeSpan = this.frameTime,
        //devicePixelRatio: Double = defaultDevicePixelRatio,
        block: suspend Stage.() -> Unit
    ) = suspendTest(timeout = timeout, cond = { OS.isJvm && !OS.isAndroid }) {
        this@ViewsForTesting.devicePixelRatio = devicePixelRatio
        //suspendTest(timeout = timeout, cond = { !OS.isAndroid && !OS.isJs && !OS.isNative }) {
        Korge.prepareViewsBase(views, gameWindow, fixedSizeStep = frameTime)

		injector.mapInstance<Module>(object : Module() {
			override val title = "KorgeViewsForTesting"
			override val size = this@ViewsForTesting.windowSize
			override val windowSize = this@ViewsForTesting.windowSize
		})

		var completed = false
		var completedException: Throwable? = null

		this@ViewsForTesting.dispatcher.dispatch(coroutineContext, Runnable {
			launchImmediately(views.coroutineContext + dispatcher) {
				try {
                    block(views.stage)
				} catch (e: Throwable) {
					completedException = e
				} finally {
					completed = true
				}
			}
		})

		withTimeout(timeout ?: TimeSpan.NIL) {
			while (!completed) {
                //println("FRAME")
				simulateFrame()
				dispatcher.executePending(1.seconds)
			}

			if (completedException != null) throw completedException!!
		}
	}

    @Suppress("UNCHECKED_CAST")
    inline fun <reified S : Scene> sceneTest(
        module: Module? = null,
        crossinline mappingsForTest: AsyncInjector.() -> Unit = {},
        timeout: TimeSpan? = DEFAULT_SUSPEND_TEST_TIMEOUT,
        frameTime: TimeSpan = this.frameTime,
        crossinline block: suspend S.() -> Unit
    ): Unit =
        viewsTest(timeout, frameTime) {
            module?.apply {
                injector.configure()
            }

            injector.mappingsForTest()

            val container = sceneContainer(views)
            container.changeTo<S>()

            with(container.currentScene as S) {
                block()
            }
        }


    private var simulatedFrames = 0
    private var lastDelay = PerformanceCounter.reference
	private suspend fun simulateFrame(count: Int = 1) {
		repeat(count) {
            //println("SIMULATE: $frameTime")
            time += frameTime
            gameWindow.dispatchRenderEvent()
            simulatedFrames++
            val now = PerformanceCounter.reference
            val elapsedSinceLastDelay = now - lastDelay
            if (elapsedSinceLastDelay >= 1.seconds) {
                lastDelay = now
                delay(1)
            }
		}
	}

    suspend fun delayFrame() {
        simulateFrame()
    }

    class TimedTask2(val time: DateTime, val continuation: CancellableContinuation<Unit>?, val callback: Runnable?) {
        var exception: Throwable? = null
        override fun toString(): String = "${time.unixMillisLong}"
    }

    inner class FastGameWindowCoroutineDispatcher : GameWindowCoroutineDispatcher() {
		val hasMore get() = timedTasks2.isNotEmpty() || tasks.isNotEmpty()

		override fun now() = time.unixMillisDouble.milliseconds

        val timedTasks2 = TGenPriorityQueue<TimedTask2> { a, b -> a.time.compareTo(b.time) }

        override fun invokeOnTimeout(timeMillis: Long, block: Runnable, context: CoroutineContext): DisposableHandle {
            //println("invokeOnTimeout: $timeMillis")
            val task = TimedTask2(time + timeMillis.toDouble().milliseconds, null, block)
            timedTasks2.add(task)
            return object : DisposableHandle {
                override fun dispose() {
                    timedTasks2.remove(task)
                }
            }
        }

        override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
            //println("scheduleResumeAfterDelay: $timeMillis")
            val task = TimedTask2(time + timeMillis.toDouble().milliseconds, continuation, null)
            continuation.invokeOnCancellation {
                task.exception = it
            }
            timedTasks2.add(task)
        }

		override fun toString(): String = "FastGameWindowCoroutineDispatcher"
	}
}
