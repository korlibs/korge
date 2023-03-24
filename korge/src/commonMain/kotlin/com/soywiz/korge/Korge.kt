package com.soywiz.korge

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.klock.DateTime
import com.soywiz.klock.TimeProvider
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.klogger.Logger
import com.soywiz.kmem.*
import com.soywiz.korag.log.AGPrint
import com.soywiz.korev.*
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
import com.soywiz.korio.dynamic.*
import com.soywiz.korio.file.std.localCurrentDirVfs
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.resources.Resources
import com.soywiz.korma.geom.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.reflect.KClass

typealias KorgeConfig = Korge

data class Korge(
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
    val virtualSize: SizeInt? = module.virtualSize,
    val windowSize: SizeInt? = module.windowSize,
    val scaleMode: ScaleMode? = null,
    val scaleAnchor: Anchor? = null,
    val clipBorders: Boolean? = null,
    val title: String? = null,
    val bgcolor: RGBA? = null,
    val quality: GameWindow.Quality? = null,
    val icon: String? = null,
    val multithreaded: Boolean? = null,
    val forceRenderEveryFrame: Boolean = true,
    val main: (suspend Stage.() -> Unit)? = module.main,
    val debugAg: Boolean = false,
    val debugFontExtraScale: Double = 1.0,
    val debugFontColor: RGBA = Colors.WHITE,
    val headlessDraw: Boolean = false,
    val stageBuilder: (Views) -> Stage = { Stage(it) },
    val constructedScene: Scene.(Views) -> Unit = module.constructedScene,
    val constructedViews: (Views) -> Unit = module.constructedViews,
    val unit: Unit = Unit,
) {
    companion object {
        val logger = Logger("Korge")
        val DEFAULT_GAME_ID = "com.soywiz.korge.unknown"
        val DEFAULT_WINDOW_SIZE: SizeInt get() = DefaultViewport.SIZE
    }

    val finalMain: (suspend Stage.() -> Unit) get() = main ?: module.main ?: {}
    val finalImageFormats get() = imageFormats + module.imageFormats
    val finalTitle: String get() = title ?: module.title
    val finalWindowSize: SizeInt get() = windowSize ?: module.windowSize
    val finalVirtualSize: SizeInt get() = virtualSize ?: module.virtualSize
    val finalScaleAnchor: Anchor get() = scaleAnchor ?: module.scaleAnchor
    val finalScaleMode: ScaleMode get() = scaleMode ?: module.scaleMode
    val finalClipBorders: Boolean get() = clipBorders ?: module.clipBorders
    val finalTargetFps: Double get() = module.targetFps

    suspend fun start(entry: suspend Stage.() -> Unit = this.finalMain) {
        KorgeRunner.invoke(this.copy(main = entry))
    }
}

suspend fun Korge(entry: suspend Stage.() -> Unit) { Korge().start(entry) }

suspend fun Korge(config: KorgeConfig, entry: suspend Stage.() -> Unit) { config.start(entry) }

/**
 * Entry point for games written in Korge.
 * You have to call the [Korge] method by either providing some parameters, or a [Korge.Config] object.
 */
object KorgeRunner {
    suspend operator fun invoke(config: Korge) {
        RegisteredImageFormats.register(config.imageFormats)

        val iconPath = config.icon
        val imageFormats = config.finalImageFormats
        val entry = config.finalMain
        val multithreaded = config.multithreaded
        val windowSize = config.windowSize ?: config.module.windowSize

        if (!Platform.isJsBrowser) {
            configureLoggerFromProperties(localCurrentDirVfs["klogger.properties"])
        }
        val realGameWindow = (config.gameWindow ?: coroutineContext[GameWindow] ?: CreateDefaultGameWindow(GameWindowCreationConfig(multithreaded = multithreaded)))
        realGameWindow.bgcolor = config.bgcolor ?: Colors.BLACK
        //println("Configure: ${width}x${height}")
        // @TODO: Configure should happen before loop. But we should ensure that all the korgw targets are ready for this
        //realGameWindow.configure(width, height, title, icon, fullscreen)
        realGameWindow.loop {
            val gameWindow = this
            if (Platform.isNative) println("Korui[0]")
            gameWindow.registerTime("configureGameWindow") {
                realGameWindow.configure(windowSize, title, icon, fullscreen, bgcolor ?: Colors.BLACK)
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
                    Korge.logger.error { "Couldn't get the application icon" }
                    e.printStackTrace()
                }
            }
            this.quality = quality
            if (Platform.isNative) println("CanvasApplicationEx.IN[0]")
            val input = Input()
            val stats = Stats()

            // Use this once Korgw is on 1.12.5
            //val views = Views(gameWindow.getCoroutineDispatcherWithCurrentContext() + SupervisorJob(), ag, injector, input, timeProvider, stats, gameWindow)
            val views: Views = Views(
                coroutineContext = coroutineContext + gameWindow.coroutineDispatcher + AsyncInjectorContext(config.injector) + SupervisorJob(),
                ag = if (config.debugAg) AGPrint() else ag,
                injector = config.injector,
                input = input,
                timeProvider = config.timeProvider,
                stats = stats,
                gameWindow = gameWindow,
                gameId = config.gameId,
                settingsFolder = config.settingsFolder,
                batchMaxQuads = config.batchMaxQuads,
                stageBuilder = config.stageBuilder
            ).also {
                it.init()
            }

            if (Platform.isJsBrowser) {
                Dyn.global["views"] = views
            }
            config.injector
                .mapInstance(ModuleArgs(config.args))
                .mapInstance(GameWindow::class, gameWindow)
                .mapInstance<Module>(object : Module() {
                    override val title = config.finalTitle
                    override val fullscreen: Boolean? = config.fullscreen
                    override val windowSize = config.finalWindowSize
                    override val virtualSize = config.finalVirtualSize
                })
            views.debugViews = debug
            views.debugFontExtraScale = config.debugFontExtraScale
            views.debugFontColor = config.debugFontColor
            views.virtualWidth = config.finalVirtualSize.width
            views.virtualHeight = config.finalVirtualSize.height
            views.scaleAnchor = config.finalScaleAnchor
            views.scaleMode = config.finalScaleMode
            views.clipBorders = config.finalClipBorders
            views.targetFps = config.finalTargetFps
            //Korge.prepareViews(views, gameWindow, bgcolor != null, bgcolor ?: Colors.TRANSPARENT_BLACK)

            gameWindow.registerTime("prepareViews") {
                prepareViews(
                    views,
                    gameWindow,
                    bgcolor != null,
                    bgcolor ?: Colors.TRANSPARENT,
                    waitForFirstRender = true,
                    forceRenderEveryFrame = config.forceRenderEveryFrame
                )
            }

            gameWindow.registerTime("completeViews") {
                completeViews(views)
            }
            views.launchImmediately {
                coroutineScope {
                    //println("coroutineContext: $coroutineContext")
                    //println("GameWindow: ${coroutineContext[GameWindow]}")
                    entry(views.stage)
                    if (config.blocking) {
                        // @TODO: Do not complete to prevent job cancelation?
                        gameWindow.waitClose()
                    }
                }
            }
            if (Platform.isNative) println("CanvasApplicationEx.IN[1]")
            if (Platform.isNative) println("Korui[1]")

            if (config.blocking) {
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
        eventDispatcher: EventListener,
        clearEachFrame: Boolean = true,
        bgcolor: RGBA = Colors.TRANSPARENT,
        fixedSizeStep: TimeSpan = TimeSpan.NIL,
        forceRenderEveryFrame: Boolean = true
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
        val downPos = MPoint()
        val upPos = MPoint()
        var downTime = DateTime.EPOCH
        var moveTime = DateTime.EPOCH
        var upTime = DateTime.EPOCH
        var moveMouseOutsideInNextFrame = false
        val mouseTouchId = -1
        views.forceRenderEveryFrame = forceRenderEveryFrame

        val tempXY: MPoint = MPoint()

        // devicePixelRatio might change at runtime by changing the resolution or changing the screen of the window
        fun getRealXY(x: Double, y: Double, scaleCoords: Boolean, out: MPoint = tempXY): MPoint {
            return views.windowToGlobalCoords(x, y, out)
        }

        fun getRealX(x: Double, scaleCoords: Boolean): Double = if (scaleCoords) x * views.devicePixelRatio else x
        fun getRealY(y: Double, scaleCoords: Boolean): Double = if (scaleCoords) y * views.devicePixelRatio else y

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

        fun mouseDown(type: String, p: Point, button: MouseButton) {
            input.toggleButton(button, true)
            input.setMouseGlobalPos(p, down = false)
            input.setMouseGlobalPos(p, down = true)
            views.mouseUpdated()
            downPos.copyFrom(input.mousePos)
            downTime = DateTime.now()
            input.mouseInside = true
        }

        fun mouseUp(type: String, p: Point, button: MouseButton) {
            //Console.log("mouseUp: $name")
            input.toggleButton(button, false)
            input.setMouseGlobalPos(p, down = false)
            views.mouseUpdated()
            upPos.copyFrom(views.input.mousePos)
        }

        fun mouseMove(type: String, p: Point, inside: Boolean) {
            views.input.setMouseGlobalPos(p, down = false)
            views.input.mouseInside = inside
            if (!inside) {
                moveMouseOutsideInNextFrame = true
            }
            views.mouseUpdated()
            moveTime = DateTime.now()
        }

        fun mouseDrag(type: String, p: Point) {
            views.input.setMouseGlobalPos(p, down = false)
            views.mouseUpdated()
            moveTime = DateTime.now()
        }

        val mouseTouchEvent = TouchEvent()

        fun dispatchSimulatedTouchEvent(
            p: Point,
            button: MouseButton,
            type: TouchEvent.Type,
            status: Touch.Status
        ) {
            mouseTouchEvent.screen = 0
            mouseTouchEvent.emulated = true
            mouseTouchEvent.currentTime = DateTime.now()
            mouseTouchEvent.scaleCoords = false
            mouseTouchEvent.startFrame(type)
            mouseTouchEvent.touch(button.id, p.xD, p.yD, status, kind = Touch.Kind.MOUSE, button = button)
            mouseTouchEvent.endFrame()
            views.dispatch(mouseTouchEvent)
        }

        eventDispatcher.onEvents(*MouseEvent.Type.ALL) { e ->
            //println("MOUSE: $e")
            Korge.logger.trace { "eventDispatcher.addEventListener<MouseEvent>:$e" }
            val p = getRealXY(e.x.toDouble(), e.y.toDouble(), e.scaleCoords).point
            when (e.type) {
                MouseEvent.Type.DOWN -> {
                    mouseDown("mouseDown", p, e.button)
                    //updateTouch(mouseTouchId, x, y, start = true, end = false)
                    dispatchSimulatedTouchEvent(p, e.button, TouchEvent.Type.START, Touch.Status.ADD)
                }

                MouseEvent.Type.UP -> {
                    mouseUp("mouseUp", p, e.button)
                    //updateTouch(mouseTouchId, x, y, start = false, end = true)
                    dispatchSimulatedTouchEvent(p, e.button, TouchEvent.Type.END, Touch.Status.REMOVE)
                }

                MouseEvent.Type.DRAG -> {
                    mouseDrag("onMouseDrag", p)
                    //updateTouch(mouseTouchId, x, y, start = false, end = false)
                    dispatchSimulatedTouchEvent(p, e.button, TouchEvent.Type.MOVE, Touch.Status.KEEP)
                }

                MouseEvent.Type.MOVE -> mouseMove("mouseMove", p, inside = true)
                MouseEvent.Type.CLICK -> Unit
                MouseEvent.Type.ENTER -> mouseMove("mouseEnter", p, inside = true)
                MouseEvent.Type.EXIT -> mouseMove("mouseExit", p, inside = false)
                MouseEvent.Type.SCROLL -> Unit
            }
            views.dispatch(e)
        }

        eventDispatcher.onEvents(*KeyEvent.Type.ALL) { e ->
            Korge.logger.trace { "eventDispatcher.addEventListener<KeyEvent>:$e" }
            views.dispatch(e)
        }
        eventDispatcher.onEvents(*GestureEvent.Type.ALL) { e ->
            Korge.logger.trace { "eventDispatcher.addEventListener<GestureEvent>:$e" }
            views.dispatch(e)
        }

        eventDispatcher.onEvents(*DropFileEvent.Type.ALL) { e -> views.dispatch(e) }
        eventDispatcher.onEvent(ResumeEvent) { e -> views.dispatch(e) }
        eventDispatcher.onEvent(PauseEvent) { e -> views.dispatch(e) }
        eventDispatcher.onEvent(StopEvent) { e -> views.dispatch(e) }
        eventDispatcher.onEvent(DestroyEvent) { e ->
            try {
                views.dispatch(e)
            } finally {
                views.launchImmediately {
                    views.close()
                }
            }
        }

        val touchMouseEvent = MouseEvent()
        eventDispatcher.onEvents(*TouchEvent.Type.ALL) { e ->
            Korge.logger.trace { "eventDispatcher.addEventListener<TouchEvent>:$e" }

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
                val p = t.p
                val x = t.x
                val y = t.y
                val button = MouseButton.LEFT

                //updateTouch(t.id, x, y, start, end)
                when {
                    start -> mouseDown("onTouchStart", p, button)
                    end -> mouseUp("onTouchEnd", p, button)
                    else -> mouseMove("onTouchMove", p, inside = true)
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

        eventDispatcher.onEvents(*GamePadConnectionEvent.Type.ALL) { e ->
            Korge.logger.trace { "eventDispatcher.addEventListener<GamePadConnectionEvent>:$e" }
            views.dispatch(e)
        }

        eventDispatcher.onEvent(GamePadUpdateEvent) { e ->
            gamepadUpdated(e)
            views.dispatch(e)
        }

        eventDispatcher.onEvent(ReshapeEvent) { e ->
            //try { throw Exception() } catch (e: Throwable) { e.printStackTrace() }
            //println("eventDispatcher.addEventListener<ReshapeEvent>: ${ag.backWidth}x${ag.backHeight} : ${e.width}x${e.height}")
            //println("resized. ${ag.backWidth}, ${ag.backHeight}")
            views.resized(ag.mainFrameBuffer.width, ag.mainFrameBuffer.height)
        }

        //println("eventDispatcher.dispatch(ReshapeEvent(0, 0, views.nativeWidth, views.nativeHeight)) : ${views.nativeWidth}x${views.nativeHeight}")
        eventDispatcher.dispatch(ReshapeEvent(0, 0, views.nativeWidth, views.nativeHeight))

        eventDispatcher.onEvent(ReloadEvent) { views.dispatch(it) }

        var renderShown = false
        views.clearEachFrame = clearEachFrame
        views.clearColor = bgcolor
        val firstRenderDeferred = CompletableDeferred<Unit>()

        fun renderBlock(event: RenderEvent) {
            //println("renderBlock: $event")
            try {
                views.frameUpdateAndRender(
                    fixedSizeStep = fixedSizeStep,
                    forceRender = views.forceRenderEveryFrame,
                    doUpdate = event.update,
                    doRender = event.render,
                )

                views.input.mouseOutside = false
                if (moveMouseOutsideInNextFrame) {
                    moveMouseOutsideInNextFrame = false
                    views.input.mouseOutside = true
                    views.input.mouseInside = false
                    views.mouseUpdated()
                }
            } catch (e: Throwable) {
                Korge.logger.error { "views.gameWindow.onRenderEvent:" }
                e.printStackTrace()
                if (views.rethrowRenderError) throw e
            }
        }

        views.gameWindow.onRenderEvent { event ->
            //println("RenderEvent: $event")
            if (!event.render) {
                renderBlock(event)
            } else {
                views.renderContext.doRender {
                    if (!renderShown) {
                        //println("!!!!!!!!!!!!! views.gameWindow.addEventListener<RenderEvent>")
                        renderShown = true
                        firstRenderDeferred.complete(Unit)
                    }
                    renderBlock(event)
                }
            }
        }

        return firstRenderDeferred
    }

    @KorgeInternal
    suspend fun prepareViews(
        views: Views,
        eventDispatcher: EventListener,
        clearEachFrame: Boolean = true,
        bgcolor: RGBA = Colors.TRANSPARENT,
        fixedSizeStep: TimeSpan = TimeSpan.NIL,
        waitForFirstRender: Boolean = true,
        forceRenderEveryFrame: Boolean = true
    ) {
        val firstRenderDeferred =
            prepareViewsBase(views, eventDispatcher, clearEachFrame, bgcolor, fixedSizeStep, forceRenderEveryFrame)
        if (waitForFirstRender) {
            firstRenderDeferred.await()
        }
    }

	data class ModuleArgs(val args: Array<String>)
}

