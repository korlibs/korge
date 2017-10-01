package com.soywiz.korge

import com.soywiz.korag.AG
import com.soywiz.korag.AGContainer
import com.soywiz.korag.AGInput
import com.soywiz.korge.input.Input
import com.soywiz.korge.input.Keys
import com.soywiz.korge.plugin.KorgePlugins
import com.soywiz.korge.plugin.defaultKorgePlugins
import com.soywiz.korge.resources.ResourcesRoot
import com.soywiz.korge.scene.*
import com.soywiz.korge.time.seconds
import com.soywiz.korge.view.*
import com.soywiz.korim.NativeImageSpecialReader
import com.soywiz.korim.format.NativeImageFormatProvider
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.Promise
import com.soywiz.korio.async.eventLoopFactoryDefaultImpl
import com.soywiz.korio.async.go
import com.soywiz.korio.coroutine.withCoroutineContext
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.lang.printStackTrace
import com.soywiz.korio.util.TimeProvider
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korio.vfs.register
import com.soywiz.korio.vfs.registerVfsSpecialReader
import com.soywiz.korma.geom.Point2d
import com.soywiz.korui.CanvasApplication
import com.soywiz.korui.KoruiEventLoop
import kotlin.math.min
import kotlin.reflect.KClass

object Korge {
	val VERSION = "0.12.0"

	suspend fun setupCanvas(config: Config): SceneContainer {
		if (config.trace) println("Korge.setupCanvas[1]")
		val injector = config.injector

		injector
			.mapSingleton { Views(get(), get(), get(), get(), get()) }
			.mapSingleton { Input() }
			.mapInstance<KorgePlugins>(defaultKorgePlugins)
			.mapPrototype { EmptyScene() }
			.mapSingleton { ResourcesRoot() }

		val container = config.container!!
		val ag = container.ag
		val size = config.module.size
		NativeImageSpecialReader.instance.register()
		injector.mapInstance<AG>(ag)
		if (config.trace) println("Korge.setupCanvas[1b]. EventLoop: ${config.eventLoop}")
		if (config.trace) println("Korge.setupCanvas[1c]. ag: $ag")
		if (config.trace) println("Korge.setupCanvas[1d]. debug: ${config.debug}")
		if (config.trace) println("Korge.setupCanvas[1e]. args: ${config.args.toList()}")
		if (config.trace) println("Korge.setupCanvas[1f]. size: $size")
		injector.mapInstance<EventLoop>(config.eventLoop)
		val views = injector.get<Views>()
		views.debugViews = config.debug
		config.constructedViews(views)
		val moduleArgs = ModuleArgs(config.args)
		if (config.trace) println("Korge.setupCanvas[2]")

		views.virtualWidth = size.width
		views.virtualHeight = size.height

		if (config.trace) println("Korge.setupCanvas[3]")
		ag.onReady.await()

		if (config.trace) println("Korge.setupCanvas[4]")
		injector.mapInstance(moduleArgs)
		injector.mapInstance(config.timeProvider)
		injector.mapInstance<Module>(config.module)
		config.module.init(injector)

		if (config.trace) println("Korge.setupCanvas[5]")

		val downPos = Point2d()
		val upPos = Point2d()

		fun updateMousePos() {
			val mouseX = container.agInput.mouseX.toDouble()
			val mouseY = container.agInput.mouseY.toDouble()
			//println("updateMousePos: $mouseX, $mouseY")
			views.input.mouse.setTo(mouseX, mouseY)
			views.mouseUpdated()
		}

		fun updateTouchPos() {
			val mouseX = container.agInput.touchEvent.x.toDouble()
			val mouseY = container.agInput.touchEvent.y.toDouble()
			//println("updateMousePos: $mouseX, $mouseY")
			views.input.mouse.setTo(mouseX, mouseY)
			views.mouseUpdated()
		}

		if (config.trace) println("Korge.setupCanvas[6]")

		val mouseMovedEvent = MouseMovedEvent()
		val mouseUpEvent = MouseUpEvent()
		val mouseClickEvent = MouseClickEvent()
		val mouseDownEvent = MouseDownEvent()

		val keyDownEvent = KeyDownEvent()
		val keyUpEvent = KeyUpEvent()
		val keyTypedEvent = KeyTypedEvent()

		fun AGInput.KeyEvent.copyTo(e: KeyEvent) {
			e.keyCode = this.keyCode
		}

		// MOUSE
		container.agInput.onMouseDown {
			views.input.mouseButtons = 1
			updateMousePos()
			downPos.copyFrom(views.input.mouse)
			views.dispatch(mouseDownEvent)
		}
		container.agInput.onMouseUp {
			views.input.mouseButtons = 0
			updateMousePos()
			upPos.copyFrom(views.input.mouse)
			views.dispatch(mouseUpEvent)
		}
		container.agInput.onMouseOver {
			updateMousePos()
			views.dispatch(mouseMovedEvent)
		}
		container.agInput.onMouseClick {
			updateMousePos()
			views.dispatch(mouseClickEvent)
		}

		// TOUCH
		var moveMouseOutsideInNextFrame = false
		container.agInput.onTouchStart {
			views.input.mouseButtons = 1
			updateTouchPos()
			downPos.copyFrom(views.input.mouse)
			views.dispatch(mouseDownEvent)
		}
		container.agInput.onTouchEnd {
			views.input.mouseButtons = 0
			updateTouchPos()
			upPos.copyFrom(views.input.mouse)
			views.dispatch(mouseUpEvent)

			moveMouseOutsideInNextFrame = true
		}
		container.agInput.onTouchMove {
			updateTouchPos()
			views.dispatch(mouseMovedEvent)
		}

		// KEYS
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

			// DEBUG!
			if (it.keyCode == Keys.F12) {
				views.debugViews = !views.debugViews
			}
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

		var lastTime = config.timeProvider.currentTimeMillis()
		//println("lastTime: $lastTime")
		ag.onRender {
			if (config.trace) println("ag.onRender")
			//println("Render")
			val currentTime = config.timeProvider.currentTimeMillis()
			//println("currentTime: $currentTime")
			val delta = (currentTime - lastTime).toInt()
			val adelta = min(delta, views.clampElapsedTimeTo)
			//println("delta: $delta")
			//println("Render($lastTime -> $currentTime): $delta")
			lastTime = currentTime
			views.update(adelta)
			views.render(clear = config.module.clearEachFrame && views.clearEachFrame, clearColor = config.module.bgcolor)

			//println("Dumping views:")
			//views.dump()

			if (moveMouseOutsideInNextFrame) {
				moveMouseOutsideInNextFrame = false
				views.input.mouse.setTo(-1000, -1000)
				views.dispatch(mouseMovedEvent)
				views.mouseUpdated()
			}
			//println("render:$delta,$adelta")
		}

		if (config.trace) println("Korge.setupCanvas[7]")

		views.animationFrameLoop {
			if (config.trace) println("views.animationFrameLoop")
			//ag.resized()
			config.container.repaint()
		}

		val sc = views.sceneContainer()
		views.stage += sc
		sc.changeTo(config.sceneClass, *config.sceneInjects.toTypedArray(), time = 0.seconds)

		if (config.trace) println("Korge.setupCanvas[8]")

		return sc
	}

	operator fun invoke(
		module: Module,
		args: Array<String> = arrayOf(),
		container: AGContainer? = null,
		sceneClass: KClass<out Scene> = module.mainScene,
		sceneInjects: List<Any> = listOf(),
		timeProvider: TimeProvider = TimeProvider(),
		injector: AsyncInjector = AsyncInjector(),
		debug: Boolean = false,
		trace: Boolean = false,
		constructedViews: (Views) -> Unit = {},
		eventLoop: EventLoop = KoruiEventLoop.instance
	) = EventLoop.main(eventLoop) {
		test(Config(
			module = module, args = args, container = container, sceneClass = sceneClass, sceneInjects = sceneInjects, injector = injector,
			timeProvider = timeProvider, debug = debug, trace = trace, constructedViews = constructedViews
		))
	}

	data class Config(
		val module: Module,
		val args: Array<String> = arrayOf(),
		val container: AGContainer? = null,
		val sceneClass: KClass<out Scene> = module.mainScene,
		val sceneInjects: List<Any> = listOf(),
		val timeProvider: TimeProvider = TimeProvider(),
		val injector: AsyncInjector = AsyncInjector(),
		val debug: Boolean = false,
		val trace: Boolean = false,
		val constructedViews: (Views) -> Unit = {},
		val eventLoop: EventLoop = KoruiEventLoop.instance
	)

	suspend fun test(config: Config): SceneContainer = withCoroutineContext {
		val done = Promise.Deferred<SceneContainer>()
		if (config.container != null) {
			done.resolve(setupCanvas(config))

		} else {
			val icon = if (config.module.icon != null) {
				try {
					ResourcesVfs[config.module.icon!!].readBitmap()
				} catch (e: Throwable) {
					e.printStackTrace()
					null
				}
			} else {
				null
			}

			CanvasApplication(config.module.title, config.module.windowSize.width, config.module.windowSize.height, icon) {
				go {
					done.resolve(setupCanvas(config.copy(container = it)))
				}
			}
		}
		return@withCoroutineContext done.promise.await()
	}

	data class ModuleArgs(val args: Array<String>)
}
