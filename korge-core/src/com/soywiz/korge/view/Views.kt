package com.soywiz.korge.view

import com.soywiz.korag.AG
import com.soywiz.korag.log.LogAG
import com.soywiz.korge.bitmapfont.BitmapFont
import com.soywiz.korge.bitmapfont.convert
import com.soywiz.korge.event.Event
import com.soywiz.korge.event.EventDispatcher
import com.soywiz.korge.input.Input
import com.soywiz.korge.plugin.KorgePlugin
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.Texture
import com.soywiz.korge.render.TransformedTexture
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.Colors
import com.soywiz.korim.font.BitmapFontGenerator
import com.soywiz.korio.inject.AsyncDependency
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.inject.Singleton
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.util.Extra
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korma.geom.*
import java.util.*

@Singleton
class Views(
	val ag: AG,
	val injector: AsyncInjector,
	val input: Input
) : AsyncDependency, Updatable, Extra by Extra.Mixin(), EventDispatcher by EventDispatcher.Mixin() {
	var lastId = 0
	val renderContext = RenderContext(ag)

	init {
		injector.mapTyped<AG>(ag)
	}

	val propsTriggers = hashMapOf<String, (View, String, String) -> Unit>()

	fun registerPropertyTrigger(propName: String, gen: (View, String, String) -> Unit) {
		propsTriggers[propName] = gen
	}

	var clampElapsedTimeTo = 100

	val nativeWidth get() = ag.backWidth
	val nativeHeight get() = ag.backHeight

	var virtualWidth = 640; internal set
	var virtualHeight = 480; internal set

	var actualVirtualLeft = 0; private set
	var actualVirtualTop = 0; private set

	var actualVirtualWidth = 640; private set
	var actualVirtualHeight = 480; private set

	val nativeMouseX: Double get() = input.mouse.x
	val nativeMouseY: Double get() = input.mouse.y

	//var actualVirtualWidth = ag.backWidth
	//var actualVirtualHeight = ag.backHeight

	//var scaleMode: ScaleMode = ScaleMode.COVER
	//var scaleMode: ScaleMode = ScaleMode.NO_SCALE
	var scaleMode: ScaleMode = ScaleMode.SHOW_ALL
	var scaleAnchor = Anchor.MIDDLE_CENTER

	suspend override fun init() {
		for (plugin in ServiceLoader.load(KorgePlugin::class.java)) {
			plugin.register(this)
		}
	}

	override fun <T : Any> dispatch(event: T, clazz: Class<T>) {
		this.stage.dispatch(event, clazz)
	}

	private val resizedEvent = StageResizedEvent(0, 0)

	fun container() = Container(this)
	inline fun solidRect(width: Number, height: Number, color: Int): SolidRect = SolidRect(this, width.toDouble(), height.toDouble(), color)

	val dummyView by lazy { View(this) }
	val transparentTexture by lazy { texture(Bitmap32(1, 1)) }
	val whiteTexture by lazy { texture(Bitmap32(1, 1, intArrayOf(Colors.WHITE))) }
	val transformedDummyTexture by lazy { TransformedTexture(transparentTexture) }
	val dummyFont by lazy { BitmapFont(ag, 16, mapOf(), mapOf()) }
	val defaultFont by lazy {
		com.soywiz.korim.font.BitmapFontGenerator.generate("Arial", 16, BitmapFontGenerator.LATIN_ALL).convert(ag)
	}
	val fontRepository = FontRepository(this)

	val stage = Stage(this)

	@Deprecated("Use stage", ReplaceWith("stage"))
	val root: Container get() = stage

	fun render() {
		stage.render(renderContext)
		renderContext.flush()
	}

	fun dump(emit: (String) -> Unit = ::println) = dumpView(stage, emit)

	fun dumpView(view: View, emit: (String) -> Unit = ::println, indent: String = "") {
		emit("$indent$view")
		if (view is Container) {
			for (child in view.children) {
				dumpView(child, emit, "$indent ")
			}
		}
	}

	override fun update(dtMs: Int) {
		//println(this)
		//println("Update: $dtMs")
		input.frame.reset()
		stage.update(dtMs)
	}

	private val virtualSize = SizeInt()
	private val actualSize = SizeInt()
	private val targetSize = SizeInt()

	fun mouseUpdated() {
		//println("localMouse: (${stage.localMouseX}, ${stage.localMouseY}), inputMouse: (${input.mouse.x}, ${input.mouse.y})")
	}

	fun resized(width: Int, height: Int) {
		//println("$e : ${views.ag.backWidth}x${views.ag.backHeight}")
		val actualWidth = width
		val actualHeight = height
		val virtualWidth = virtualWidth
		val virtualHeight = virtualHeight
		val anchor = scaleAnchor

		actualSize.setTo(actualWidth, actualHeight)
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

		stage.dispatch(resizedEvent.setSize(width, height))
	}
}

class Stage(views: Views) : Container(views) {
	override fun getLocalBounds(out: Rectangle) {
		out.setTo(views.actualVirtualLeft, views.actualVirtualTop, views.actualVirtualWidth, views.actualVirtualHeight)
	}

	override fun hitTestInternal(x: Double, y: Double): View? {
		return super.hitTestInternal(x, y) ?: this
	}

	override fun hitTestBoundingInternal(x: Double, y: Double): View? {
		return super.hitTestBoundingInternal(x, y) ?: this
	}
}

class ViewsLog(
	val injector: AsyncInjector = AsyncInjector(),
	val ag: LogAG = LogAG(),
	val input: Input = Input()
) : AsyncDependency {
	val views = Views(ag, injector, input)

	suspend override fun init() {
		views.init()
	}
}

/*
object ViewFactory {
    inline fun Container.container(): Container {
        val container = views.container()
        this += container
        return container
    }
}

inline fun viewFactory(callback: ViewFactory.() -> Unit) {
    ViewFactory.callback()
}
*/

inline fun Container.container(): Container = container { }

inline fun Container.container(callback: Container.() -> Unit): Container {
	val child = views.container()
	this += child
	callback(child)
	return child
}

fun Views.texture(bmp: Bitmap, mipmaps: Boolean = false): Texture {
	return Texture(Texture.Base(ag.createTexture(bmp, mipmaps), bmp.width, bmp.height))
}

interface ViewsContainer {
	val views: Views
}

data class KorgeFileLoaderTester<T>(val name: String, val tester: suspend (s: SyncStream, injector: AsyncInjector) -> KorgeFileLoader<T>?) {
	suspend operator fun invoke(s: SyncStream, injector: AsyncInjector) = tester(s, injector)
	override fun toString(): String = "KorgeFileTester(\"$name\")"
}

data class KorgeFileLoader<T>(val name: String, val loader: suspend VfsFile.(Views) -> T) {
	override fun toString(): String = "KorgeFileLoader(\"$name\")"
}

//suspend val AsyncInjector.views: Views get() = this.get<Views>()

data class StageResizedEvent(var width: Int, var height: Int) : Event {
	fun setSize(width: Int, height: Int) = this.apply {
		this.width = width
		this.height = height
	}
}

interface MouseEvent : Event
object MouseUpEvent : MouseEvent
object MouseDownEvent : MouseEvent
object MouseMovedEvent : MouseEvent
