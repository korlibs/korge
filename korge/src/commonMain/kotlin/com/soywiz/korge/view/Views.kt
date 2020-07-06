package com.soywiz.korge.view

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.klock.hr.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.log.*
import com.soywiz.korev.*
import com.soywiz.korge.*
import com.soywiz.korge.annotations.*
import com.soywiz.korge.input.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.render.*
import com.soywiz.korge.stat.*
import com.soywiz.korgw.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korinject.*
import com.soywiz.korio.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.stream.*
import com.soywiz.korma.geom.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.reflect.*

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
    val timeProvider: HRTimeProvider,
    val stats: Stats,
    val gameWindow: GameWindow
) : Extra by Extra.Mixin(), EventDispatcher by EventDispatcher.Mixin(), CoroutineScope, ViewsScope,
	BoundsProvider, DialogInterface by gameWindow, AsyncCloseable {

    val keys get() = input.keys

	var imageFormats = RegisteredImageFormats
	val renderContext = RenderContext(ag, this, stats, coroutineContext)
	val agBitmapTextureManager = renderContext.agBitmapTextureManager
	var clearEachFrame = true
	var clearColor: RGBA = Colors.BLACK
	override val views = this
	val propsTriggers = hashMapOf<String, (View, String, String) -> Unit>()
	var clampElapsedTimeTo = HRTimeSpan.fromMilliseconds(100.0)

    /** Native width in pixels (in retina displays this will be twice the window width). Use [virtualWidth] instead */
    @KorgeInternal
	val nativeWidth get() = ag.mainRenderBuffer.width
    /** Native height in pixels (in retina displays this will be twice the window height). Use [virtualHeight] instead */
    @KorgeInternal
	val nativeHeight get() = ag.mainRenderBuffer.height

    // Later updated
    /** The defined virtual width */
	var virtualWidth = DefaultViewport.WIDTH; internal set
    /** The defined virtual height */
	var virtualHeight = DefaultViewport.HEIGHT; internal set

    @KorgeExperimental
	var actualVirtualLeft = 0; private set
    @KorgeExperimental
	var actualVirtualTop = 0; private set

    @KorgeExperimental
	var actualVirtualWidth = DefaultViewport.WIDTH; private set
    @KorgeExperimental
	var actualVirtualHeight = DefaultViewport.HEIGHT; private set

    @KorgeExperimental
	override val virtualLeft get() = -actualVirtualLeft * views.stage.scaleX
    @KorgeExperimental
	override val virtualTop get() = -actualVirtualTop * views.stage.scaleY
    @KorgeExperimental
	override val virtualRight get() = virtualLeft + virtualWidth * views.stage.scaleX
    @KorgeExperimental
	override val virtualBottom get() = virtualTop + virtualHeight * views.stage.scaleY

    @KorgeExperimental
    val actualVirtualRight get() = actualVirtualWidth
    @KorgeExperimental
    val actualVirtualBottom get() = actualVirtualHeight

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

    @KorgeInternal
	override suspend fun close() {
		closeables.fastForEach { it.close() }
		closeables.clear()
		coroutineContext.cancel()
		gameWindow.close()
	}

    /** Mouse coordinates relative to the native window. Can't be used directly. Use [globalMouseX] instead */
    @KorgeInternal
	val nativeMouseX: Double get() = input.mouse.x
    /** Mouse coordinates relative to the native window. Can't be used directly. Use [globalMouseY] instead */
    @KorgeInternal
	val nativeMouseY: Double get() = input.mouse.y

    /** Mouse coordinates relative to the [Stage] singleton */
    val globalMouseXY get() = stage.mouseXY
    /** Mouse X coordinate relative to the [Stage] singleton */
    val globalMouseX get() = stage.mouseX
    /** Mouse Y coordinate relative to the [Stage] singleton */
    val globalMouseY get() = stage.mouseY

	var scaleMode: ScaleMode = ScaleMode.SHOW_ALL
	var scaleAnchor = Anchor.MIDDLE_CENTER
	var clipBorders = true

	private val resizedEvent = ReshapeEvent(0, 0)

    /** Reference to the root node [Stage] */
	val stage = Stage(this)

    /** Reference to the root node [Stage] (alias) */
	val root = stage

    var supportTogglingDebug = true
	var debugViews = false
	val debugHandlers = arrayListOf<Views.(RenderContext) -> Unit>()

	var lastTime = timeProvider.now()

    private val tempViews: ArrayList<View> = arrayListOf()
	private val virtualSize = SizeInt()
	private val actualSize = SizeInt()
	private val targetSize = SizeInt()

    @KorgeInternal
    val actualWidth get() = actualSize.width
    @KorgeInternal
    val actualHeight get() = actualSize.height

    val onBeforeRender = Signal<Unit>()
    val onAfterRender = Signal<Unit>()

    var targetFps: Double = -1.0

	init {
		injector.mapInstance(CoroutineContext::class, coroutineContext)
		injector.mapInstance(AG::class, ag)
		injector.mapInstance(Views::class, this)
        onAfterRender {
            renderContext.flush()
            renderContext.finish()
            agBitmapTextureManager.afterRender()
        }
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

	@Suppress("EXPERIMENTAL_API_USAGE")
    override fun <T : Event> dispatch(clazz: KClass<T>, event: T) {
		val e = event
		try {
			this.stage.dispatch(clazz, event)
            val stagedViews = getAllDescendantViews(stage, tempViews, true)
			when (e) {
				is MouseEvent -> stagedViews.fastForEach { it._components?.mouse?.fastForEach { it.onMouseEvent(views, e) } }
                is TouchEvent -> stagedViews.fastForEach { it._components?.touch?.fastForEach { it.onTouchEvent(views, e) } }
				is ReshapeEvent -> stagedViews.fastForEach { it._components?.resize?.fastForEach { it.resized(views, e.width, e.height) } }
				is KeyEvent -> {
                    input.triggerOldKeyEvent(e)
                    input.keys.triggerKeyEvent(e)
                    if ((e.type == KeyEvent.Type.UP) && supportTogglingDebug && (e.key == Key.F12 || e.key == Key.F7)) {
                        debugViews = !debugViews
                    }
                    stagedViews.fastForEach { it._components?.key?.fastForEach { it.onKeyEvent(views, e) } }
                }
				is GamePadConnectionEvent -> stagedViews.fastForEach { it._components?.gamepad?.fastForEach { it.onGamepadEvent(views, e) } }
				is GamePadUpdateEvent -> stagedViews.fastForEach { it._components?.gamepad?.fastForEach { it.onGamepadEvent(views, e) } }
				is GamePadButtonEvent -> stagedViews.fastForEach { it._components?.gamepad?.fastForEach { it.onGamepadEvent(views, e) } }
				is GamePadStickEvent -> stagedViews.fastForEach { it._components?.gamepad?.fastForEach { it.onGamepadEvent(views, e) } }
                else -> stagedViews.fastForEach { it._components?.event?.fastForEach { it.onEvent(e) } }
			}
		} catch (e: PreventDefaultException) {
			//println("PreventDefaultException.Reason: ${e.reason}")
		}
	}

	fun render() {
        onBeforeRender()
		if (clearEachFrame) ag.clear(clearColor, stencil = 0, clearColor = true, clearStencil = true)
		stage.render(renderContext)

		if (debugViews) {
			debugHandlers.fastForEach { debugHandler ->
				this.debugHandler(renderContext)
			}
		}

        onAfterRender()
    }

	fun frameUpdateAndRender(fixedSizeStep: TimeSpan = TimeSpan.NULL) {
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
		if (fixedSizeStep != TimeSpan.NULL) {
			update(fixedSizeStep.hr)
		} else {
			update(adelta)
		}
		render()
	}

    @Deprecated("")
    fun update(dtMs: Int) {
        update(dtMs.hrMilliseconds)
    }

	fun update(elapsed: HRTimeSpan) {
		//println(this)
		//println("Update: $dtMs")
		input.startFrame(elapsed)
		stage.updateSingleViewWithViewsAll(this, elapsed, tempViews)
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
		val virtualWidth = virtualWidth
		val virtualHeight = virtualHeight
		val anchor = scaleAnchor

		virtualSize.setTo(virtualWidth, virtualHeight)

		scaleMode(virtualSize, actualSize, targetSize)

		val ratioX = targetSize.width.toDouble() / virtualWidth.toDouble()
		val ratioY = targetSize.height.toDouble() / virtualHeight.toDouble()

		actualVirtualWidth = (actualSize.width / ratioX).toIntRound()
		actualVirtualHeight = (actualSize.height / ratioY).toIntRound()

        // @TODO: Create a parent to stage that is "invisible" in code but that affect the matrix so we don't adjust stage stuff?
		stage.scaleX = ratioX
		stage.scaleY = ratioY

		stage.x = (((actualVirtualWidth - virtualWidth) * anchor.sx) * ratioX).toIntRound().toDouble()
		stage.y = (((actualVirtualHeight - virtualHeight) * anchor.sy) * ratioY).toIntRound().toDouble()

		actualVirtualLeft = -(stage.x / ratioX).toIntRound()
		actualVirtualTop = -(stage.y / ratioY).toIntRound()

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
}

fun viewsLog(callback: suspend Stage.(log: ViewsLog) -> Unit) = Korio {
	val log = ViewsLog(coroutineContext)
	callback(log.views.stage, log)
}

open class GameWindowLog : GameWindow() {
}

class ViewsLog(
	override val coroutineContext: CoroutineContext,
	val injector: AsyncInjector = AsyncInjector(),
	val ag: AG = LogAG(),
	val input: Input = Input(),
	val timeProvider: HRTimeProvider = HRTimeProvider,
	val stats: Stats = Stats(),
	val gameWindow: GameWindow = GameWindowLog()
) : CoroutineScope {
	val views = Views(coroutineContext + AsyncInjectorContext(injector), ag, injector, input, timeProvider, stats, gameWindow)
}

fun Views.texture(bmp: Bitmap, mipmaps: Boolean = false): Texture =
	Texture(Texture.Base(ag.createTexture(bmp, mipmaps), bmp.width, bmp.height))

fun Views.texture(bmp: BitmapSlice<Bitmap>, mipmaps: Boolean = false): Texture =
	Texture(Texture.Base(ag.createTexture(bmp, mipmaps), bmp.width, bmp.height))

fun Bitmap.texture(views: Views, mipmaps: Boolean = false) = views.texture(this, mipmaps)

fun Views.texture(width: Int, height: Int, mipmaps: Boolean = false) =
	texture(Bitmap32(width, height), mipmaps)

suspend fun Views.texture(bmp: ByteArray, mipmaps: Boolean = false): Texture =
	texture(nativeImageFormatProvider.decode(bmp), mipmaps)

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
fun getAllDescendantViews(view: View, out: ArrayList<View> = ArrayList(), reversed: Boolean = true): ArrayList<View> {
    out.clear()
    val pos = getAllDescendantViewsBase(view, out, reversed, 0)
    while (out.size > pos) out.removeAt(out.size - 1)
    return out
}

private fun <T> ArrayList<T>.replaceOrAdd(pos: Int, value: T) {
    if (pos >= this.size) add(value) else this[pos] = value
}

private fun getAllDescendantViewsBase(view: View, out: ArrayList<View>, reversed: Boolean, cursor: Int): Int {
    var pos = cursor
    if (reversed) {
        view.forEachChildrenReversed { pos = getAllDescendantViewsBase(it, out, reversed, pos) }
        out.replaceOrAdd(pos++, view)
    } else {
        out.replaceOrAdd(pos++, view)
        view.forEachChildren { pos = getAllDescendantViewsBase(it, out, reversed, pos) }
    }
    return pos
}

@OptIn(KorgeInternal::class)
fun View.updateSingleView(dtMsD: Double, tempViews: ArrayList<View> = arrayListOf()) {
    getAllDescendantViews(this, tempViews).fastForEach { view ->
        view._components?.update?.fastForEach { comp ->
            comp.update(dtMsD * view.globalSpeed)
        }
    }
}

@Deprecated("")
@OptIn(KorgeInternal::class)
fun View.updateSingleViewWithViews(views: Views, dtMsD: Double, tempViews: ArrayList<View> = arrayListOf()) {
    getAllDescendantViews(this, tempViews).fastForEach { view ->
        view._components?.updateWV?.fastForEach { comp ->
            comp.update(views, dtMsD * view.globalSpeed)
        }
    }
}

@OptIn(KorgeInternal::class)
fun View.updateSingleViewWithViewsAll(
    views: Views,
    delta: HRTimeSpan,
    tempViews: ArrayList<View> = arrayListOf()
) {
    getAllDescendantViews(this, tempViews).fastForEach { view ->
        view._components?.updateWV?.fastForEach { comp -> comp.update(views, delta * view.globalSpeed) }
        view._components?.update?.fastForEach { comp -> comp.update(delta * view.globalSpeed) }
    }
    //updateSingleView(dtMsD, tempComponents)
    //updateSingleViewWithViews(views, dtMsD, tempComponents)
}

interface BoundsProvider {
    val virtualLeft: Double
    val virtualTop: Double
    val virtualRight: Double
    val virtualBottom: Double

    object Dummy : BoundsProvider {
        override val virtualLeft: Double = 0.0
        override val virtualTop: Double = 0.0
        override val virtualRight: Double = 0.0
        override val virtualBottom: Double = 0.0
    }
}

interface ViewsScope {
    val views: Views
}
