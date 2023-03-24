package korlibs.korge.view

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.time.*
import korlibs.memory.*
import korlibs.graphics.*
import korlibs.graphics.log.*
import korlibs.event.*
import korlibs.korge.*
import korlibs.korge.annotations.*
import korlibs.korge.bitmapfont.*
import korlibs.korge.input.*
import korlibs.korge.internal.*
import korlibs.korge.render.*
import korlibs.korge.stat.*
import korlibs.render.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.format.*
import korlibs.inject.*
import korlibs.io.*
import korlibs.io.async.*
import korlibs.io.file.*
import korlibs.io.file.std.*
import korlibs.io.lang.*
import korlibs.io.resources.*
import korlibs.io.stream.*
import korlibs.math.geom.*
import kotlinx.coroutines.*
import kotlin.collections.set
import kotlin.coroutines.*

//@Singleton
/**
 * Heavyweight singleton object within the application that contains information about the Views.
 * It contains information about the [coroutineContext], the [gameWindow], the [injector], the [input]
 * and contains a reference to the [root] [Stage] view.
 */
class Views constructor(
    override val coroutineContext: CoroutineContext,
    val ag: AG,
    val injector: AsyncInjector,
    val input: Input,
    val timeProvider: TimeProvider,
    val stats: Stats,
    val gameWindow: GameWindow,
    val gameId: String = "korgegame",
    val settingsFolder: String? = null,
    val batchMaxQuads: Int = BatchBuilder2D.DEFAULT_BATCH_QUADS,
    val bp: BoundsProvider = BoundsProvider.Base(),
    val stageBuilder: (Views) -> Stage = { Stage(it) }
) : BaseEventListener(),
    Extra by Extra.Mixin(),
    CoroutineScope, ViewsContainer,
	BoundsProvider by bp,
    DialogInterfaceProvider by gameWindow,
    Closeable,
    ResourcesContainer,
    InvalidateNotifier,
    DeviceDimensionsProvider by gameWindow
{
    override val views = this

    var rethrowRenderError = false
    var forceRenderEveryFrame: Boolean by gameWindow::continuousRenderMode

    val virtualPixelsPerInch: Double get() = pixelsPerInch / globalToWindowScaleAvg
    val virtualPixelsPerCm: Double get() = virtualPixelsPerInch / DeviceDimensionsProvider.INCH_TO_CM

    internal val resizedEvent = ReshapeEvent(0, 0)
    internal val updateEvent = UpdateEvent()
    internal val viewsUpdateEvent = ViewsUpdateEvent(this)
    internal val viewsResizedEvent = ViewsResizedEvent(this)

    val keys get() = input.keys

    val gameIdFolder get() = gameId.replace("\\", "").replace("/", "").replace("..", "")

    val realSettingsFolder: String by lazy {
        when {
            settingsFolder != null -> settingsFolder!!
            else -> when {
                Platform.isMac -> "/Users/${Environment["USER"]}/Library/Preferences/$gameIdFolder"
                Platform.isWindows -> "${Environment["APPDATA"]}/$gameIdFolder"
                else -> "${Environment["HOME"]}/.config/$gameIdFolder"
            }
        }
    }

    lateinit var debugBmpFont: BitmapFont
        private set

    suspend fun init() {
        debugBmpFont = debugBmpFont()
    }

    var name: String? = null
    var currentVfs: VfsFile = resourcesVfs
    var imageFormats = RegisteredImageFormats
	val renderContext = RenderContext(ag, this, gameWindow, stats, coroutineContext, batchMaxQuads)
	@Deprecated("") val agBitmapTextureManager get() = renderContext.agBitmapTextureManager
    @Deprecated("") val agBufferManager get() = renderContext.agBufferManager
	var clearEachFrame = true
	var clearColor: RGBA = Colors.BLACK
	val propsTriggers = hashMapOf<String, (View, String, String) -> Unit>()
	var clampElapsedTimeTo = 100.0.milliseconds

    override val resources: Resources = Resources(coroutineContext, currentVfs)

    val globalResources: Resources get() = resources

    var editingMode: Boolean = false

    /** Native width in pixels (in retina displays this will be twice the window width). Use [virtualWidth] instead */
    @KorgeInternal
	val nativeWidth get() = ag.mainFrameBuffer.width
    /** Native height in pixels (in retina displays this will be twice the window height). Use [virtualHeight] instead */
    @KorgeInternal
	val nativeHeight get() = ag.mainFrameBuffer.height

    // Later updated
    /** The defined virtual width */
	var virtualWidth: Int = DefaultViewport.WIDTH; internal set
    /** The defined virtual height */
	var virtualHeight: Int = DefaultViewport.HEIGHT; internal set

    var virtualWidthDouble: Double
        get() = virtualWidth.toDouble()
        set(value) { virtualWidth = value.toInt() }
    var virtualHeightDouble: Double
        get() = virtualHeight.toDouble()
        set(value) { virtualHeight = value.toInt() }

	private val closeables = arrayListOf<AsyncCloseable>()

    /**
     * Adds a [callback] to be executed when the game is closed in normal circumstances.
     */
	fun onClose(callback: suspend () -> Unit) {
		closeables += object : AsyncCloseable {
			override suspend fun close() {
				callback()
			}
		}
	}

    //@KorgeInternal
	override fun close() {
        launchImmediately {
            closeSuspend()
        }
	}

    suspend fun closeSuspend() {
        KorgeReload.unregisterEventDispatcher()
        closeables.fastForEach { it.close() }
        closeables.clear()
        coroutineContext.cancel()
        gameWindow.close()
    }

    /** Mouse coordinates relative to the native window. Can't be used directly. Use [globalMousePos] instead */
    @KorgeInternal
    val windowMousePos: Point get() = bp.globalToWindowCoords(input.mousePos.mutable).point

    /** Mouse coordinates relative to the [Stage] singleton */
    val globalMousePos get() = stage.mousePos

	var scaleMode: ScaleMode = ScaleMode.SHOW_ALL
	var scaleAnchor = Anchor.MIDDLE_CENTER
	var clipBorders = true

    /** Reference to the root node [Stage] */
	val stage: Stage = stageBuilder(this)

    /** Reference to the root node [Stage] (alias) */
	val root = stage

    var supportTogglingDebug = true
	var debugViews = false
	var debugFontExtraScale by renderContext::debugExtraFontScale
	var debugFontColor by renderContext::debugExtraFontColor
	val debugHandlers = FastArrayList<Views.(RenderContext) -> Unit>()

    fun addDebugRenderer(block: Views.(RenderContext) -> Unit) {
        debugHandlers.add(block)
    }

	var lastTime = timeProvider.now()

    private val tempViewsPool = Pool { FastArrayList<View>() }
    //private val tempViews = FastArrayList<View>()
	private val virtualSize = MSizeInt()
	private val actualSize = MSizeInt()
	private val targetSize = MSizeInt()

    @KorgeInternal
    val actualWidth get() = actualSize.width
    @KorgeInternal
    val actualHeight get() = actualSize.height

    val onBeforeRender = Signal<RenderContext>()
    val onAfterRender = Signal<RenderContext>()

    var targetFps: Double = -1.0

	init {
		injector.mapInstance(CoroutineContext::class, coroutineContext)
		injector.mapInstance(AG::class, ag)
		injector.mapInstance(Views::class, this)
        onAfterRender {
            renderContext.afterRender()
        }
        onBeforeRender {
            renderContext.beforeRender()
        }
        installFpsDebugOverlay()
    }

	fun dumpStats() {
		stats.dump()
	}

	fun registerPropertyTrigger(propName: String, gen: (View, String, String) -> Unit) {
		propsTriggers[propName] = gen
	}

	fun registerPropertyTriggerSuspend(propName: String, gen: suspend (View, String, String) -> Unit) {
		propsTriggers[propName] = { view, key, value ->
			launchImmediately(coroutineContext) {
				gen(view, key, value)
			}
		}
	}

	fun setVirtualSize(width: Int, height: Int) {
		this.virtualWidth = width
		this.virtualHeight = height
		resized()
	}

    override fun <T : BEvent> dispatch(event: T) {
        super.dispatch(event)
        val e = event
        e.target = views
        // @TODO: Remove this
        if (e is KeyEvent) {
            input.triggerOldKeyEvent(e)
            input.keys.triggerKeyEvent(e)
            if ((e.type == KeyEvent.Type.UP) && supportTogglingDebug && (e.key == Key.F12 || e.key == Key.F7)) {
                debugViews = !debugViews
                gameWindow.debug = debugViews
                invalidatedView(stage)
            }
        }
        try {
            stage.dispatch(e)
        } catch (e: PreventDefaultException) {
            //println("PreventDefaultException.Reason: ${e.reason}")
        }
    }

	fun render() {
        ag.startFrame()
		if (clearEachFrame) renderContext.clear(clearColor, stencil = 0, depth = 1f, clearColor = true, clearStencil = true, clearDepth = true)
        onBeforeRender(renderContext)
        renderContext.flush()
		stage.render(renderContext)
        renderContext.flush()
        stage.renderDebug(renderContext)

		if (debugViews) {
            //renderContext.setTemporalProjectionMatrixTransform(Matrix()) {
            run {
                debugHandlers.fastForEach { debugHandler ->
                    this.debugHandler(renderContext)
                }
            }
		}

        onAfterRender(renderContext)
        renderContext.flush()
    }

	fun frameUpdateAndRender(
        fixedSizeStep: TimeSpan = TimeSpan.NIL,
        forceRender: Boolean = false,
        doUpdate: Boolean = true,
        doRender: Boolean = true,
    ) {
        val currentTime = timeProvider.now()
		views.stats.startFrame()
		Korge.logger.trace { "ag.onRender" }
		//println("Render")
		//println("currentTime: $currentTime")
		val delta = (currentTime - lastTime)
		val adelta = if (delta > views.clampElapsedTimeTo) views.clampElapsedTimeTo else delta
		//println("delta: $delta")
		//println("Render($lastTime -> $currentTime): $delta")
		lastTime = currentTime
        if (doUpdate) {
            if (fixedSizeStep != TimeSpan.NIL) {
                update(fixedSizeStep)
            } else {
                update(adelta)
            }
        }
        val doRender2 = doRender && (forceRender || updatedSinceFrame > 0)
        if (doRender2) {
            if (printRendering) {
                println("Views.frameUpdateAndRender[${DateTime.nowUnixMillisLong()}]: doRender=$doRender2 -> [forceRender=$forceRender, updatedSinceFrame=$updatedSinceFrame]")
            }
            render()
            startFrame()
        }
	}

    //var printRendering: Boolean = true
    var printRendering: Boolean = Environment["SHOW_FRAME_UPDATE_AND_RENDER"] == "true"

    private val eventResults = EventResult()

	fun update(elapsed: TimeSpan) {
		//println(this)
		//println("Update: $elapsed")
		input.startFrame(elapsed)
        eventResults.reset()
        stage.updateSingleViewWithViewsAll(this, elapsed)
        //println("Views.update:eventResults=$eventResults")
		input.endFrame(elapsed)
	}

	fun mouseUpdated() {
		//println("localMouse: (${stage.localMouseX}, ${stage.localMouseY}), inputMouse: (${input.mouse.x}, ${input.mouse.y})")
	}

	fun resized(width: Int, height: Int) {
		val actualWidth = width
		val actualHeight = height
		//println("RESIZED: $width, $height")
		actualSize.setTo(actualWidth, actualHeight)
		resized()
	}


	fun resized() {
		//println("$e : ${views.ag.backWidth}x${views.ag.backHeight}")
        bp.setBoundsInfo(
            virtualWidth,
            virtualHeight,
            actualSize,
            scaleMode,
            scaleAnchor,
            virtualSize,
            targetSize
        )

        //println("RESIZED: $virtualSize, $actualSize, $targetSize")

        renderContext.projectionMatrixTransform.copyFrom(bp.globalToWindowMatrix)
        renderContext.projectionMatrixTransformInv.copyFrom(bp.windowToGlobalMatrix)

        //println("virtualSize=$virtualSize, targetSize=$targetSize, actualVirtualBounds=${actualVirtualBounds}")

        resizedEvent.apply {
            this.width = actualSize.width
            this.height = actualSize.height
        }

		stage.dispatch(resizedEvent)
        dispatch(resizedEvent)

		stage.invalidate()

		//println("STAGE RESIZED: $this, virtualSize=$virtualSize, actualSize=$actualSize, targetSize=$targetSize, actualVirtualWidth=$actualVirtualWidth")
	}

	fun dispose() {
	}

    /*
    @KorgeInternal
    fun getWindowBounds(view: View, out: Rectangle = Rectangle()): Rectangle {
        val bounds = view.getGlobalBounds(out)
        return bounds.setBounds(
            globalToWindowX(bounds.left, bounds.top),
            globalToWindowY(bounds.left, bounds.top),
            globalToWindowX(bounds.right, bounds.bottom),
            globalToWindowY(bounds.right, bounds.bottom)
        )
    }

    /** Transform global coordinates [x] and [y] into coordinates in the window space X */
    //internal fun globalToWindowX(x: Double, y: Double): Double = stage.localMatrix.transformX(x, y)
    internal fun globalToWindowX(x: Double, y: Double): Double = x
    /** Transform global coordinates [x] and [y] into coordinates in the window space Y */
    //internal fun globalToWindowY(x: Double, y: Double): Double = stage.localMatrix.transformY(x, y)
    internal fun globalToWindowY(x: Double, y: Double): Double = y
    */

    val debugHighlighters = Signal<View?>()

    fun debugHightlightView(viewToHightlight: View?, onlyIfDebuggerOpened: Boolean = false) {
        if (onlyIfDebuggerOpened && !gameWindow.debug) return
        println("debugHightlightView: $viewToHightlight")
        debugHighlighters(viewToHightlight)
    }

    data class SaveEvent(val action: String, val view: View?) {
        override fun toString(): String = buildString {
            append(action)
            if (view != null) {
                append(" ")
                append(if (view.name != null) view.name else "#${view.index}")
                append(" (${view::class.simpleName})")
            }
        }
    }

    val debugSavedHandlers = Signal<SaveEvent>()
    val completedEditing = Signal<Unit>()

    fun debugSaveView(e: SaveEvent) {
        debugSavedHandlers(e)
    }

    fun debugSaveView(action: String, view: View?) {
        debugSavedHandlers(SaveEvent(action, view))
    }

    fun <T : View?> undoable(action: String, view: T, block: (T) -> Unit) {
        block(view)
        debugSaveView(action, view)
    }

    var updatedSinceFrame: Int by gameWindow::updatedSinceFrame

    fun startFrame() {
        gameWindow.startFrame()
    }

    override fun invalidatedView(view: BaseView?) {
        //println("invalidatedView: $view")
        gameWindow.invalidatedView()
    }

    //var viewExtraBuildDebugComponent = arrayListOf<(views: Views, view: View, container: UiContainer) -> Unit>()
}

fun viewsLog(callback: suspend Stage.(log: ViewsLog) -> Unit) = Korio {
    viewsLogSuspend(callback)
}

suspend fun viewsLogSuspend(callback: suspend Stage.(log: ViewsLog) -> Unit) {
    val log = ViewsLog(coroutineContext).also { it.init() }
    callback(log.views.stage, log)
}

open class GameWindowLog : GameWindow() {
}

class ViewsLog constructor(
	override val coroutineContext: CoroutineContext,
	val injector: AsyncInjector = AsyncInjector(),
	val ag: AG = AGLog(),
	val input: Input = Input(),
	val timeProvider: TimeProvider = TimeProvider,
	val stats: Stats = Stats(),
	val gameWindow: GameWindow = GameWindowLog()
) : CoroutineScope {
	val views = Views(coroutineContext + AsyncInjectorContext(injector), ag, injector, input, timeProvider, stats, gameWindow).also {
	    it.rethrowRenderError = true
    }
    val stage: Stage get() = views.stage
    private var initialized = false
    suspend fun init() {
        if (!initialized) {
            initialized = true
            RegisteredImageFormats.register(QOI, PNG) // This might be required for Node.JS debug bitmap font in tests
            views.init()
        }
    }
}

interface ViewsContainer {
	val views: Views
}

val ViewsContainer.ag: AG get() = views.ag

data class KorgeFileLoaderTester<T>(
	val name: String,
	val tester: suspend (s: FastByteArrayInputStream, injector: AsyncInjector) -> KorgeFileLoader<T>?
) {
	suspend operator fun invoke(s: FastByteArrayInputStream, injector: AsyncInjector) = tester(s, injector)
	override fun toString(): String = "KorgeFileTester(\"$name\")"
}

data class KorgeFileLoader<T>(val name: String, val loader: suspend VfsFile.(FastByteArrayInputStream, Views) -> T) {
	override fun toString(): String = "KorgeFileLoader(\"$name\")"
}

//suspend val AsyncInjector.views: Views get() = this.get<Views>()

/////////////////////////
/////////////////////////

@OptIn(KorgeInternal::class)
fun getAllDescendantViews(view: View, out: FastArrayList<View> = FastArrayList(), reversed: Boolean = true): FastArrayList<View> {
    out.clear()
    val pos = getAllDescendantViewsBase(view, out, reversed, 0)
    while (out.size > pos) out.removeAt(out.size - 1)
    return out
}

private fun <T> FastArrayList<T>.replaceOrAdd(pos: Int, value: T) {
    if (pos >= this.size) add(value) else this[pos] = value
}

private fun getAllDescendantViewsBase(view: View, out: FastArrayList<View>, reversed: Boolean, cursor: Int): Int {
    var pos = cursor
    if (reversed) {
        view.forEachChildReversed { pos = getAllDescendantViewsBase(it, out, reversed, pos) }
        out.replaceOrAdd(pos++, view)
    } else {
        out.replaceOrAdd(pos++, view)
        view.forEachChild { pos = getAllDescendantViewsBase(it, out, reversed, pos) }
    }
    return pos
}

@OptIn(KorgeInternal::class)
fun View.updateSingleView(delta: TimeSpan, tempUpdate: UpdateEvent = UpdateEvent()) {
    dispatch(tempUpdate.also { it.deltaTime = delta })
}

//@OptIn(KorgeInternal::class)
//fun View.updateSingleViewWithViews(views: Views, dtMsD: Double, tempViews: ArrayList<View> = arrayListOf()) {
//    getAllDescendantViews(this, tempViews).fastForEach { view ->
//        view._components?.updateWV?.fastForEach { comp ->
//            comp.update(views, (dtMsD * view.globalSpeed).milliseconds)
//        }
//    }
//}

@OptIn(KorgeInternal::class)
fun View.updateSingleViewWithViewsAll(
    views: Views,
    delta: TimeSpan,
) {
    dispatch(views.updateEvent.also { it.deltaTime = delta })
    dispatch(views.viewsUpdateEvent.also { it.delta = delta })
}

interface BoundsProvider {
    val windowToGlobalMatrix: MMatrix
    var windowToGlobalTransform: MatrixTransform
    val globalToWindowMatrix: MMatrix
    var globalToWindowTransform: MatrixTransform
    val actualVirtualBounds: MRectangle

    @KorgeExperimental val actualVirtualLeft: Int get() = actualVirtualBounds.left.toIntRound()
    @KorgeExperimental val actualVirtualTop: Int get() = actualVirtualBounds.top.toIntRound()
    @KorgeExperimental val actualVirtualWidth: Int get() = actualVirtualBounds.width.toIntRound()
    @KorgeExperimental val actualVirtualHeight: Int get() = actualVirtualBounds.height.toIntRound()
    //@KorgeExperimental var actualVirtualWidth = DefaultViewport.WIDTH; private set
    //@KorgeExperimental var actualVirtualHeight = DefaultViewport.HEIGHT; private set

    val virtualLeft: Double get() = actualVirtualBounds.left
    val virtualTop: Double get() = actualVirtualBounds.top
    val virtualRight: Double get() = actualVirtualBounds.right
    val virtualBottom: Double get() = actualVirtualBounds.bottom

    @KorgeExperimental
    val actualVirtualRight: Double get() = actualVirtualBounds.right
    @KorgeExperimental
    val actualVirtualBottom: Double get() = actualVirtualBounds.bottom

    fun globalToWindowBounds(bounds: MRectangle, out: MRectangle = MRectangle()): MRectangle =
        out.copyFrom(bounds).applyTransform(globalToWindowMatrix)

    val windowToGlobalScale: Scale get() = windowToGlobalTransform.scale
    val windowToGlobalScaleX: Double get() = windowToGlobalTransform.scale.scaleXD
    val windowToGlobalScaleY: Double get() = windowToGlobalTransform.scale.scaleYD
    val windowToGlobalScaleAvg: Double get() = windowToGlobalTransform.scale.scaleAvgD

    val globalToWindowScale: Scale get() = globalToWindowTransform.scale
    val globalToWindowScaleX: Double get() = globalToWindowTransform.scaleX.toDouble()
    val globalToWindowScaleY: Double get() = globalToWindowTransform.scaleY.toDouble()
    val globalToWindowScaleAvg: Double get() = globalToWindowTransform.scaleAvg.toDouble()

    fun windowToGlobalCoords(pos: MPoint, out: MPoint = MPoint()): MPoint = windowToGlobalMatrix.transform(pos, out)
    fun windowToGlobalCoords(x: Double, y: Double, out: MPoint = MPoint()): MPoint = windowToGlobalMatrix.transform(x, y, out)
    fun windowToGlobalCoordsX(x: Double, y: Double): Double = windowToGlobalMatrix.transformX(x, y)
    fun windowToGlobalCoordsY(x: Double, y: Double): Double = windowToGlobalMatrix.transformY(x, y)
    fun windowToGlobalCoordsX(pos: MPoint): Double = windowToGlobalCoordsX(pos.x, pos.y)
    fun windowToGlobalCoordsY(pos: MPoint): Double = windowToGlobalCoordsY(pos.x, pos.y)

    fun globalToWindowCoords(pos: MPoint, out: MPoint = MPoint()): MPoint = globalToWindowMatrix.transform(pos, out)
    fun globalToWindowCoords(x: Double, y: Double, out: MPoint = MPoint()): MPoint = globalToWindowMatrix.transform(x, y, out)
    fun globalToWindowCoordsX(x: Double, y: Double): Double = globalToWindowMatrix.transformX(x, y)
    fun globalToWindowCoordsY(x: Double, y: Double): Double = globalToWindowMatrix.transformY(x, y)
    fun globalToWindowCoordsX(pos: MPoint): Double = globalToWindowCoordsX(pos.x, pos.y)
    fun globalToWindowCoordsY(pos: MPoint): Double = globalToWindowCoordsY(pos.x, pos.y)

    open class Base : BoundsProvider {
        override val windowToGlobalMatrix: MMatrix = MMatrix()
        override var windowToGlobalTransform: MatrixTransform = MatrixTransform()
        override val globalToWindowMatrix: MMatrix = MMatrix()
        override var globalToWindowTransform: MatrixTransform = MatrixTransform()
        override val actualVirtualBounds: MRectangle = MRectangle(0, 0, DefaultViewport.WIDTH, DefaultViewport.HEIGHT)
    }
}

fun BoundsProvider.setBoundsInfo(
    virtualWidth: Int,
    virtualHeight: Int,
    actualSize: MSizeInt,
    scaleMode: ScaleMode = ScaleMode.FILL,
    anchor: Anchor = Anchor.CENTER,
    virtualSize: MSizeInt = MSizeInt(),
    targetSize: MSizeInt = MSizeInt()
) {
    virtualSize.setTo(virtualWidth, virtualHeight)
    scaleMode(virtualSize, actualSize, targetSize)

    val ratioX = targetSize.width.toDouble() / virtualWidth.toDouble()
    val ratioY = targetSize.height.toDouble() / virtualHeight.toDouble()
    val actualVirtualWidth = (actualSize.width / ratioX).toIntRound()
    val actualVirtualHeight = (actualSize.height / ratioY).toIntRound()

    globalToWindowMatrix.identity()
    globalToWindowMatrix.prescale(ratioX, ratioY)
    globalToWindowMatrix.pretranslate(
        ((actualVirtualWidth - virtualWidth) * anchor.doubleX).toIntRound().toDouble(),
        ((actualVirtualHeight - virtualHeight) * anchor.doubleY).toIntRound().toDouble(),
    )
    windowToGlobalMatrix.invert(globalToWindowMatrix)
    globalToWindowTransform = globalToWindowMatrix.immutable.toTransform()
    windowToGlobalTransform = windowToGlobalMatrix.immutable.toTransform()

    val tl = windowToGlobalCoords(0.0, 0.0)
    val br = windowToGlobalCoords(actualSize.width.toDouble(), actualSize.height.toDouble())
    actualVirtualBounds.setToBounds(tl.x, tl.y, br.x, br.y)
}

suspend fun views(): Views = injector().get()

class UpdateEvent(var deltaTime: TimeSpan = TimeSpan.ZERO) : Event(), TEvent<UpdateEvent> {
    companion object : EventType<UpdateEvent>
    override val type: EventType<UpdateEvent> get() = UpdateEvent

    fun copyFrom(other: UpdateEvent) {
        this.deltaTime = other.deltaTime
    }

    override fun toString(): String = "UpdateEvent(time=$deltaTime)"
}

class ViewsUpdateEvent(val views: Views, var delta: TimeSpan = TimeSpan.ZERO) : Event(), TEvent<ViewsUpdateEvent> {
    companion object : EventType<ViewsUpdateEvent>
    override val type: EventType<ViewsUpdateEvent> get() = ViewsUpdateEvent

    fun copyFrom(other: ViewsUpdateEvent) {
        this.delta = other.delta
    }

    override fun toString(): String = "ViewsUpdateEvent(time=$delta)"
}

class ViewsResizedEvent(val views: Views, var size: SizeInt = SizeInt(0, 0)) : Event(), TEvent<ViewsResizedEvent> {
    companion object : EventType<ViewsResizedEvent>
    override val type: EventType<ViewsResizedEvent> get() = ViewsResizedEvent

    fun copyFrom(other: ViewsResizedEvent) {
        this.size = other.size
    }

    override fun toString(): String = "ViewsResizedEvent(size=$size)"
}