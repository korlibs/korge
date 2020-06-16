package com.soywiz.korge

import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.klock.hr.*
import com.soywiz.klogger.*
import com.soywiz.korag.*
import com.soywiz.korev.*
import com.soywiz.korge.input.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.logger.*
import com.soywiz.korge.resources.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.stat.*
import com.soywiz.korge.time.*
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

    suspend operator fun invoke(config: Config) {
        //println("Korge started from Config")
        Korge(
            title = config.module.title,
            width = config.module.windowSize.width,
            height = config.module.windowSize.height,
            virtualWidth = config.module.size.width,
            virtualHeight = config.module.size.height,
            bgcolor = config.module.bgcolor,
            quality = config.module.quality,
            icon = null,
            iconPath = config.module.icon,
            iconDrawable = config.module.iconImage,
            imageFormats = ImageFormats(config.module.imageFormats),
            targetFps = config.module.targetFps,
            scaleAnchor = config.module.scaleAnchor,
            scaleMode = config.module.scaleMode,
            clipBorders = config.module.clipBorders,
            debug = config.debug,
            fullscreen = config.module.fullscreen,
            args = config.args,
            gameWindow = config.gameWindow,
            injector = config.injector,
            timeProvider = config.timeProvider,
            entry = {
                //println("Korge views prepared for Config")
                RegisteredImageFormats.register(*config.module.imageFormats.toTypedArray())
                val injector = config.injector
                injector.mapInstance(Module::class, config.module).mapInstance(Config::class, config)
                config.constructedViews(views)
                config.module.apply { injector.configure() }
                val sc = SceneContainer(views, name = "rootSceneContainer")
                views.stage += sc
                sc.changeTo(config.sceneClass, *config.sceneInjects.toTypedArray(), time = 0.milliseconds)
                // Se we have the opportunity to execute deinitialization code at the scene level
                views.onClose { sc.changeTo<EmptyScene>() }
            }
        )
    }

    suspend operator fun invoke(
		title: String = "Korge",
		width: Int = DefaultViewport.WIDTH, height: Int = DefaultViewport.HEIGHT,
		virtualWidth: Int = width, virtualHeight: Int = height,
		icon: Bitmap? = null,
        iconPath: String? = null,
        iconDrawable: SizedDrawable? = null,
        imageFormats: ImageFormat = ImageFormats(PNG),
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
        timeProvider: HRTimeProvider = HRTimeProvider,
        injector: AsyncInjector = AsyncInjector(),
        entry: suspend Stage.() -> Unit
	) {
        if (!OS.isJsBrowser) {
            configureLoggerFromProperties(localCurrentDirVfs["klogger.properties"])
        }
        val realGameWindow = (gameWindow ?: coroutineContext[GameWindow] ?: CreateDefaultGameWindow())
        //println("Configure: ${width}x${height}")
        // @TODO: Configure should happen before loop. But we should ensure that all the korgw targets are ready for this
        //realGameWindow.configure(width, height, title, icon, fullscreen)
        realGameWindow.loop {
            val gameWindow = this
            if (OS.isNative) println("Korui[0]")
            realGameWindow.configure(width, height, title, icon, fullscreen)
            try {
                // Do nothing
                when {
                    iconDrawable != null -> this.icon = iconDrawable.render()
                    iconPath != null -> this.icon = resourcesVfs[iconPath!!].readBitmapOptimized(imageFormats)
                    else -> Unit
                }
            } catch (e: Throwable) {
                logger.error { "Couldn't get the application icon" }
                e.printStackTrace()
            }
            this.quality = quality
            if (OS.isNative) println("CanvasApplicationEx.IN[0]")
            val input = Input()
            val stats = Stats()

            // Use this once Korgw is on 1.12.5
            //val views = Views(gameWindow.getCoroutineDispatcherWithCurrentContext() + SupervisorJob(), ag, injector, input, timeProvider, stats, gameWindow)
            val views = Views(coroutineContext + gameWindow.coroutineDispatcher + SupervisorJob(), ag, injector, input, timeProvider, stats, gameWindow)

            if (OS.isJsBrowser) KDynamic { global["views"] = views }
            injector
                .mapInstance(AG::class, ag)
                .mapInstance(TimeProvider::class, timeProvider.toTimeProvider()) // Deprecated
                .mapInstance(HRTimeProvider::class, timeProvider)
                .mapInstance(views)
                .mapInstance(input)
                .mapInstance(stats)
                .mapInstance(ModuleArgs(args))
                .mapInstance(CoroutineContext::class, views.coroutineContext)
                .mapInstance(GameWindow::class, gameWindow)
                .mapSingleton(ResourcesRoot::class) { ResourcesRoot() }
                .mapPrototype(EmptyScene::class) { EmptyScene() }
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
            //Korge.prepareViews(views, gameWindow, bgcolor != null, bgcolor ?: Colors.TRANSPARENT_BLACK)

            prepareViews(views, gameWindow, bgcolor != null, bgcolor ?: Colors.TRANSPARENT_BLACK, waitForFirstRender = true)

            views.launchImmediately {
                coroutineScope {
                    //println("coroutineContext: $coroutineContext")
                    //println("GameWindow: ${coroutineContext[GameWindow]}")
                    entry(views.stage)
                    // @TODO: Do not complete to prevent job cancelation?
                    gameWindow.waitClose()
                }
            }
            if (OS.isNative) println("CanvasApplicationEx.IN[1]")
            if (OS.isNative) println("Korui[1]")

            // @TODO: Do not complete to prevent job cancelation?
            gameWindow.waitClose()
        }
    }

    suspend fun GameWindow.waitClose() {
        while (running) {
            delay(100.milliseconds)
        }
    }

    @KorgeInternal
    fun prepareViewsBase(
        views: Views,
        eventDispatcher: EventDispatcher,
        clearEachFrame: Boolean = true,
        bgcolor: RGBA = Colors.TRANSPARENT_BLACK,
        fixedSizeStep: TimeSpan = TimeSpan.NULL
    ): CompletableDeferred<Unit> {
        val input = views.input
        val ag = views.ag
        val downPos = Point()
        val upPos = Point()
        var downTime = DateTime.EPOCH
        var moveTime = DateTime.EPOCH
        var upTime = DateTime.EPOCH
        var moveMouseOutsideInNextFrame = false
        val mouseTouchId = -1

        // devicePixelRatio might change at runtime by changing the resolution or changing the screen of the window
        fun getRealX(x: Double, scaleCoords: Boolean) = if (scaleCoords) x * ag.devicePixelRatio else x
        fun getRealY(y: Double, scaleCoords: Boolean) = if (scaleCoords) y * ag.devicePixelRatio else y

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
            //println("MOUSE: $e")
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
                MouseEvent.Type.DRAG -> {
                    mouseDrag("onMouseDrag", x, y)
                    updateTouch(mouseTouchId, x, y, start = false, end = false)
                }
                MouseEvent.Type.MOVE -> mouseMove("mouseMove", x, y, inside = true)
                MouseEvent.Type.CLICK -> Unit
                MouseEvent.Type.ENTER -> mouseMove("mouseEnter", x, y, inside = true)
                MouseEvent.Type.EXIT -> mouseMove("mouseExit", x, y, inside = false)
                MouseEvent.Type.SCROLL -> Unit
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
                views.launchImmediately {
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

        eventDispatcher.addEventListener<GamePadConnectionEvent> { e ->
            logger.trace { "eventDispatcher.addEventListener<GamePadConnectionEvent>:$e" }
            views.dispatch(e)
        }

        eventDispatcher.addEventListener<GamePadUpdateEvent> { e ->
            gamepadUpdated(e)
            views.dispatch(e)
        }

        eventDispatcher.addEventListener<ReshapeEvent> { e ->
            //try { throw Exception() } catch (e: Throwable) { e.printStackTrace() }
            //println("eventDispatcher.addEventListener<ReshapeEvent>: ${ag.backWidth}x${ag.backHeight} : ${e.width}x${e.height}")
            views.resized(ag.backWidth, ag.backHeight)
        }

        //println("eventDispatcher.dispatch(ReshapeEvent(0, 0, views.nativeWidth, views.nativeHeight)) : ${views.nativeWidth}x${views.nativeHeight}")
        eventDispatcher.dispatch(ReshapeEvent(0, 0, views.nativeWidth, views.nativeHeight))

        var renderShown = false
        views.clearEachFrame = clearEachFrame
        views.clearColor = bgcolor
        val firstRenderDeferred = CompletableDeferred<Unit>()
        views.gameWindow.addEventListener<RenderEvent> {
            if (!renderShown) {
                //println("!!!!!!!!!!!!! views.gameWindow.addEventListener<RenderEvent>")
                renderShown = true
                firstRenderDeferred.complete(Unit)
            }
            try {
                views.frameUpdateAndRender(fixedSizeStep = fixedSizeStep)

                if (moveMouseOutsideInNextFrame) {
                    moveMouseOutsideInNextFrame = false
                    views.input.mouseInside = false
                    views.mouseUpdated()
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        return firstRenderDeferred
    }

    @KorgeInternal
    suspend fun prepareViews(
        views: Views,
        eventDispatcher: EventDispatcher,
        clearEachFrame: Boolean = true,
        bgcolor: RGBA = Colors.TRANSPARENT_BLACK,
        fixedSizeStep: TimeSpan = TimeSpan.NULL,
        waitForFirstRender: Boolean = true
    ) {
       val firstRenderDeferred = prepareViewsBase(views, eventDispatcher, clearEachFrame, bgcolor, fixedSizeStep)
        if (waitForFirstRender) {
            firstRenderDeferred.await()
        }
    }

	data class Config(
		val module: Module,
		val args: Array<String> = arrayOf(),
		val imageFormats: ImageFormat = RegisteredImageFormats,
		val gameWindow: GameWindow? = null,
		//val eventDispatcher: EventDispatcher = gameWindow ?: DummyEventDispatcher, // Removed
		val sceneClass: KClass<out Scene> = module.mainScene,
		val sceneInjects: List<Any> = listOf(),
		val timeProvider: HRTimeProvider = HRTimeProvider,
		val injector: AsyncInjector = AsyncInjector(),
		val debug: Boolean = false,
		val trace: Boolean = false,
		val context: Any? = null,
		val fullscreen: Boolean? = null,
		val constructedViews: (Views) -> Unit = {}
	)

	data class ModuleArgs(val args: Array<String>)
}
