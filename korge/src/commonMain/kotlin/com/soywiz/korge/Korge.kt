package com.soywiz.korge

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.klock.DateTime
import com.soywiz.klock.TimeProvider
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.klogger.Console
import com.soywiz.klogger.Logger
import com.soywiz.korag.log.PrintAG
import com.soywiz.korev.DestroyEvent
import com.soywiz.korev.DropFileEvent
import com.soywiz.korev.EventDispatcher
import com.soywiz.korev.GamePadConnectionEvent
import com.soywiz.korev.GamePadUpdateEvent
import com.soywiz.korev.KeyEvent
import com.soywiz.korev.MouseButton
import com.soywiz.korev.MouseEvent
import com.soywiz.korev.PauseEvent
import com.soywiz.korev.ReshapeEvent
import com.soywiz.korev.ResumeEvent
import com.soywiz.korev.StopEvent
import com.soywiz.korev.Touch
import com.soywiz.korev.TouchEvent
import com.soywiz.korev.addEventListener
import com.soywiz.korev.dispatch
import com.soywiz.korge.input.Input
import com.soywiz.korge.internal.DefaultViewport
import com.soywiz.korge.internal.KorgeInternal
import com.soywiz.korge.logger.configureLoggerFromProperties
import com.soywiz.korge.render.BatchBuilder2D
import com.soywiz.korge.resources.ResourcesRoot
import com.soywiz.korge.scene.EmptyScene
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.scene.SceneContainer
import com.soywiz.korge.stat.Stats
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.Views
import com.soywiz.korgw.CreateDefaultGameWindow
import com.soywiz.korgw.GameWindow
import com.soywiz.korgw.GameWindowCreationConfig
import com.soywiz.korgw.configure
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.format.ImageFormat
import com.soywiz.korim.format.ImageFormats
import com.soywiz.korim.format.RegisteredImageFormats
import com.soywiz.korim.format.plus
import com.soywiz.korim.format.readBitmap
import com.soywiz.korinject.AsyncInjector
import com.soywiz.korinject.AsyncInjectorContext
import com.soywiz.korio.async.delay
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.dynamic.KDynamic
import com.soywiz.korio.file.std.localCurrentDirVfs
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.resources.Resources
import com.soywiz.korio.util.OS
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.ISizeInt
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.ScaleMode
import com.soywiz.korma.geom.SizeInt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.reflect.KClass

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
            multithreaded = config.multithreaded,
            entry = {
                //println("Korge views prepared for Config")
                RegisteredImageFormats.register(*module.imageFormats.toTypedArray())
                val injector = config.injector
                injector.mapInstance(Module::class, module)
                injector.mapInstance(Config::class, config)

                module.apply { injector.configure() }

                config.constructedViews(views)

                when {
                    config.main != null -> {
                        config.main?.invoke(stage)
                    }
                    config.sceneClass != null -> {
                        val sc = SceneContainer(views, name = "rootSceneContainer")
                        views.stage += sc
                        val scene = sc.changeTo(config.sceneClass, *config.sceneInjects.toTypedArray(), time = 0.milliseconds)
                        config.constructedScene(scene, views)
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
        debugFontExtraScale: Double = 1.0,
        debugFontColor: RGBA = Colors.WHITE,
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
        multithreaded: Boolean? = null,
        entry: suspend Stage.() -> Unit
	) {
        if (!OS.isJsBrowser) {
            configureLoggerFromProperties(localCurrentDirVfs["klogger.properties"])
        }
        val realGameWindow = (gameWindow ?: coroutineContext[GameWindow] ?: CreateDefaultGameWindow(GameWindowCreationConfig(multithreaded = multithreaded)))
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
                        iconPath != null -> this.icon = resourcesVfs[iconPath!!].readBitmap(imageFormats)
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
                ag = if (debugAg) PrintAG() else ag,
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
            views.debugFontExtraScale = debugFontExtraScale
            views.debugFontColor = debugFontColor
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
        KorgeReload.registerEventDispatcher(eventDispatcher)

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

        val tempXY: Point = Point()
        // devicePixelRatio might change at runtime by changing the resolution or changing the screen of the window
        fun getRealXY(x: Double, y: Double, scaleCoords: Boolean, out: Point = tempXY): Point {
            return views.windowToGlobalCoords(x, y, out)
        }

        fun getRealX(x: Double, scaleCoords: Boolean): Double = if (scaleCoords) x * ag.devicePixelRatio else x
        fun getRealY(y: Double, scaleCoords: Boolean): Double = if (scaleCoords) y * ag.devicePixelRatio else y

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
            input.setMouseGlobalXY(x, y, down = false)
            input.setMouseGlobalXY(x, y, down = true)
            views.mouseUpdated()
            downPos.copyFrom(input.mouse)
            downTime = DateTime.now()
            input.mouseInside = true
        }

        fun mouseUp(type: String, x: Double, y: Double, button: MouseButton) {
            //Console.log("mouseUp: $name")
            input.toggleButton(button, false)
            input.setMouseGlobalXY(x, y, down = false)
            views.mouseUpdated()
            upPos.copyFrom(views.input.mouse)
        }

        fun mouseMove(type: String, x: Double, y: Double, inside: Boolean) {
            views.input.setMouseGlobalXY(x, y, down = false)
            views.input.mouseInside = inside
            if (!inside) {
                moveMouseOutsideInNextFrame = true
            }
            views.mouseUpdated()
            moveTime = DateTime.now()
        }

        fun mouseDrag(type: String, x: Double, y: Double) {
            views.input.setMouseGlobalXY(x, y, down = false)
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
            val (x, y) = getRealXY(e.x.toDouble(), e.y.toDouble(), e.scaleCoords)
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


        eventDispatcher.addEventListener<DropFileEvent> { e -> views.dispatch(e) }
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
            val ee = input.touch
            for (t in ee.touches) {
                val (x, y) = getRealXY(t.x, t.y, e.scaleCoords)
                t.x = x
                t.y = y
            }
            views.dispatch(ee)

            // Touch to mouse events
            if (ee.numTouches == 1) {
                val start = ee.isStart
                val end = ee.isEnd
                val t = ee.touches.first()
                val x = t.x
                val y = t.y
                val button = MouseButton.LEFT

                //updateTouch(t.id, x, y, start, end)
                when {
                    start -> mouseDown("onTouchStart", x, y, button)
                    end -> mouseUp("onTouchEnd", x, y, button)
                    else -> mouseMove("onTouchMove", x, y, inside = true)
                }
                views.dispatch(touchMouseEvent.also {
                    it.id = 0
                    it.button = button
                    it.buttons = if (end) 0 else 1 shl button.id
                    it.x = x.toInt()
                    it.y = y.toInt()
                    it.scaleCoords = false
                    it.emulated = true
                    it.type = when {
                        start -> MouseEvent.Type.DOWN
                        end -> MouseEvent.Type.UP
                        else -> MouseEvent.Type.DRAG
                    }
                })
                if (end) {
                    moveMouseOutsideInNextFrame = true
                }
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

        eventDispatcher.addEventListener<ReloadEvent> { views.dispatch(it) }

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
        val virtualSize: ISizeInt? = module.size,
        val windowSize: ISizeInt? = module.windowSize,
        val scaleMode: ScaleMode? = null,
        val scaleAnchor: Anchor? = null,
        val clipBorders: Boolean? = null,
        val title: String? = null,
        val bgcolor: RGBA? = null,
        val quality: GameWindow.Quality? = null,
        val icon: String? = null,
        val multithreaded: Boolean? = null,
        val main: (suspend Stage.() -> Unit)? = module.main,
        val constructedScene: Scene.(Views) -> Unit = module.constructedScene,
        val constructedViews: (Views) -> Unit = module.constructedViews,
	) {
        val finalWindowSize: ISizeInt get() = windowSize ?: module.windowSize
        val finalVirtualSize: ISizeInt get() = virtualSize ?: module.size
    }

	data class ModuleArgs(val args: Array<String>)
}

