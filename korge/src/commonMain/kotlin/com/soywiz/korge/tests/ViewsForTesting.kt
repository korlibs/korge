package com.soywiz.korge.tests

import com.soywiz.kds.iterators.*
import com.soywiz.kds.mapWhile
import com.soywiz.klock.*
import com.soywiz.klock.hr.*
import com.soywiz.korag.log.*
import com.soywiz.korev.*
import com.soywiz.korge.*
import com.soywiz.korge.input.*
import com.soywiz.korge.input.MouseEvents
import com.soywiz.korge.internal.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.jvm.*
import kotlin.math.max

open class ViewsForTesting @JvmOverloads constructor(
    val frameTime: TimeSpan = 10.milliseconds,
    val windowSize: SizeInt = SizeInt(DefaultViewport.WIDTH, DefaultViewport.HEIGHT),
    val virtualSize: SizeInt = SizeInt(windowSize.size.clone()),
    val log: Boolean = false
) {
	val startTime = DateTime(0.0)
	var time = startTime
	val elapsed get() = time - startTime

	val timeProvider: TimeProvider = object : TimeProvider {
		override fun now(): DateTime = time
	}
	val dispatcher = FastGameWindowCoroutineDispatcher()
    class TestGameWindow(initialSize: SizeInt, val dispatcher: FastGameWindowCoroutineDispatcher) : GameWindowLog() {
        override var width: Int = initialSize.width
        override var height: Int = initialSize.height
        override val coroutineDispatcher = dispatcher
    }

	val gameWindow = TestGameWindow(windowSize, dispatcher)
    val ag = if (log) LogAG(windowSize.width, windowSize.height) else DummyAG(windowSize.width, windowSize.height)
	val viewsLog = ViewsLog(gameWindow, ag = ag, gameWindow = gameWindow).also { viewsLog ->
        viewsLog.views.virtualWidth = virtualSize.width
        viewsLog.views.virtualHeight = virtualSize.height
    }
	val injector get() = viewsLog.injector
    val logAg get() = ag as? LogAG?
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
        mouseMoveTo(x.toDouble(), y.toDouble())
        mouseClick(button)
    }

    suspend fun mouseMoveTo(x: Int, y: Int) {
        gameWindow.dispatch(MouseEvent(type = MouseEvent.Type.MOVE, id = 0, x = x, y = y))
        //views.update(frameTime)
        simulateFrame(count = 2)
    }

    suspend fun mouseMoveTo(x: Double, y: Double) = mouseMoveTo(x.toInt(), y.toInt())

    @Deprecated("Kotlin/Native boxes inline+Number")
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
        //mouseDown(button)
        //simulateFrame(count = 2)
        //mouseUp(button)
        mouseEvent(MouseEvent.Type.CLICK, button, false)
        simulateFrame(count = 2)
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

	//@Suppress("UNCHECKED_CAST")
	//fun <T : Scene> testScene(
	//	module: Module,
	//	sceneClass: KClass<T>,
	//	vararg injects: Any,
	//	callback: suspend T.() -> Unit
	//) = viewsTest {
	//	//disableNativeImageLoading {
	//	val sc = Korge.test(
	//		Korge.Config(
	//			module,
	//			sceneClass = sceneClass,
	//			sceneInjects = injects.toList(),
	//			container = canvas,
	//			eventDispatcher = eventDispatcher,
	//			timeProvider = TimeProvider { testDispatcher.time })
	//	)
	//	callback(sc.currentScene as T)
	//	//}
	//}

    val View.viewMouse: MouseEvents get() {
        this.mouse.views = views
        return this.mouse
    }

	suspend fun View.simulateClick() {
        viewMouse.onClick(viewMouse)
		simulateFrame()
	}

	suspend fun View.simulateOver() {
        viewMouse.onOver(viewMouse)
		simulateFrame()
	}

	suspend fun View.simulateOut() {
        viewMouse.onOut(viewMouse)
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
	fun viewsTest(timeout: TimeSpan? = DEFAULT_SUSPEND_TEST_TIMEOUT, block: suspend Stage.() -> Unit): Unit = suspendTest(timeout = timeout, cond = { !OS.isNative }) {
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

		withTimeout(timeout ?: TimeSpan.NULL) {
			while (!completed) {
				simulateFrame()
				dispatcher.executePending()
			}

			if (completedException != null) throw completedException!!
		}
	}

    private var simulatedFrames = 0
    private var lastDelay = PerformanceCounter.hr
	private suspend fun simulateFrame(count: Int = 1) {
		repeat(count) {
            //println("SIMULATE: $frameTime")
            time += frameTime
            // @TODO: ag.onRender + gameWindow.dispatch(RenderEvent) aren't duplicated?
            gameWindow.dispatch(RenderEvent())
            ag.onRender(ag)
            simulatedFrames++
            val now = PerformanceCounter.hr
            val elapsedSinceLastDelay = now - lastDelay
            if (elapsedSinceLastDelay >= 1.hrSeconds) {
                lastDelay = now
                delay(1)
            }
		}
	}

	inner class FastGameWindowCoroutineDispatcher : GameWindowCoroutineDispatcher() {
		val hasMore get() = timedTasks.isNotEmpty() || tasks.isNotEmpty()

		override fun now() = time.unixMillisDouble.hrMilliseconds

		override fun executePending() {
			//println("executePending.hasMore=$hasMore")
			try {
				val timedTasks = mapWhile({ timedTasks.isNotEmpty() }) { timedTasks.removeHead() }

				timedTasks.fastForEach { item ->
					time = DateTime.fromUnix(max(time.unixMillis, item.time.millisecondsDouble))
					if (item.exception != null) {
						item.continuation?.resumeWithException(item.exception!!)
						if (item.callback != null) {
							item.exception?.printStackTrace()
						}
					} else {
						item.continuation?.resume(Unit)
						item.callback?.run()
					}
				}

				while (tasks.isNotEmpty()) {
					val task = tasks.dequeue()
					task.run()
				}
			} catch (e: Throwable) {
				println("Error in GameWindowCoroutineDispatcher.executePending:")
				e.printStackTrace()
			}
		}

		override fun toString(): String = "FastGameWindowCoroutineDispatcher"
	}
}
