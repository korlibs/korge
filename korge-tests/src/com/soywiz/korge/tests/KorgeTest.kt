package com.soywiz.korge.tests

import com.soywiz.korag.AG
import com.soywiz.korag.AGContainer
import com.soywiz.korag.AGInput
import com.soywiz.korge.Korge
import com.soywiz.korge.input.Input
import com.soywiz.korge.input.mouse
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korio.async.EventLoopTest
import com.soywiz.korio.async.sync
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.util.TimeProvider
import com.soywiz.korma.geom.Rectangle
import org.junit.Before

@Suppress("unused")
open class KorgeTest {
	val eventLoop = EventLoopTest()
	val injector: AsyncInjector = AsyncInjector()
	val ag: AG = DummyAG()
	val input: Input = Input()
	val views = Views(eventLoop, ag, injector, input)
	var testTime = 0L
	val canvas = DummyAGContainer(ag)

	fun syncTest(block: suspend EventLoopTest.() -> Unit): Unit {
		sync(el = eventLoop, step = 10, block = block)
	}

	@Before
	fun init() = syncTest {
		views.init()
	}

	@Suppress("UNCHECKED_CAST")
	fun <T : Scene> testScene(module: Module, sceneClass: Class<T>, vararg injects: Any, callback: suspend T.() -> Unit) = syncTest {
		//disableNativeImageLoading {
		val sc = Korge.test(Korge.Config(module, sceneClass = sceneClass, sceneInjects = injects.toList(), container = canvas, timeProvider = TimeProvider {
			//println("Requested Time: $testTime")
			testTime
		}))
		callback(sc.currentScene as T)
		//}
	}

	suspend fun Scene.updateTime(dtMs: Int = 20) {
		testTime += dtMs
		views.clampElapsedTimeTo = Int.MAX_VALUE
		//println("updateTime: $dtMs :: $testTime")
		//println("updateTime: $dtMs")
		ag.onRender(ag)
		eventLoop.sleepNextFrame()
		//views.update(dtMs)
	}

	suspend fun Scene.updateTimeSteps(time: Int, step: Int = 20) {
		var remainingTime = time
		while (remainingTime > 0) {
			val elapsed = Math.min(step, remainingTime)
			updateTime(elapsed)
			remainingTime -= elapsed
		}
	}

	suspend fun Scene.updateMousePosition(x: Int, y: Int) {
		canvas.agInput.mouseEvent.x = x
		canvas.agInput.mouseEvent.y = y
		canvas.agInput.onMouseOver(canvas.agInput.mouseEvent)
		updateTime(0)
	}

	suspend fun View.simulateClick() {
		this.mouse.onClick(this.mouse)
		ag.onRender(ag)
		eventLoop.sleepNextFrame()
	}

	suspend fun View.simulateOver() {
		this.mouse.onOver(this.mouse)
		ag.onRender(ag)
		eventLoop.sleepNextFrame()
	}

	suspend fun View.simulateOut() {
		this.mouse.onOut(this.mouse)
		ag.onRender(ag)
		eventLoop.sleepNextFrame()
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

	class DummyAG : AG() {
		override val nativeComponent: Any get() = Any()

		init {
			ready()
		}
	}

	class DummyAGContainer(override val ag: AG) : AGContainer {
		override val agInput: AGInput = AGInput()

		override fun repaint(): Unit {
			ag.onRender(ag)
		}
	}
}
