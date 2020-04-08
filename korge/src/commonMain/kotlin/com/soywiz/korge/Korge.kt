package com.soywiz.korge

import com.soywiz.klock.*
import com.soywiz.klogger.*
import com.soywiz.korag.*
import com.soywiz.korev.*
import com.soywiz.korge.input.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.logger.*
import com.soywiz.korge.resources.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.stat.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korim.vector.*
import com.soywiz.korinject.*
import com.soywiz.korio.async.*
import com.soywiz.korio.dynamic.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.reflect.*

/**
 * Entry point for games written in Korge.
 * You have to call the [Korge] method by either providing some parameters, or a [Korge.Config] object.
 */
object Korge {
	val logger = Logger("Korge")

	suspend fun setupCanvas(config: Config, context: CoroutineContext): SceneContainer {
		logger.trace { "Korge.setupCanvas[1]" }
		val injector = config.injector

		val eventDispatcher = config.eventDispatcher
		val ag = config.gameWindow!!.ag
		val size = config.module.size
		val moduleArgs = ModuleArgs(config.args)

		RegisteredImageFormats.register(*config.module.imageFormats.toTypedArray())

		logger.trace { "pre injector" }
		injector
			// Instances
			.mapInstance(ModuleArgs::class, moduleArgs)
			.mapInstance(TimeProvider::class, config.timeProvider)
			.mapInstance(CoroutineContext::class, context + Job())
			.mapInstance(Module::class, config.module)
			.mapInstance(AG::class, ag)
			.mapInstance(Config::class, config)
			// Singletons
			.mapSingleton(Stats::class) { Stats() }
			.mapSingleton(Input::class) { Input() }
			.mapSingleton(Views::class) { Views(get(), get(), get(), get(), get(), get(), get()) }
			.mapSingleton(ResourcesRoot::class) { ResourcesRoot() }
			// Prototypes
			.mapPrototype(EmptyScene::class) { EmptyScene() }

			.mapInstance(GameWindow::class, config.gameWindow)

		//println("FRAME: $frame, ${config.frame}")

		@Suppress("RemoveExplicitTypeArguments")

		//ag.onReady.await() // Already done by CanvasApplicationEx

		logger.trace { "Korge.setupCanvas[1c]. ag: $ag" }
		logger.trace { "Korge.setupCanvas[1d]. debug: ${config.debug}" }
		logger.trace { "Korge.setupCanvas[1e]. args: ${config.args.toList()}" }
		logger.trace { "Korge.setupCanvas[1f]. size: $size" }
		logger.trace { "Korge.setupCanvas[1g]" }
		val views = injector.get(Views::class)
		logger.trace { "Korge.setupCanvas[1h]" }
		val input = views.input
		logger.trace { "Korge.setupCanvas[1i]" }
		logger.trace { "Korge.setupCanvas[1j]" }
		views.debugViews = config.debug
		logger.trace { "Korge.setupCanvas[1k]" }
		config.constructedViews(views)
		logger.trace { "Korge.setupCanvas[1l]" }
		logger.trace { "Korge.setupCanvas[2]" }

		views.virtualWidth = size.width
		views.virtualHeight = size.height
		views.scaleAnchor = config.module.scaleAnchor
		views.scaleMode = config.module.scaleMode
		views.clipBorders = config.module.clipBorders

		logger.trace { "Korge.setupCanvas[3]" }

		logger.trace { "Korge.setupCanvas[4]" }
		config.module.apply {
			injector.configure()
		}

		logger.trace { "Korge.setupCanvas[5]" }

		prepareViews(views, eventDispatcher, config.module.clearEachFrame, config.module.bgcolor)

		logger.trace { "Korge.setupCanvas[7]" }

		views.targetFps = config.module.targetFps

		val sc = SceneContainer(views)
		views.stage += sc

		if (OS.isJsBrowser) {
			KDynamic { global["views"] = views }
		}

		sc.changeTo(config.sceneClass, *config.sceneInjects.toTypedArray(), time = 0.seconds)

		views.onClose {
			sc.changeTo<EmptyScene>()
		}

		logger.trace { "Korge.setupCanvas[8]" }

		return sc
	}

	fun prepareViews(
		views: Views,
		eventDispatcher: EventDispatcher,
		clearEachFrame: Boolean = true,
		bgcolor: RGBA = Colors.TRANSPARENT_BLACK,
		fixedSizeStep: TimeSpan = TimeSpan.NULL
	) {
		val input = views.input
		val ag = views.ag
		val downPos = Point()
		val upPos = Point()
		var downTime = DateTime.EPOCH
		var moveTime = DateTime.EPOCH
		var upTime = DateTime.EPOCH
		var moveMouseOutsideInNextFrame = false
		val mouseTouchId = -1

		//fun AGInput.GamepadEvent.copyTo(e: GamepadUpdatedEvent) { e.gamepad.copyFrom(this.gamepad) }
		//fun AGInput.GamepadEvent.copyTo(e: GamepadConnectionEvent) {e.gamepad.copyFrom(this.gamepad) }
		//// MOUSE
		//agInput.onMouseDown { e -> mouseDown("onMouseDown", e.x, e.y) }
		//agInput.onMouseUp { e -> mouseUp("onMouseUp", e.x, e.y) }
		//agInput.onMouseOver { e -> mouseMove("onMouseOver", e.x, e.y) }
		//agInput.onMouseDrag { e -> mouseDrag("onMouseDrag", e.x, e.y);  updateTouch(mouseTouchId, e.x, e.y, start = false, end = false) }
		////agInput.onMouseClick { e -> } // Triggered by mouseUp

		fun pixelRatio(): Double = ag.devicePixelRatio

		fun getRealX(x: Double, scaleCoords: Boolean) = if (scaleCoords) x * pixelRatio() else x
		fun getRealY(y: Double, scaleCoords: Boolean) = if (scaleCoords) y * pixelRatio() else y

		fun updateTouch(id: Int, x: Double, y: Double, start: Boolean, end: Boolean) {
			val touch = input.getTouch(id)
			val now = DateTime.now()

			touch.id = id
			touch.active = !end

			if (start) {
				touch.startTime = now
				touch.start.setTo(x, y)
			}

			touch.currentTime = now
			touch.current.setTo(x, y)

			input.updateTouches()
		}

		fun mouseDown(type: String, x: Double, y: Double) {
			views.input.mouseButtons = 1
			views.input.mouse.setTo(x, y)
			views.mouseUpdated()
			downPos.copyFrom(views.input.mouse)
			downTime = DateTime.now()
            views.input.mouseInside = true
		}

		fun mouseUp(type: String, x: Double, y: Double) {
			//Console.log("mouseUp: $name")
			views.input.mouseButtons = 0
			views.input.mouse.setTo(x, y)
			views.mouseUpdated()
			upPos.copyFrom(views.input.mouse)

			if (type == "onTouchEnd") {
				upTime = DateTime.now()
				if ((downTime - upTime) <= 40.milliseconds) {
					//Console.log("mouseClick: $name")
					views.dispatch(MouseEvent(MouseEvent.Type.CLICK))
				}
			}
		}

		fun mouseMove(type: String, x: Double, y: Double, inside: Boolean) {
			views.input.mouse.setTo(x, y)
            views.input.mouseInside = inside
            if (!inside) {
                moveMouseOutsideInNextFrame = true
            }
			views.mouseUpdated()
			moveTime = DateTime.now()
		}

		fun mouseDrag(type: String, x: Double, y: Double) {
			views.input.mouse.setTo(x, y)
			views.mouseUpdated()
			moveTime = DateTime.now()
		}

		eventDispatcher.addEventListener<MouseEvent> { e ->
			logger.trace { "eventDispatcher.addEventListener<MouseEvent>:$e" }
			val x = getRealX(e.x.toDouble(), e.scaleCoords)
			val y = getRealY(e.y.toDouble(), e.scaleCoords)
			when (e.type) {
				MouseEvent.Type.DOWN -> {
					mouseDown("mouseDown", x, y)
					updateTouch(mouseTouchId, x, y, start = true, end = false)
				}
				MouseEvent.Type.UP -> {
					mouseUp("mouseUp", x, y)
					updateTouch(mouseTouchId, x, y, start = false, end = true)
				}
				MouseEvent.Type.MOVE -> {
					mouseMove("mouseMove", x, y, inside = true)
				}
				MouseEvent.Type.DRAG -> {
					mouseDrag("onMouseDrag", x, y)
					updateTouch(mouseTouchId, x, y, start = false, end = false)
				}
				MouseEvent.Type.CLICK -> {
				}
				MouseEvent.Type.ENTER -> {
                    mouseMove("mouseEnter", x, y, inside = true)
				}
				MouseEvent.Type.EXIT -> {
					mouseMove("mouseExit", x, y, inside = false)
				}
			}
			views.dispatch(e)
		}

		eventDispatcher.addEventListener<KeyEvent> { e ->
			logger.trace { "eventDispatcher.addEventListener<KeyEvent>:$e" }
			views.dispatch(e)
		}

		eventDispatcher.addEventListener<ResumeEvent> { e -> views.dispatch(e) }
		eventDispatcher.addEventListener<PauseEvent> { e -> views.dispatch(e) }
		eventDispatcher.addEventListener<StopEvent> { e -> views.dispatch(e) }
		eventDispatcher.addEventListener<DestroyEvent> { e ->
			try {
				views.dispatch(e)
			} finally {
				launchImmediately(views.coroutineContext) {
					views.close()
				}
			}
		}


		// TOUCH
		fun touch(e: TouchEvent, start: Boolean, end: Boolean) {
			val t = e.touches.first()
			val x = t.current.x
			val y = t.current.y
			updateTouch(t.id, x, y, start, end)
			when {
				start -> {
					mouseDown("onTouchStart", x, y)
				}
				end -> {
					mouseUp("onTouchEnd", x, y)
					moveMouseOutsideInNextFrame = true
				}
				else -> {
					mouseMove("onTouchMove", x, y, inside = true)
				}
			}
		}

		eventDispatcher.addEventListener<TouchEvent> { e ->
			logger.trace { "eventDispatcher.addEventListener<TouchEvent>:$e" }
			val touch = e.touches.first()
			val ix = getRealX(touch.current.x, e.scaleCoords).toInt()
			val iy = getRealX(touch.current.y, e.scaleCoords).toInt()
			when (e.type) {
				TouchEvent.Type.START -> {
					touch(e, start = true, end = false)
					views.dispatch(MouseEvent(MouseEvent.Type.DOWN, 0, ix, iy, MouseButton.LEFT, 1))
				}
				TouchEvent.Type.MOVE -> {
					touch(e, start = false, end = false)
					views.dispatch(MouseEvent(MouseEvent.Type.DRAG, 0, ix, iy, MouseButton.LEFT, 1))
				}
				TouchEvent.Type.END -> {
					touch(e, start = false, end = true)
					views.dispatch(MouseEvent(MouseEvent.Type.UP, 0, ix, iy, MouseButton.LEFT, 0))
					//println("DISPATCH MouseEvent(MouseEvent.Type.UP)")
				}
			}
			views.dispatch(e)
		}

		fun gamepadUpdated(e: GamePadUpdateEvent) {
			e.gamepads.fastForEach { gamepad ->
				input.gamepads[gamepad.index].copyFrom(gamepad)
			}
			input.updateConnectedGamepads()
		}

		//agInput.onGamepadUpdate {
		//	gamepadUpdated(it.gamepad)
		//	it.copyTo(gamepadTypedEvent)
		//	views.dispatch(gamepadTypedEvent)
		//}

		eventDispatcher.addEventListener<GamePadConnectionEvent> { e ->
			logger.trace { "eventDispatcher.addEventListener<GamePadConnectionEvent>:$e" }
			//gamepadUpdated(it.gamepad)
			//it.copyTo(gamepadConnectionEvent)
			views.dispatch(e)
		}

		eventDispatcher.addEventListener<GamePadUpdateEvent> { e ->
			//it.copyTo(gamepadConnectionEvent)
			gamepadUpdated(e)
			views.dispatch(e)
		}



		eventDispatcher.addEventListener<ReshapeEvent> { e ->
			//println("Korge:eventDispatcher.addEventListener<ResizedEvent>:$e - backSize=(${ag.backWidth}, ${ag.backHeight})")
			views.resized(ag.backWidth, ag.backHeight)
		}

		//ag.onResized { e ->
		//	//println("ag.onResized:$e - backSize=(${ag.backWidth}, ${ag.backHeight}) :: ${agContainer.ag} :: frame=(${frame?.width}x${frame?.height})")
		//	//println("ag.onResized: ${ag.backWidth},${ag.backHeight}")
		//	views.resized(ag.backWidth, ag.backHeight)
		//}
		//ag.resized(initialWidth, initialHeight)


		//views.gameWindow.addEventListener<ReshapeEvent> {
		//	//println("ag.onResized:$e - backSize=(${ag.backWidth}, ${ag.backHeight}) :: ${agContainer.ag} :: frame=(${frame?.width}x${frame?.height})")
		//	//println("ag.onResized: ${ag.backWidth},${ag.backHeight}")
		//	views.resized(ag.backWidth, ag.backHeight)
		//}

		eventDispatcher.dispatch(ReshapeEvent(0, 0, views.nativeWidth, views.nativeHeight))

		//println("lastTime: $lastTime")
		views.clearEachFrame = clearEachFrame
		views.clearColor = bgcolor
		views.gameWindow.addEventListener<RenderEvent> {
			try {
				views.frameUpdateAndRender(fixedSizeStep = fixedSizeStep)

				if (moveMouseOutsideInNextFrame) {
					moveMouseOutsideInNextFrame = false
					//views.input.mouse.setTo(-1000, -1000)
                    views.input.mouseInside = false
					//views.dispatch(mouseMovedEvent)
					views.mouseUpdated()
				}
				//println("render:$delta,$adelta")
			} catch (e: Throwable) {
				e.printStackTrace()
			}
		}
		//ag.onRender {
		//}
	}

	//private suspend fun <T> setContext(context: CoroutineContext, callback: suspend () -> T): T = suspendCancellableCoroutine { c ->
	//	callback.startCoroutine(object : Continuation<T> {
	//		override val context: CoroutineContext = context
	//		override fun resumeWith(result: Result<T>) = c.resumeWith(result)
	//	})
	//}

	suspend fun KoruiWithLogger(gameWindow: GameWindow?, entry: suspend GameWindow.() -> Unit) {
		if (!OS.isJsBrowser) {
			configureLoggerFromProperties(localCurrentDirVfs["klogger.properties"])
		}
		try {
			(gameWindow ?: coroutineContext[GameWindow] ?: CreateDefaultGameWindow()).loop {
				entry()
			}
		} finally {
			println("Korge gameWindow.loop completed")
		}
	}

	// New Korge
	suspend operator fun invoke(
		title: String = "Korge",
		width: Int = DefaultViewport.WIDTH, height: Int = DefaultViewport.HEIGHT,
		virtualWidth: Int = width, virtualHeight: Int = height,
		icon: Bitmap? = null,
		quality: GameWindow.Quality = GameWindow.Quality.AUTOMATIC,
		targetFps: Double = 0.0,
		scaleAnchor: Anchor = Anchor.MIDDLE_CENTER,
		scaleMode: ScaleMode = ScaleMode.SHOW_ALL,
		clipBorders: Boolean = true,
		bgcolor: RGBA? = Colors.BLACK,
		debug: Boolean = false,
		fullscreen: Boolean? = null,
		args: Array<String> = arrayOf(),
		gameWindow: GameWindow? = null,
		entry: suspend Stage.() -> Unit
	) = KoruiWithLogger(gameWindow) {
		val gameWindow = this
		if (OS.isNative) println("Korui[0]")
		configure(width, height, title, icon, fullscreen)
		this.quality = quality
		if (OS.isNative) println("CanvasApplicationEx.IN[0]")
		val injector = AsyncInjector()
		val input = Input()
		val stats = Stats()
		val views = Views(coroutineContext + Job(), ag, injector, input, TimeProvider, stats, gameWindow)
		injector
			.mapInstance(views)
			.mapInstance(input)
			.mapInstance(stats)
			.mapInstance(Korge.ModuleArgs(args))
			.mapInstance<Module>(object : Module() {
				override val title = title
				override val fullscreen: Boolean? = fullscreen
				override val windowSize = SizeInt(width, height)
				override val size = SizeInt(virtualWidth, virtualHeight)
			})
		views.debugViews = debug
		views.virtualWidth = virtualWidth
		views.virtualHeight = virtualHeight
		views.scaleAnchor = scaleAnchor
		views.scaleMode = scaleMode
		views.clipBorders = clipBorders
		views.targetFps = targetFps
		Korge.prepareViews(views, gameWindow, bgcolor != null, bgcolor ?: Colors.TRANSPARENT_BLACK)
        views.launchImmediately {
            //println("coroutineContext: $coroutineContext")
            //println("GameWindow: ${coroutineContext[GameWindow]}")
            entry(views.stage)
        }
		if (OS.isNative) println("CanvasApplicationEx.IN[1]")
		if (OS.isNative) println("Korui[1]")
	}

	suspend operator fun invoke(config: Config) = KoruiWithLogger(config.gameWindow) {
		val module = config.module
		configure(module.windowSize.width, module.windowSize.height, module.title, null, config.fullscreen ?: config.module.fullscreen ?: false)
		val gameWindow = this
		logger.trace { "Korge.invoke(config)" }
		if (OS.isJvm) {
			logger.trace { "!!!! KORGE: if the main window doesn't appear and hangs, check that the VM option -XstartOnFirstThread is set" }
		}

		logger.trace { "Korge.test" }
		logger.trace { "Korge.test.checkEnvironment" }
		val done = CompletableDeferred<SceneContainer>(Job())
		logger.trace { "Korge.test without container" }
		logger.trace { "Korge.test loading icon" }
		try {
            // Do nothing
            when {
				module.iconImage != null -> this.icon = module.iconImage!!.render()
				module.icon != null -> this.icon = resourcesVfs[module.icon!!].readBitmapOptimized(config.imageFormats)
				else -> Unit
			}
		} catch (e: Throwable) {
			logger.error { "Couldn't get the application icon" }
			e.printStackTrace()
		}

		logger.trace { "Korge.test pre CanvasApplicationEx" }

		this.quality = module.quality

		logger.trace { "Korge.test [1]" }
		coroutineScope {
			launchImmediately {
				logger.trace { "Korge.test [2]" }
				done.complete(
					setupCanvas(config.copy(gameWindow = gameWindow, eventDispatcher = gameWindow), context = gameWindow.coroutineDispatcher)
				)
			}
		}
	}

	data class Config(
		val module: Module,
		val args: Array<String> = arrayOf(),
		val imageFormats: ImageFormat = RegisteredImageFormats,
		val gameWindow: GameWindow? = null,
		val eventDispatcher: EventDispatcher = gameWindow ?: DummyEventDispatcher,
		val sceneClass: KClass<out Scene> = module.mainScene,
		val sceneInjects: List<Any> = listOf(),
		val timeProvider: TimeProvider = TimeProvider,
		val injector: AsyncInjector = AsyncInjector(),
		val debug: Boolean = false,
		val trace: Boolean = false,
		val context: Any? = null,
		val fullscreen: Boolean? = null,
		val constructedViews: (Views) -> Unit = {}
	)

	data class ModuleArgs(val args: Array<String>)

	internal fun configureViews() {

	}
}
