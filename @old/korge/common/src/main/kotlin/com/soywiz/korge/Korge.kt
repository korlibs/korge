package com.soywiz.korge

import com.soywiz.klock.Klock
import com.soywiz.klogger.Logger
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
import com.soywiz.korim.format.readBitmap
import com.soywiz.korim.vector.render
import com.soywiz.korinject.AsyncInjector
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.Promise
import com.soywiz.korio.async.go
import com.soywiz.korio.lang.Console
import com.soywiz.korio.lang.printStackTrace
import com.soywiz.korio.time.TimeProvider
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korio.vfs.register
import com.soywiz.korma.geom.Point2d
import com.soywiz.korui.CanvasApplicationEx
import com.soywiz.korui.KoruiEventLoop
import com.soywiz.korui.ui.Frame
import kotlin.math.min
import kotlin.reflect.KClass

object Korge {
	val VERSION = KORGE_VERSION

	val logger = Logger("Korge")

	suspend fun setupCanvas(config: Config): SceneContainer {
		if (config.trace) println("Korge.setupCanvas[1]")
		val injector = config.injector

		val container = config.container!!
		val agInput = container.agInput
		val ag = container.ag
		val size = config.module.size

		injector
			.mapSingleton(Views::class) { Views(get(), get(), get(), get(), get()) }
			.mapSingleton(Input::class) { Input() }
			.mapInstance(KorgePlugins::class, defaultKorgePlugins)
			.mapInstance(Config::class, config)
			.mapInstance(AGContainer::class, container)
			.mapInstance(AG::class, ag)
			.mapPrototype(EmptyScene::class) { EmptyScene() }
			.mapSingleton(ResourcesRoot::class) { ResourcesRoot() }

		if (config.frame != null) {
			injector.mapInstance(Frame::class, config.frame)
		}

		// Register module plugins
		for (plugin in config.module.plugins) {
			defaultKorgePlugins.register(plugin)
		}

		NativeImageSpecialReader.instance.register()
		injector.mapInstance(AG::class, ag)
		if (config.trace) println("Korge.setupCanvas[1b]. EventLoop: ${config.eventLoop}")
		if (config.trace) println("Korge.setupCanvas[1c]. ag: $ag")
		if (config.trace) println("Korge.setupCanvas[1d]. debug: ${config.debug}")
		if (config.trace) println("Korge.setupCanvas[1e]. args: ${config.args.toList()}")
		if (config.trace) println("Korge.setupCanvas[1f]. size: $size")
		injector.mapInstance(EventLoop::class, config.eventLoop)
		val views = injector.get(Views::class)
		views.debugViews = config.debug
		config.constructedViews(views)
		val moduleArgs = ModuleArgs(config.args)
		if (config.trace) println("Korge.setupCanvas[2]")

		views.virtualWidth = size.width
		views.virtualHeight = size.height

		// Inject all modules
		for (plugin in defaultKorgePlugins.plugins) {
			plugin.register(views)
		}

		if (config.trace) println("Korge.setupCanvas[3]")
		ag.onReady.await()

		if (config.trace) println("Korge.setupCanvas[4]")
		injector.mapInstance(ModuleArgs::class, moduleArgs)
		injector.mapInstance(TimeProvider::class, config.timeProvider)
		injector.mapInstance<Module>(Module::class, config.module)
		config.module.init(injector)

		if (config.trace) println("Korge.setupCanvas[5]")

		val downPos = Point2d()
		val upPos = Point2d()
		val mouseMovedEvent = MouseMovedEvent()
		val mouseUpEvent = MouseUpEvent()
		val mouseClickEvent = MouseClickEvent()
		val mouseDownEvent = MouseDownEvent()

		val keyDownEvent = KeyDownEvent()
		val keyUpEvent = KeyUpEvent()
		val keyTypedEvent = KeyTypedEvent()
		var downTime = 0.0
		var moveTime = 0.0
		var upTime = 0.0

		fun mouseDown(name: String, x: Int, y: Int) {
			//Console.log("mouseDown: $name")
			views.input.mouseButtons = 1
			views.input.mouse.setTo(x * ag.pixelDensity, y * ag.pixelDensity)
			views.mouseUpdated()
			downPos.copyFrom(views.input.mouse)
			views.dispatch(mouseDownEvent)
			downTime = Klock.currentTimeMillisDouble()
		}

		fun mouseMove(name: String, x: Int, y: Int) {
			//Console.log("mouseMove: $name")
			views.input.mouse.setTo(x * ag.pixelDensity, y * ag.pixelDensity)
			views.mouseUpdated()
			views.dispatch(mouseMovedEvent)
			moveTime = Klock.currentTimeMillisDouble()
		}

		fun mouseUp(name: String, x: Int, y: Int) {
			//Console.log("mouseUp: $name")
			views.input.mouseButtons = 0
			views.input.mouse.setTo(x * ag.pixelDensity, y * ag.pixelDensity)
			views.mouseUpdated()
			upPos.copyFrom(views.input.mouse)
			views.dispatch(mouseUpEvent)
			upTime = Klock.currentTimeMillisDouble()
			if ((downTime - upTime) <= 40.0) {
				//Console.log("mouseClick: $name")
				views.dispatch(mouseClickEvent)
			}
		}

		fun AGInput.KeyEvent.copyTo(e: KeyEvent) {
			e.keyCode = this.keyCode
		}

		// MOUSE
		agInput.onMouseDown { e -> mouseDown("onMouseDown", e.x, e.y) }
		agInput.onMouseUp { e -> mouseUp("onMouseUp", e.x, e.y) }
		agInput.onMouseOver { e -> mouseMove("onMouseOver", e.x, e.y) }
		//agInput.onMouseClick { e -> } // Triggered by mouseUp

		// TOUCH
		var moveMouseOutsideInNextFrame = false
		agInput.onTouchStart { e -> mouseDown("onTouchStart", e.x, e.y) }
		agInput.onTouchEnd { e -> mouseUp("onTouchEnd", e.x, e.y); moveMouseOutsideInNextFrame = true }
		agInput.onTouchMove { e -> mouseMove("onTouchMove", e.x, e.y) }

		// KEYS
		agInput.onKeyDown {
			views.input.setKey(it.keyCode, true)
			//println("onKeyDown: $it")
			it.copyTo(keyDownEvent)
			views.dispatch(keyDownEvent)
		}
		agInput.onKeyUp {
			views.input.setKey(it.keyCode, false)
			//println("onKeyUp: $it")
			it.copyTo(keyUpEvent)
			views.dispatch(keyUpEvent)

			// DEBUG!
			if (it.keyCode == Keys.F12) {
				views.debugViews = !views.debugViews
			}
		}
		agInput.onKeyTyped {
			//println("onKeyTyped: $it")
			it.copyTo(keyTypedEvent)
			views.dispatch(keyTypedEvent)
		}

		ag.onResized {
			//println("ag.onResized: ${ag.backWidth},${ag.backHeight}")
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
		val frame: Frame? = null,
		val sceneClass: KClass<out Scene> = module.mainScene,
		val sceneInjects: List<Any> = listOf(),
		val timeProvider: TimeProvider = TimeProvider(),
		val injector: AsyncInjector = AsyncInjector(),
		val debug: Boolean = false,
		val trace: Boolean = false,
		val constructedViews: (Views) -> Unit = {},
		val eventLoop: EventLoop = KoruiEventLoop.instance
	)

	suspend fun test(config: Config): SceneContainer {
		val done = Promise.Deferred<SceneContainer>()
		if (config.container != null) {
			done.resolve(setupCanvas(config))

		} else {
			val module = config.module
			val icon = try {
				when {
					module.iconImage != null -> {
						module.iconImage!!.render()
					}
					module.icon != null -> {
						ResourcesVfs[module.icon!!].readBitmap()
					}
					else -> {
						null
					}
				}
			} catch (e: Throwable) {
				logger.error { "Couldn't get the application icon" }
				e.printStackTrace()
				null
			}
			CanvasApplicationEx(config.module.title, config.module.windowSize.width, config.module.windowSize.height, icon) { container, frame ->
				go {
					done.resolve(setupCanvas(config.copy(container = container, frame = frame)))
				}
			}
		}
		return done.promise.await()
	}

	data class ModuleArgs(val args: Array<String>)
}
