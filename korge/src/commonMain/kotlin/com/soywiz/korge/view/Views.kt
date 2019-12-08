package com.soywiz.korge.view

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.klogger.*
import com.soywiz.korag.*
import com.soywiz.korag.log.*
import com.soywiz.korev.*
import com.soywiz.korge.*
import com.soywiz.korge.component.*
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
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.stream.*
import com.soywiz.korma.geom.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.math.*
import kotlin.reflect.*

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

//@Singleton
class Views constructor(
	override val coroutineContext: CoroutineContext,
	val ag: AG,
	val injector: AsyncInjector,
	val input: Input,
	val timeProvider: TimeProvider,
	val stats: Stats,
	val gameWindow: GameWindow
) : Updatable, Extra by Extra.Mixin(), EventDispatcher by EventDispatcher.Mixin(), CoroutineScope, ViewsScope,
	BoundsProvider, DialogInterface by gameWindow, AsyncCloseable {

	var imageFormats = RegisteredImageFormats
	val renderContext = RenderContext(ag, this, stats, coroutineContext)
	val agBitmapTextureManager = renderContext.agBitmapTextureManager
	var clearEachFrame = true
	var clearColor: RGBA = Colors.BLACK
	override val views = this
	val propsTriggers = hashMapOf<String, (View, String, String) -> Unit>()
	var clampElapsedTimeTo = 100

	val nativeWidth get() = ag.mainRenderBuffer.width
	val nativeHeight get() = ag.mainRenderBuffer.height

	var virtualWidth = DefaultViewport.WIDTH; internal set
	var virtualHeight = DefaultViewport.HEIGHT; internal set

	var actualVirtualLeft = 0; private set
	var actualVirtualTop = 0; private set

	var actualVirtualWidth = DefaultViewport.WIDTH; private set
	var actualVirtualHeight = DefaultViewport.HEIGHT; private set

	override val virtualLeft get() = -actualVirtualLeft * views.stage.scaleX
	override val virtualTop get() = -actualVirtualTop * views.stage.scaleY
	override val virtualRight get() = virtualLeft + virtualWidth * views.stage.scaleX
	override val virtualBottom get() = virtualTop + virtualHeight * views.stage.scaleY

	private val closeables = arrayListOf<AsyncCloseable>()

	fun onClose(callback: suspend () -> Unit) {
		closeables += object : AsyncCloseable {
			override suspend fun close() {
				callback()
			}
		}
	}

	override suspend fun close() {
		closeables.fastForEach { it.close() }
		closeables.clear()
		coroutineContext.cancel()
		gameWindow.close()
	}

	val actualVirtualRight get() = actualVirtualWidth
	val actualVirtualBottom get() = actualVirtualHeight

	val nativeMouseX: Double get() = input.mouse.x
	val nativeMouseY: Double get() = input.mouse.y

	var scaleMode: ScaleMode = ScaleMode.SHOW_ALL
	var scaleAnchor = Anchor.MIDDLE_CENTER
	var clipBorders = true

	private val resizedEvent = ReshapeEvent(0, 0)

	val stage = Stage(this)
	val root = stage
    var supportTogglingDebug = true
	var debugViews = false
	val debugHandlers = arrayListOf<Views.(RenderContext) -> Unit>()

	var lastTime = timeProvider.now()

	private val tempComponents: ArrayList<Component> = arrayListOf()

	private val virtualSize = SizeInt()
	private val actualSize = SizeInt()
	private val targetSize = SizeInt()

	var targetFps: Double = -1.0

	init {
		injector.mapInstance(CoroutineContext::class, coroutineContext)
		injector.mapInstance(AG::class, ag)
		injector.mapInstance(Views::class, this)
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

	override fun <T : Event> dispatch(clazz: KClass<T>, event: T) {
		val e = event
		try {
			this.stage.dispatch(clazz, event)
			stage.forEachComponent<EventComponent>(tempComponents) { it.onEvent(event) }
			when (e) {
				is MouseEvent -> stage.forEachComponent<MouseComponent>(tempComponents) { it.onMouseEvent(views, e) }
				is ReshapeEvent -> stage.forEachComponent<ResizeComponent>(tempComponents) {
					it.resized(views, e.width, e.height)
				}
				is KeyEvent -> stage.forEachComponent<KeyComponent>(tempComponents) { it.onKeyEvent(views, e) }
				is GamePadConnectionEvent -> stage.forEachComponent<GamepadComponent>(tempComponents) {
					it.onGamepadEvent(views, e)
				}
				is GamePadUpdateEvent -> stage.forEachComponent<GamepadComponent>(tempComponents) {
					it.onGamepadEvent(views, e)
				}
				is GamePadButtonEvent -> stage.forEachComponent<GamepadComponent>(tempComponents) {
					it.onGamepadEvent(views, e)
				}
				is GamePadStickEvent -> stage.forEachComponent<GamepadComponent>(tempComponents) {
					it.onGamepadEvent(views, e)
				}
			}
		} catch (e: PreventDefaultException) {
			//println("PreventDefaultException.Reason: ${e.reason}")
		}
	}

	val onBeforeRender = Signal<Unit>()

	fun render() {
		onBeforeRender()
		if (clearEachFrame) ag.clear(clearColor, stencil = 0, clearColor = true, clearStencil = true)
		stage.render(renderContext)

		if (debugViews) {
			debugHandlers.fastForEach { debugHandler ->
				this.debugHandler(renderContext)
			}
		}

		renderContext.flush()
		renderContext.finish()
		agBitmapTextureManager.afterRender()
	}

	fun frameUpdateAndRender(fixedSizeStep: TimeSpan = TimeSpan.NULL) {
		views.stats.startFrame()
		Korge.logger.trace { "ag.onRender" }
		//println("Render")
		val currentTime = timeProvider.now()
		//println("currentTime: $currentTime")
		val delta = (currentTime - lastTime).millisecondsInt
		val adelta = min(delta, views.clampElapsedTimeTo)
		//println("delta: $delta")
		//println("Render($lastTime -> $currentTime): $delta")
		lastTime = currentTime
		if (fixedSizeStep != TimeSpan.NULL) {
			update(fixedSizeStep.millisecondsInt)
		} else {
			update(adelta)
		}
		render()
	}

	override fun update(dtMs: Int) {
		//println(this)
		//println("Update: $dtMs")
		input.startFrame(dtMs)
		val dtMsD = dtMs.toDouble()
		stage.updateSingleView(dtMsD, tempComponents)
		stage.updateSingleViewWithViews(this, dtMsD, tempComponents)
		input.endFrame(dtMs)
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

		actualVirtualWidth = (actualSize.width / ratioX).toInt()
		actualVirtualHeight = (actualSize.height / ratioY).toInt()

		stage.scaleX = ratioX
		stage.scaleY = ratioY

		stage.x = (((actualVirtualWidth - virtualWidth) * anchor.sx) * ratioX).toInt().toDouble()
		stage.y = (((actualVirtualHeight - virtualHeight) * anchor.sy) * ratioY).toInt().toDouble()

		actualVirtualLeft = -(stage.x / ratioX).toInt()
		actualVirtualTop = -(stage.y / ratioY).toInt()

		stage.dispatch(resizedEvent.apply {
			this.width = actualSize.width
			this.height = actualSize.height
		})

		stage.invalidate()

		//println("STAGE RESIZED: $this, virtualSize=$virtualSize, actualSize=$actualSize, targetSize=$targetSize")
	}

	fun dispose() {
	}
}

class Stage(val views: Views) : Container(), View.Reference, CoroutineScope by views {
	val ag get() = views.ag
	override val stage: Stage = this

	override fun getLocalBoundsInternal(out: Rectangle) {
		out.setTo(views.actualVirtualLeft, views.actualVirtualTop, views.actualVirtualWidth, views.actualVirtualHeight)
	}

	override fun hitTest(x: Double, y: Double): View? = super.hitTest(x, y) ?: this

    private val scissors = AG.Scissor(0, 0, 0, 0)

	override fun renderInternal(ctx: RenderContext) {
		if (views.clipBorders) {
            scissors.x = x.toInt()
            scissors.y = y.toInt()
            scissors.width = (views.virtualWidth * scaleX).toInt()
            scissors.height = (views.virtualHeight * scaleY).toInt()
			ctx.ctx2d.scissor(scissors) {
				super.renderInternal(ctx)
			}
		} else {
			super.renderInternal(ctx)
		}
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
	val ag: LogAG = LogAG(),
	val input: Input = Input(),
	val timeProvider: TimeProvider = TimeProvider,
	val stats: Stats = Stats(),
	val gameWindow: GameWindow = GameWindowLog()
) : CoroutineScope {
	val views = Views(coroutineContext, ag, injector, input, timeProvider, stats, gameWindow)
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

inline fun <reified T : Component> View.forEachComponent(
	tempComponents: ArrayList<Component> = arrayListOf(),
	callback: (T) -> Unit
) {
	val components = getComponents(this, tempComponents)
	var n = 0
	while (n < components.size) {
		val c = components.getOrNull(n) ?: break
		if (c is T) callback(c)
		n++
	}
}

fun getComponents(view: View, out: ArrayList<Component> = arrayListOf()): List<Component> {
	out.clear()
	appendComponents(view, out)
	return out
}

fun appendComponents(view: View, out: ArrayList<Component>) {
	if (view is Container) {
		view.children.fastForEach { appendComponents(it, out) }
	}
	val components = view.unsafeListRawComponents
	if (components != null) out.addAll(components)
}

fun View.updateSingleView(dtMsD: Double, tempComponents: ArrayList<Component> = arrayListOf()) {
	this.forEachComponent<UpdateComponent>(tempComponents) {
		it.update(dtMsD * it.view.globalSpeed)
	}
}

fun View.updateSingleViewWithViews(views: Views, dtMsD: Double, tempComponents: ArrayList<Component> = arrayListOf()) {
	this.forEachComponent<UpdateComponentWithViews>(tempComponents) {
		it.update(views, dtMsD * it.view.globalSpeed)
	}
}

