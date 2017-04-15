package com.soywiz.korge.tests

import com.soywiz.korag.AG
import com.soywiz.korag.AGContainer
import com.soywiz.korge.Korge
import com.soywiz.korge.input.Input
import com.soywiz.korge.input.mouse
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korim.format.disableNativeImageLoading
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.Signal
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.util.TimeProvider

@Suppress("unused")
open class KorgeTest {
	val injector: AsyncInjector = AsyncInjector()
	val ag: AG = DummyAG()
	val input: Input = Input()
	val views = Views(ag, injector, input)
	var testTime = 0L
	val canvas = DummyAGContainer(ag)

	@Suppress("UNCHECKED_CAST")
	fun <T : Scene> testScene(module: Module, sceneClass: Class<T>, callback: suspend T.() -> Unit) = syncTest {
		disableNativeImageLoading {
			val sc = Korge.test(module, sceneClass = sceneClass, canvas = canvas, timeProvider = TimeProvider {
				//println("Requested Time: $testTime")
				testTime
			})
			callback(sc.currentScene as T)
		}
	}

	suspend fun Scene.updateTime(dtMs: Int = 20) {
		testTime += dtMs
		//println("updateTime: $dtMs :: $testTime")
		//println("updateTime: $dtMs")
		ag.onRender(ag)
		EventLoop.sleepNextFrame()
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
		canvas.mouseX = x
		canvas.mouseY = y
		canvas.onMouseOver(Unit)
		updateTime(0)
	}

	suspend fun View.simulateClick() {
		this.mouse.onClick(this.mouse)
		ag.onRender(ag)
		EventLoop.sleepNextFrame()
	}

	class DummyAG : AG() {
		override val nativeComponent: Any get() = Any()

		init {
			ready()
		}
	}

	class DummyAGContainer(override val ag: AG) : AGContainer {
		override var mouseX: Int = 0
		override var mouseY: Int = 0
		override val onMouseOver: Signal<Unit> = Signal()
		override val onMouseUp: Signal<Unit> = Signal()
		override val onMouseDown: Signal<Unit> = Signal()

		override fun repaint(): Unit {
		}
	}
}
