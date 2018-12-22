package com.soywiz.korge.view

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.klogger.*
import com.soywiz.korag.*
import com.soywiz.korag.log.*
import com.soywiz.korge.*
import com.soywiz.korge.async.*
import com.soywiz.korge.audio.*
import com.soywiz.korge.component.*
import com.soywiz.korge.input.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.render.*
import com.soywiz.korge.stat.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korinject.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.stream.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*
import com.soywiz.korui.event.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.math.*
import kotlin.reflect.*

private val logger = Logger("Views")

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
class Views(
	override val coroutineContext: CoroutineContext,
	val ag: AG,
	val injector: AsyncInjector,
	val input: Input,
	val timeProvider: TimeProvider,
	val stats: Stats,
	val koruiContext: KoruiContext
) : Updatable, Extra by Extra.Mixin(), EventDispatcher by EventDispatcher.Mixin(), CoroutineScope, ViewsScope,
	BoundsProvider {

	var imageFormats = RegisteredImageFormats
	val renderContext = RenderContext(ag, this, stats, coroutineContext)
	val agBitmapTextureManager = renderContext.agBitmapTextureManager
	var clearEachFrame = true
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

	val actualVirtualRight get() = actualVirtualWidth
	val actualVirtualBottom get() = actualVirtualHeight

	val nativeMouseX: Double get() = input.mouse.x
	val nativeMouseY: Double get() = input.mouse.y

	var scaleMode: ScaleMode = ScaleMode.SHOW_ALL
	var scaleAnchor = Anchor.MIDDLE_CENTER
	var clipBorders = true

	private val resizedEvent = ResizedEvent(0, 0)

	val stage = Stage(this)
	val root = stage
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
		injector.mapInstance(SoundSystem::class, soundSystem)
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
			if (e is MouseEvent) {
				stage.forEachComponent<MouseComponent>(tempComponents) { it.onMouseEvent(views, e) }
			}
			else if (e is ResizedEvent) {
				stage.forEachComponent<ResizeComponent>(tempComponents) { it.resized(views, e.width, e.height) }
			}
			else if (e is KeyEvent) {
				stage.forEachComponent<KeyComponent> (tempComponents){ it.onKeyEvent(views, e) }
			}
			else if (e is GamePadConnectionEvent) {
				stage.forEachComponent<GamepadComponent>(tempComponents) { it.onGamepadEvent(views, e) }
			}
			else if (e is GamePadButtonEvent) {
				stage.forEachComponent<GamepadComponent>(tempComponents) { it.onGamepadEvent(views, e) }
			}
			else if (e is GamePadStickEvent) {
				stage.forEachComponent<GamepadComponent>(tempComponents) { it.onGamepadEvent(views, e) }
			}
		} catch (e: PreventDefaultException) {
			//println("PreventDefaultException.Reason: ${e.reason}")
		}
	}

	val onBeforeRender = Signal<Unit>()

	fun render(clearColor: RGBA = Colors.BLACK, clear: Boolean = true) {
		onBeforeRender()
		if (clear) ag.clear(clearColor, stencil = 0, clearColor = true, clearStencil = true)
		stage.render(renderContext)

		if (debugViews) {
			for (debugHandler in debugHandlers) {
				this.debugHandler(renderContext)
			}
		}

		renderContext.flush()
		renderContext.finish()
		agBitmapTextureManager.afterRender()
	}

	fun frameUpdateAndRender(clear: Boolean, clearColor: RGBA, fixedSizeStep: TimeSpan = TimeSpan.NULL) {
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
		render(clearColor, clear)
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
		soundSystem.close()
	}
}

class Stage(val views: Views) : Container(), View.Reference, CoroutineScope by views {
	val ag get() = views.ag
	override val stage: Stage = this

	override fun getLocalBoundsInternal(out: Rectangle) {
		out.setTo(views.actualVirtualLeft, views.actualVirtualTop, views.actualVirtualWidth, views.actualVirtualHeight)
	}

	override fun hitTest(x: Double, y: Double): View? = super.hitTest(x, y) ?: this

	override fun renderInternal(ctx: RenderContext) {
		if (views.clipBorders) {
			ctx.ctx2d.scissor(
				AG.Scissor(
					x.toInt(), y.toInt(), (views.virtualWidth * scaleX).toInt(),
					(views.virtualHeight * scaleY).toInt()
				)
			) {
				super.renderInternal(ctx)
			}
		} else {
			super.renderInternal(ctx)
		}
	}
}

fun viewsLog(callback: Stage.(log: ViewsLog) -> Unit) {
	val log = ViewsLog()
	callback(log.views.stage, log)
}

class ViewsLog(
	override val coroutineContext: CoroutineDispatcher = KorgeDispatcher,
	val injector: AsyncInjector = AsyncInjector(),
	val ag: LogAG = LogAG(),
	val input: Input = Input(),
	val timeProvider: TimeProvider = TimeProvider,
	val stats: Stats = Stats(),
	val koruiContext: KoruiContext = KoruiContext()
) : CoroutineScope {
	val views = Views(coroutineContext, ag, injector, input, timeProvider, stats, koruiContext)
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

inline fun <reified T : Component> View.forEachComponent(tempComponents: ArrayList<Component> = arrayListOf(), callback: (T) -> Unit) {
	for (c in getComponents(this, tempComponents)) {
		if (c is T) callback(c)
	}
}

fun getComponents(view: View, out: ArrayList<Component> = arrayListOf()): List<Component> {
	out.clear()
	appendComponents(view, out)
	return out
}

fun appendComponents(view: View, out: ArrayList<Component>) {
	if (view is Container) for (child in view.children) appendComponents(child, out)
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

