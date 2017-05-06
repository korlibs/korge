package com.soywiz.korge

import com.soywiz.korag.AG
import com.soywiz.korag.AGContainer
import com.soywiz.korag.AGInput
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.scene.SceneContainer
import com.soywiz.korge.scene.sceneContainer
import com.soywiz.korge.time.seconds
import com.soywiz.korge.view.*
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.async.*
import com.soywiz.korio.coroutine.withCoroutineContext
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.util.TimeProvider
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korma.geom.Point2d
import com.soywiz.korui.CanvasApplication

object Korge {
	val VERSION = "0.9.1"

	suspend fun setupCanvas(
		container: AGContainer,
		module: Module,
		eventLoop: EventLoop,
		args: Array<String> = arrayOf(),
		sceneClass: Class<out Scene> = module.mainScene,
		timeProvider: TimeProvider = TimeProvider(),
		injector: AsyncInjector = AsyncInjector(),
		constructedViews: (Views) -> Unit = {},
		trace: Boolean = false
	): SceneContainer {
		if (trace) println("Korge.setupCanvas[1]")
		val ag = container.ag
		injector.mapTyped<AG>(ag)
		injector.mapTyped<EventLoop>(eventLoop)
		val views = injector.get<Views>()
		constructedViews(views)
		val moduleArgs = ModuleArgs(args)
		if (trace) println("Korge.setupCanvas[2]")

		views.virtualWidth = module.virtualWidth
		views.virtualHeight = module.virtualHeight

		if (trace) println("Korge.setupCanvas[3]")
		ag.onReady.await()

		if (trace) println("Korge.setupCanvas[4]")
		injector.mapTyped(moduleArgs)
		injector.mapTyped(timeProvider)
		injector.mapTyped<Module>(module)
		module.init(injector)

		if (trace) println("Korge.setupCanvas[5]")

		val downPos = Point2d()
		val upPos = Point2d()

		fun updateMousePos() {
			val mouseX = container.agInput.mouseX.toDouble()
			val mouseY = container.agInput.mouseY.toDouble()
			//println("updateMousePos: $mouseX, $mouseY")
			views.input.mouse.setTo(mouseX, mouseY)
			views.mouseUpdated()
		}

		if (trace) println("Korge.setupCanvas[6]")

		val mouseMovedEvent = MouseMovedEvent()
		val mouseUpEvent = MouseUpEvent()
		val mouseDownEvent = MouseDownEvent()

		val keyDownEvent = KeyDownEvent()
		val keyUpEvent = KeyUpEvent()
		val keyTypedEvent = KeyTypedEvent()

		fun AGInput.KeyEvent.copyTo(e: KeyEvent) {
			e.keyCode = this.keyCode
		}

		container.agInput.onMouseOver {
			updateMousePos()
			views.dispatch(mouseMovedEvent)
		}
		container.agInput.onMouseUp {
			views.input.mouseButtons = 0
			updateMousePos()
			upPos.copyFrom(views.input.mouse)
			//if (upPos.distanceTo(downPos) < 10) {
			//	views.input.frame.clicked = true
			//}
			views.dispatch(mouseUpEvent)
		}
		container.agInput.onMouseDown {
			views.input.mouseButtons = 1
			updateMousePos()
			downPos.copyFrom(views.input.mouse)
			views.dispatch(mouseDownEvent)
		}
		container.agInput.onKeyDown {
			views.input.setKey(it.keyCode, true)
			//println("onKeyDown: $it")
			it.copyTo(keyDownEvent)
			views.dispatch(keyDownEvent)
		}
		container.agInput.onKeyUp {
			views.input.setKey(it.keyCode, false)
			//println("onKeyUp: $it")
			it.copyTo(keyUpEvent)
			views.dispatch(keyUpEvent)
		}
		container.agInput.onKeyTyped {
			//println("onKeyTyped: $it")
			it.copyTo(keyTypedEvent)
			views.dispatch(keyTypedEvent)
		}
		ag.onResized {
			views.resized(ag.backWidth, ag.backHeight)
		}
		ag.resized()

		var lastTime = timeProvider.currentTimeMillis()
		//println("lastTime: $lastTime")
		ag.onRender {
			//println("Render")
			ag.clear(module.bgcolor, stencil = 0, clearStencil = true)
			val currentTime = timeProvider.currentTimeMillis()
			//println("currentTime: $currentTime")
			val delta = (currentTime - lastTime).toInt()
			val adelta = Math.min(delta, views.clampElapsedTimeTo)
			//println("delta: $delta")
			//println("Render($lastTime -> $currentTime): $delta")
			lastTime = currentTime
			views.update(adelta)
			views.render()
			//println("render:$delta,$adelta")
		}

		if (trace) println("Korge.setupCanvas[7]")

		views.animationFrameLoop {
			//ag.resized()
			container.repaint()
		}

		val sc = views.sceneContainer()
		views.stage += sc
		sc.changeTo(sceneClass, time = 0.seconds)

		if (trace) println("Korge.setupCanvas[8]")

		return sc
	}

	operator fun invoke(
		module: Module,
		args: Array<String> = arrayOf(),
		canvas: AGContainer? = null,
		sceneClass: Class<out Scene> = module.mainScene,
		timeProvider: TimeProvider = TimeProvider(),
		injector: AsyncInjector = AsyncInjector(),
		debug: Boolean = false,
		trace: Boolean = false,
		constructedViews: (Views) -> Unit = {},
		eventLoop: EventLoop = eventLoopFactoryDefaultImpl.createEventLoop()
	) = EventLoop.main(eventLoop) {
		test(
			module = module, args = args, canvas = canvas, sceneClass = sceneClass, injector = injector,
			timeProvider = timeProvider, debug = debug, trace = trace, constructedViews = constructedViews
		)
	}

	suspend fun test(
		module: Module,
		args: Array<String> = arrayOf(),
		canvas: AGContainer? = null,
		sceneClass: Class<out Scene> = module.mainScene,
		injector: AsyncInjector = AsyncInjector(),
		timeProvider: TimeProvider = TimeProvider(),
		debug: Boolean = false,
		constructedViews: (Views) -> Unit = {},
		trace: Boolean = false
	): SceneContainer = withCoroutineContext {
		val done = Promise.Deferred<SceneContainer>()
		if (canvas != null) {
			done.resolve(setupCanvas(
				container = canvas, module = module, args = args,
				sceneClass = sceneClass, timeProvider = timeProvider,
				injector = injector, trace = trace, eventLoop = eventLoop,
				constructedViews = constructedViews
			))

		} else {
			val icon = if (module.icon != null) {
				try {
					ResourcesVfs[module.icon!!].readBitmap()
				} catch (e: Throwable) {
					e.printStackTrace()
					null
				}
			} else {
				null
			}

			CanvasApplication(module.title, module.width, module.height, icon) {
				go {
					done.resolve(setupCanvas(
						container = it, module = module, args = args, sceneClass = sceneClass,
						timeProvider = timeProvider, injector = injector, eventLoop = eventLoop,
						constructedViews = constructedViews
					))
				}
			}
		}
		return@withCoroutineContext done.promise.await()
	}

	data class ModuleArgs(val args: Array<String>)
}
