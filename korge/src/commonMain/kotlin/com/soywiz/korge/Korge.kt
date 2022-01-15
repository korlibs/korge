package com.soywiz.korge

import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.klogger.*
import com.soywiz.korag.log.*
import com.soywiz.korau.sound.*
import com.soywiz.korev.*
import com.soywiz.korge.input.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.logger.*
import com.soywiz.korge.render.*
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
import com.soywiz.korio.resources.*
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
    val DEFAULT_GAME_ID = "com.soywiz.korge.unknown"

    suspend operator fun invoke(config: Config) {
        //println("Korge started from Config")
        val module = config.module
        val windowSize = module.windowSize

        Korge(
            title = config.title ?: module.title,
            width = config.windowSize?.width ?: windowSize.width,
            height = config.windowSize?.height ?: windowSize.height,
            virtualWidth = config.virtualSize?.width ?: module.size.width,
            virtualHeight = config.virtualSize?.height ?: module.size.height,
            bgcolor = config.bgcolor ?: module.bgcolor,
            quality = config.quality ?: module.quality,
            icon = null,
            iconPath = config.icon ?: module.icon,
            //iconDrawable = module.iconImage,
            imageFormats = ImageFormats(config.imageFormats + module.imageFormats),
            targetFps = module.targetFps,
            scaleAnchor = config.scaleAnchor ?: module.scaleAnchor,
            scaleMode = config.scaleMode ?: module.scaleMode,
            clipBorders = config.clipBorders ?: module.clipBorders,
            debug = config.debug,
            fullscreen = config.fullscreen ?: module.fullscreen,
            args = config.args,
            gameWindow = config.gameWindow,
            injector = config.injector,
            timeProvider = config.timeProvider,
            blocking = config.blocking,
            gameId = config.gameId,
            settingsFolder = config.settingsFolder,
            batchMaxQuads = config.batchMaxQuads,
            entry = {
                //println("Korge views prepared for Config")
                RegisteredImageFormats.register(*module.imageFormats.toTypedArray())
                val injector = config.injector
                injector.mapInstance(Module::class, module)
                injector.mapInstance(Config::class, config)

                config.constructedViews(views)

                module.apply { injector.configure() }

                when {
                    config.main != null -> {
                        config.main?.invoke(stage)
                    }
                    config.sceneClass != null -> {
                        val sc = SceneContainer(views, name = "rootSceneContainer")
                        views.stage += sc
                        sc.changeTo(config.sceneClass, *config.sceneInjects.toTypedArray(), time = 0.milliseconds)
                        // Se we have the opportunity to execute deinitialization code at the scene level
                        views.onClose { sc.changeTo<EmptyScene>() }
                    }
                }
            }
        )
    }

    suspend operator fun invoke(
        title: String = "Korge",
        width: Int = DefaultViewport.WIDTH, height: Int = DefaultViewport.HEIGHT,
        virtualWidth: Int = width, virtualHeight: Int = height,
        icon: Bitmap? = null,
        iconPath: String? = null,
        //iconDrawable: SizedDrawable? = null,
        imageFormats: ImageFormat = ImageFormats(),
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
        timeProvider: TimeProvider = TimeProvider,
        injector: AsyncInjector = AsyncInjector(),
        debugAg: Boolean = false,
        blocking: Boolean = true,
        gameId: String = DEFAULT_GAME_ID,
        settingsFolder: String? = null,
        batchMaxQuads: Int = BatchBuilder2D.DEFAULT_BATCH_QUADS,
        entry: @ViewDslMarker suspend Stage.() -> Unit
	) {
        if (!OS.isJsBrowser) {
            configureLoggerFromProperties(localCurrentDirVfs["klogger.properties"])
        }
        val realGameWindow = (gameWindow ?: coroutineContext[GameWindow] ?: CreateDefaultGameWindow())
        realGameWindow.bgcolor = bgcolor ?: Colors.BLACK
        //println("Configure: ${width}x${height}")
        // @TODO: Configure should happen before loop. But we should ensure that all the korgw targets are ready for this
        //realGameWindow.configure(width, height, title, icon, fullscreen)
        realGameWindow.loop {
            val gameWindow = this
            if (OS.isNative) println("Korui[0]")
            gameWindow.registerTime("configureGameWindow") {
                realGameWindow.configure(width, height, title, icon, fullscreen, bgcolor ?: Colors.BLACK)
            }
            gameWindow.registerTime("setIcon") {
                try {
                    // Do nothing
                    when {
                        //iconDrawable != null -> this.icon = iconDrawable.render()
                        iconPath != null -> this.icon = resourcesVfs[iconPath!!].readBitmapOptimized(imageFormats)
                        else -> Unit
                    }
                } catch (e: Throwable) {
                    logger.error { "Couldn't get the application icon" }
                    e.printStackTrace()
                }
            }
            this.quality = quality
            if (OS.isNative) println("CanvasApplicationEx.IN[0]")
            val input = Input()
            val stats = Stats()

            // Use this once Korgw is on 1.12.5
            //val views = Views(gameWindow.getCoroutineDispatcherWithCurrentContext() + SupervisorJob(), ag, injector, input, timeProvider, stats, gameWindow)
            val views: Views = Views(
                coroutineContext = coroutineContext + gameWindow.coroutineDispatcher + AsyncInjectorContext(injector) + SupervisorJob(),
                //ag = if (debugAg) PrintAG() else ag,
                ag = ag,
                injector = injector,
                input = input,
                timeProvider = timeProvider,
                stats = stats,
                gameWindow = gameWindow,
                gameId = gameId,
                settingsFolder = settingsFolder,
                batchMaxQuads = batchMaxQuads
            ).also {
                it.init()
            }

            if (OS.isJsBrowser) KDynamic { global["views"] = views }
            injector
                .mapInstance(ModuleArgs(args))
                .mapInstance(GameWindow::class, gameWindow)
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

            gameWindow.registerTime("prepareViews") {
                prepareViews(views, gameWindow, bgcolor != null, bgcolor ?: Colors.TRANSPARENT_BLACK, waitForFirstRender = true)
            }

            gameWindow.registerTime("nativeSoundProvider") {
                nativeSoundProvider.initOnce()
            }
            gameWindow.registerTime("completeViews") {
                completeViews(views)
            }
            views.launchImmediately {
                coroutineScope {
                    //println("coroutineContext: $coroutineContext")
                    //println("GameWindow: ${coroutineContext[GameWindow]}")
                    entry(views.stage)
                    if (blocking) {
                        // @TODO: Do not complete to prevent job cancelation?
                        gameWindow.waitClose()
                    }
                }
            }
            if (OS.isNative) println("CanvasApplicationEx.IN[1]")
            if (OS.isNative) println("Korui[1]")

            if (blocking) {
                // @TODO: Do not complete to prevent job cancelation?
                gameWindow.waitClose()
                gameWindow.exit()
            }
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
        fixedSizeStep: TimeSpan = TimeSpan.NIL
    ): CompletableDeferred<Unit> {
        val injector = views.injector
        injector.mapInstance(views)
        injector.mapInstance(views.ag)
        injector.mapInstance(Resources::class, views.globalResources)
        injector.mapSingleton(ResourcesRoot::class) { ResourcesRoot() }
        injector.mapInstance(views.input)
        injector.mapInstance(views.stats)
        injector.mapInstance(CoroutineContext::class, views.coroutineContext)
        injector.mapPrototype(EmptyScene::class) { EmptyScene() }
        injector.mapInstance(TimeProvider::class, views.timeProvider)

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

        /*
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
        */

        fun mouseDown(type: String, x: Double, y: Double, button: MouseButton) {
            input.toggleButton(button, true)
            input.mouse.setTo(x, y)
            input.mouseDown.setTo(x, y)
            views.mouseUpdated()
            downPos.copyFrom(input.mouse)
            downTime = DateTime.now()
            input.mouseInside = true
        }

        fun mouseUp(type: String, x: Double, y: Double, button: MouseButton) {
            //Console.log("mouseUp: $name")
            input.toggleButton(button, false)
            input.mouse.setTo(x, y)
            views.mouseUpdated()
            upPos.copyFrom(views.input.mouse)
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

        val mouseTouchEvent = TouchEvent()

        fun dispatchSimulatedTouchEvent(x: Double, y: Double, button: MouseButton, type: TouchEvent.Type, status: Touch.Status) {
            mouseTouchEvent.screen = 0
            mouseTouchEvent.emulated = true
            mouseTouchEvent.currentTime = DateTime.now()
            mouseTouchEvent.scaleCoords = false
            mouseTouchEvent.startFrame(type)
            mouseTouchEvent.touch(button.id, x, y, status, kind = Touch.Kind.MOUSE, button = button)
            mouseTouchEvent.endFrame()
            views.dispatch(mouseTouchEvent)
        }

        eventDispatcher.addEventListener<MouseEvent> { e ->
            //println("MOUSE: $e")
            logger.trace { "eventDispatcher.addEventListener<MouseEvent>:$e" }
            val x = getRealX(e.x.toDouble(), e.scaleCoords)
            val y = getRealY(e.y.toDouble(), e.scaleCoords)
            when (e.type) {
                MouseEvent.Type.DOWN -> {
                    mouseDown("mouseDown", x, y, e.button)
                    //updateTouch(mouseTouchId, x, y, start = true, end = false)
                    dispatchSimulatedTouchEvent(x, y, e.button, TouchEvent.Type.START, Touch.Status.ADD)
                }
                MouseEvent.Type.UP -> {
                    mouseUp("mouseUp", x, y, e.button)
                    //updateTouch(mouseTouchId, x, y, start = false, end = true)
                    dispatchSimulatedTouchEvent(x, y, e.button, TouchEvent.Type.END, Touch.Status.REMOVE)
                }
                MouseEvent.Type.DRAG -> {
                    mouseDrag("onMouseDrag", x, y)
                    //updateTouch(mouseTouchId, x, y, start = false, end = false)
                    dispatchSimulatedTouchEvent(x, y, e.button, TouchEvent.Type.MOVE, Touch.Status.KEEP)
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

        val touchMouseEvent = MouseEvent()
        eventDispatcher.addEventListener<TouchEvent> { e ->
            logger.trace { "eventDispatcher.addEventListener<TouchEvent>:$e" }

            input.updateTouches(e)
            views.dispatch(e)

            // Touch to mouse events
            if (e.numTouches == 1) {
                val start = e.isStart
                val end = e.isEnd
                val t = e.touches.first()
                val x = getRealX(t.x, e.scaleCoords)
                val y = getRealY(t.y, e.scaleCoords)
                val button = MouseButton.LEFT
                touchMouseEvent.id = 0
                touchMouseEvent.button = button
                touchMouseEvent.buttons = if (end) 0 else 1 shl button.id
                touchMouseEvent.x = x.toInt()
                touchMouseEvent.y = y.toInt()
                touchMouseEvent.scaleCoords = false
                touchMouseEvent.emulated = true
                //updateTouch(t.id, x, y, start, end)
                when {
                    start -> {
                        mouseDown("onTouchStart", x, y, button)
                        touchMouseEvent.type = MouseEvent.Type.DOWN
                    }
                    end -> {
                        mouseUp("onTouchEnd", x, y, button)
                        moveMouseOutsideInNextFrame = true
                        touchMouseEvent.type = MouseEvent.Type.UP
                    }
                    else -> {
                        mouseMove("onTouchMove", x, y, inside = true)
                        touchMouseEvent.type = MouseEvent.Type.DRAG
                    }
                }
                views.dispatch(touchMouseEvent)
            }

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
            //println("resized. ${ag.backWidth}, ${ag.backHeight}")
            views.resized(ag.backWidth, ag.backHeight)
        }

        //println("eventDispatcher.dispatch(ReshapeEvent(0, 0, views.nativeWidth, views.nativeHeight)) : ${views.nativeWidth}x${views.nativeHeight}")
        eventDispatcher.dispatch(ReshapeEvent(0, 0, views.nativeWidth, views.nativeHeight))

        var renderShown = false
        views.clearEachFrame = clearEachFrame
        views.clearColor = bgcolor
        val firstRenderDeferred = CompletableDeferred<Unit>()
        views.gameWindow.onRenderEvent {
            //println("RenderEvent: $it")
            views.ag.doRender {
                if (!renderShown) {
                    //println("!!!!!!!!!!!!! views.gameWindow.addEventListener<RenderEvent>")
                    renderShown = true
                    firstRenderDeferred.complete(Unit)
                }
                try {
                    views.frameUpdateAndRender(fixedSizeStep = fixedSizeStep)

                    views.input.mouseOutside = false
                    if (moveMouseOutsideInNextFrame) {
                        moveMouseOutsideInNextFrame = false
                        views.input.mouseOutside = true
                        views.input.mouseInside = false
                        views.mouseUpdated()
                    }
                } catch (e: Throwable) {
                    Console.error("views.gameWindow.onRenderEvent:")
                    e.printStackTrace()
                    if (views.rethrowRenderError) throw e
                }
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
            fixedSizeStep: TimeSpan = TimeSpan.NIL,
            waitForFirstRender: Boolean = true
    ) {
        val firstRenderDeferred = prepareViewsBase(views, eventDispatcher, clearEachFrame, bgcolor, fixedSizeStep)
        if (waitForFirstRender) {
            firstRenderDeferred.await()
        }
    }

	data class Config(
        val module: Module = Module(),
        val args: Array<String> = arrayOf(),
        val imageFormats: ImageFormat = RegisteredImageFormats,
        val gameWindow: GameWindow? = null,
		//val eventDispatcher: EventDispatcher = gameWindow ?: DummyEventDispatcher, // Removed
        val sceneClass: KClass<out Scene>? = module.mainScene,
        val sceneInjects: List<Any> = listOf(),
        val timeProvider: TimeProvider = TimeProvider,
        val injector: AsyncInjector = AsyncInjector(),
        val debug: Boolean = false,
        val trace: Boolean = false,
        val context: Any? = null,
        val fullscreen: Boolean? = null,
        val blocking: Boolean = true,
        val gameId: String = DEFAULT_GAME_ID,
        val settingsFolder: String? = null,
        val batchMaxQuads: Int = BatchBuilder2D.DEFAULT_BATCH_QUADS,
        val virtualSize: ISizeInt? = null,
        val windowSize: ISizeInt? = null,
        val scaleMode: ScaleMode? = null,
        val scaleAnchor: Anchor? = null,
        val clipBorders: Boolean? = null,
        val title: String? = null,
        val bgcolor: RGBA? = null,
        val quality: GameWindow.Quality? = null,
        val icon: String? = null,
        val main: (suspend Stage.() -> Unit)? = null,
        val constructedViews: (Views) -> Unit = {}
	)

	data class ModuleArgs(val args: Array<String>)
}

