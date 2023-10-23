package korlibs.korge

import korlibs.annotations.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.inject.*
import korlibs.io.dynamic.*
import korlibs.io.file.std.*
import korlibs.korge.internal.*
import korlibs.korge.logger.*
import korlibs.korge.render.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.logger.*
import korlibs.math.geom.*
import korlibs.platform.*
import korlibs.render.*
import korlibs.time.*
import kotlin.coroutines.*
import kotlin.reflect.*

typealias KorgeConfig = Korge

data class KorgeDisplayMode(val scaleMode: ScaleMode, val scaleAnchor: Anchor, val clipBorders: Boolean) {
    companion object {
        val DEFAULT get() = CENTER
        val CENTER = KorgeDisplayMode(ScaleMode.SHOW_ALL, Anchor.CENTER, clipBorders = true)
        //@Deprecated("Typically TOP_LEFT_NO_CLIP is better")
        val CENTER_NO_CLIP = KorgeDisplayMode(ScaleMode.SHOW_ALL, Anchor.CENTER, clipBorders = false)
        val TOP_LEFT_NO_CLIP = KorgeDisplayMode(ScaleMode.SHOW_ALL, Anchor.TOP_LEFT, clipBorders = false)
        val NO_SCALE = KorgeDisplayMode(ScaleMode.NO_SCALE, Anchor.TOP_LEFT, clipBorders = false)
    }
}

suspend fun Korge(
    args: Array<String> = arrayOf(),
    imageFormats: ImageFormat = RegisteredImageFormats,
    gameWindow: GameWindow? = null,
    //val eventDispatcher: EventDispatcher = gameWindow ?: DummyEventDispatcher, // Removed
    @DeprecatedParameter("Create a sceneContainer instead") mainSceneClass: KClass<out Scene>? = null,
    timeProvider: TimeProvider = TimeProvider,
    injector: Injector = Injector(),
    configInjector: Injector.() -> Unit = {},
    debug: Boolean = false,
    trace: Boolean = false,
    context: Any? = null,
    fullscreen: Boolean? = null,
    blocking: Boolean = true,
    gameId: String = Korge.DEFAULT_GAME_ID,
    settingsFolder: String? = null,
    batchMaxQuads: Int = BatchBuilder2D.DEFAULT_BATCH_QUADS,
    // @TODO: Why @Deprecated doesn't support AnnotationTarget.VALUE_PARAMETER???
    @DeprecatedParameter("Use windowSize instead") windowWidth: Int = DefaultViewport.SIZE.width.toInt(),
    @DeprecatedParameter("Use windowSize instead") windowHeight: Int = DefaultViewport.SIZE.height.toInt(),
    windowSize: Size = Size(windowWidth, windowHeight),
    @DeprecatedParameter("Use virtualSize instead") virtualWidth: Int = windowSize.width.toInt(),
    @DeprecatedParameter("Use virtualSize instead") virtualHeight: Int = windowSize.height.toInt(),
    virtualSize: Size = Size(virtualWidth, virtualHeight),
    @DeprecatedParameter("Use displayMode instead") scaleMode: ScaleMode = ScaleMode.SHOW_ALL,
    @DeprecatedParameter("Use displayMode instead") scaleAnchor: Anchor = Anchor.CENTER,
    @DeprecatedParameter("Use displayMode instead") clipBorders: Boolean = true,
    displayMode: KorgeDisplayMode = KorgeDisplayMode(scaleMode, scaleAnchor, clipBorders),
    title: String = "Game",
    @DeprecatedParameter("Use backgroundColor instead")
    bgcolor: RGBA? = Colors.BLACK,
    backgroundColor: RGBA? = bgcolor,
    quality: GameWindowQuality = GameWindowQuality.PERFORMANCE,
    icon: String? = null,
    @DeprecatedParameter("Ignored") multithreaded: Boolean? = null,
    forceRenderEveryFrame: Boolean = true,
    @DeprecatedParameter("Use entry instead") main: (suspend Stage.() -> Unit) = {},
    debugAg: Boolean = false,
    debugFontExtraScale: Double = 1.0,
    debugFontColor: RGBA = Colors.WHITE,
    stageBuilder: (Views) -> Stage = { Stage(it) },
    targetFps: Double = 0.0,
    entry: suspend Stage.() -> Unit = {},
): Unit = Korge(
    args = args, imageFormats = imageFormats, gameWindow = gameWindow, mainSceneClass = mainSceneClass,
    timeProvider = timeProvider, injector = injector, configInjector = configInjector, debug = debug,
    trace = trace, context = context, fullscreen = fullscreen, blocking = blocking, gameId = gameId,
    settingsFolder = settingsFolder, batchMaxQuads = batchMaxQuads,
    windowSize = windowSize, virtualSize = virtualSize,
    displayMode = displayMode, title = title, backgroundColor = backgroundColor, quality = quality,
    icon = icon,
    multithreaded = multithreaded,
    forceRenderEveryFrame = forceRenderEveryFrame,
    main = main,
    debugAg = debugAg,
    debugFontExtraScale = debugFontExtraScale,
    debugFontColor = debugFontColor,
    stageBuilder = stageBuilder,
    unit = Unit,
    targetFps = targetFps,
).start(entry)

data class Korge(
    val args: Array<String> = arrayOf(),
    val imageFormats: ImageFormat = RegisteredImageFormats,
    val gameWindow: GameWindow? = null,
    //val eventDispatcher: EventDispatcher = gameWindow ?: DummyEventDispatcher, // Removed
    val mainSceneClass: KClass<out Scene>? = null,
    val timeProvider: TimeProvider = TimeProvider,
    val injector: Injector = Injector(),
    val configInjector: Injector.() -> Unit = {},
    val debug: Boolean = false,
    val trace: Boolean = false,
    val context: Any? = null,
    val fullscreen: Boolean? = null,
    val blocking: Boolean = true,
    val gameId: String = DEFAULT_GAME_ID,
    val settingsFolder: String? = null,
    val batchMaxQuads: Int = BatchBuilder2D.DEFAULT_BATCH_QUADS,
    val windowSize: Size = DefaultViewport.SIZE,
    val virtualSize: Size = windowSize,
    val displayMode: KorgeDisplayMode = KorgeDisplayMode.DEFAULT,
    val title: String = "Game",
    val backgroundColor: RGBA? = Colors.BLACK,
    val quality: GameWindowQuality = GameWindowQuality.PERFORMANCE,
    val icon: String? = null,
    val multithreaded: Boolean? = null,
    val forceRenderEveryFrame: Boolean = true,
    val main: (suspend Stage.() -> Unit) = {},
    val debugAg: Boolean = false,
    val debugFontExtraScale: Double = 1.0,
    val debugFontColor: RGBA = Colors.WHITE,
    val stageBuilder: (Views) -> Stage = { Stage(it) },
    val targetFps: Double = 0.0,
    val unit: Unit = Unit,
) {
    companion object {
        val logger = Logger("Korge")
        val DEFAULT_GAME_ID = "korlibs.korge.unknown"
        val DEFAULT_WINDOW_SIZE: Size get() = DefaultViewport.SIZE
    }

    suspend fun start(entry: suspend Stage.() -> Unit = this.main) {
        if (!Platform.isJsBrowser) {
            configureLoggerFromProperties(localCurrentDirVfs["klogger.properties"])
        }
        val config = this
        val creationConfig = GameWindowCreationConfig(multithreaded = config.multithreaded, fullscreen = config.fullscreen, title = config.title)
        val gameWindow = (config.gameWindow ?: coroutineContext[GameWindow] ?: CreateDefaultGameWindow(creationConfig))
        gameWindow.configureKorge(config) {
            entry()
        }
    }
}

suspend fun Korge(entry: suspend Stage.() -> Unit) { Korge().start(entry) }

// @TODO: Doesn't compile on WASM: https://youtrack.jetbrains.com/issue/KT-58859/WASM-e-java.util.NoSuchElementException-Key-VALUEPARAMETER-namethis-typekorlibs.korge.Korge-korlibs.korge.KorgeConfig-is-missing
//suspend fun Korge(config: KorgeConfig, entry: suspend Stage.() -> Unit) { config.start(entry) }

suspend fun KorgeWithConfig(config: KorgeConfig, entry: suspend Stage.() -> Unit) { config.start(entry) }

data class KorgeArgs(val args: Array<String>)

/**
 * Configures a [GameWindow] to run a [Korge] application.
 **/
fun GameWindow.configureKorge(config: KorgeConfig = KorgeConfig(), block: suspend Stage.() -> Unit = {}) {
    val gameWindow = this
    val iconPath = config.icon
    val imageFormats = config.imageFormats
    val entry = config.main
    val multithreaded = config.multithreaded
    val windowSize = config.windowSize
    val views = Views(
        gameWindow = gameWindow,
        coroutineContext = gameWindow.coroutineDispatcher,
        //ag = if (config.debugAg) AGPrint() else gameWindow.ag,
        ag = gameWindow.ag,
        injector = config.injector,
        gameId = config.gameId,
        timeProvider = config.timeProvider,
        settingsFolder = config.settingsFolder,
        batchMaxQuads = config.batchMaxQuads,
        stageBuilder = config.stageBuilder
    ).also {
        if (Platform.isJsBrowser) {
            Dyn.global["views"] = it
        }
    }
    views.setVirtualSize(config.virtualSize)
    Korge.logger.logTime("configureGameWindow") {
        gameWindow.configure(windowSize, config.title, null, config.fullscreen, config.backgroundColor ?: Colors.BLACK)
    }
    gameWindow.quality = quality
    gameWindow.backgroundColor = config.backgroundColor ?: Colors.BLACK

    val injector = views.injector
    config.configInjector(injector)
    RegisteredImageFormats.register(config.imageFormats)
    injector.mapInstance(KorgeArgs(config.args))
    injector.mapInstance(KorgeConfig::class, config)
    views.debugFontExtraScale = config.debugFontExtraScale
    views.debugFontColor = config.debugFontColor
    views.virtualWidth = config.virtualSize.width.toInt()
    views.virtualHeight = config.virtualSize.height.toInt()
    views.scaleAnchor = config.displayMode.scaleAnchor
    views.scaleMode = config.displayMode.scaleMode
    views.clipBorders = config.displayMode.clipBorders
    views.targetFps = config.targetFps

    KorgeReload.registerEventDispatcher(gameWindow)
    @Suppress("OPT_IN_USAGE")
    views.prepareViewsBase(gameWindow, true, gameWindow.bgcolor, config.forceRenderEveryFrame, config.configInjector)

    gameWindow.queueSuspend {
        // @TODO: ResourcesVfs seems to require an access here? If ResourcesVfs is accessed in a scene, it is Cancelled later.
        resourcesVfs["klogger.properties"].exists()

        if (!Platform.isJsBrowser) {
            try {
                configureLoggerFromProperties(localCurrentDirVfs["klogger.properties"])
            } catch (e: Throwable) {

            }
        }

        Korge.logger.info { "Initializing..." }
        runCatching { views.init() }.exceptionOrNull()?.let { it.stackTraceToString() }

        // Initialize
        try {
            Korge.logger.logTime("setIcon") {
                try {
                    // Do nothing
                    when {
                        //iconDrawable != null -> this.icon = iconDrawable.render()
                        iconPath != null -> gameWindow.icon = resourcesVfs[iconPath].readBitmap(imageFormats)
                        else -> Unit
                    }
                } catch (e: Throwable) {
                    Korge.logger.error { "Couldn't get the application icon" }
                    e.printStackTrace()
                }
            }
        } finally {
            Korge.logger.info { "Initialized" }
        }

        config.main(views.stage)
        block(views.stage)
        if (config.mainSceneClass != null) {
            views.stage.sceneContainer().changeTo(config.mainSceneClass)
        }
    }
}
