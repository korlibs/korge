package com.soywiz.korge.tests

import com.soywiz.kds.iterators.*
import com.soywiz.kds.mapWhile
import com.soywiz.klock.*
import com.soywiz.klock.hr.hrMilliseconds
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
import kotlin.jvm.JvmOverloads
import kotlin.math.max

open class ViewsForTesting @JvmOverloads constructor(val frameTime: TimeSpan = 10.milliseconds, val size: SizeInt = SizeInt(640, 480)) {
	val startTime = DateTime(0.0)
	var time = startTime
	val elapsed get() = time - startTime
	//val dispatcher2 = TestCoroutineDispatcher(frameTime)
	//val dispatcher = Dispatchers.Default

	val timeProvider: TimeProvider = object : TimeProvider {
		override fun now(): DateTime = time
	}
	val koruiEventDispatcher = EventDispatcher()
	val dispatcher = FastGameWindowCoroutineDispatcher()
	val gameWindow = object : GameWindowLog() {
		override val coroutineDispatcher = dispatcher
	}
	val viewsLog = ViewsLog(dispatcher, ag = LogAG(DefaultViewport.WIDTH, DefaultViewport.HEIGHT), gameWindow = gameWindow)
	val injector get() = viewsLog.injector
	val ag get() = viewsLog.ag
	val input get() = viewsLog.input
	val views get() = viewsLog.views
    val stage get() = views.stage
	val stats get() = views.stats
	val mouse get() = input.mouse

    suspend inline fun mouseMoveAndClickTo(x: Number, y: Number, button: MouseButton = MouseButton.LEFT) {
        mouseMoveTo(x.toDouble(), y.toDouble())
        mouseClick(button)
    }

    suspend fun mouseMoveTo(x: Int, y: Int) {
        koruiEventDispatcher.dispatch(MouseEvent(type = MouseEvent.Type.MOVE, id = 0, x = x, y = y))
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
        koruiEventDispatcher.dispatch(
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
	fun viewsTest(block: suspend Stage.() -> Unit): Unit = suspendTest {
		if (OS.isNative) return@suspendTest // @TODO: kotlin-native SKIP NATIVE FOR NOW: kotlin.IllegalStateException: Cannot execute task because event loop was shut down
        Korge.prepareViewsBase(views, koruiEventDispatcher, fixedSizeStep = frameTime)

		injector.mapInstance<Module>(object : Module() {
			override val title = "KorgeViewsForTesting"
			override val size = this@ViewsForTesting.size
			override val windowSize = this@ViewsForTesting.size
		})

		var completed = false
		var completedException: Throwable? = null

		this@ViewsForTesting.dispatcher.dispatch(coroutineContext, Runnable {
			launchImmediately(dispatcher) {
				try {
					block(views.stage)
				} catch (e: Throwable) {
					completedException = e
				} finally {
					completed = true
				}
			}
		})

		withTimeout(10.seconds) {
			while (!completed) {
				simulateFrame()
				dispatcher.executePending()
			}

			if (completedException != null) throw completedException!!
		}
	}

	private suspend fun simulateFrame(count: Int = 1) {
		repeat(count) {
			time += frameTime
			ag.onRender(ag)
            gameWindow.dispatch(RenderEvent())
			delay(frameTime)
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
