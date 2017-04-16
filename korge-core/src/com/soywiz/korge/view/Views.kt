package com.soywiz.korge.view

import com.soywiz.korag.AG
import com.soywiz.korag.log.LogAG
import com.soywiz.korge.bitmapfont.BitmapFont
import com.soywiz.korge.bitmapfont.convert
import com.soywiz.korge.input.Input
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.Texture
import com.soywiz.korge.render.TransformedTexture
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.font.BitmapFontGenerator
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.inject.Singleton
import com.soywiz.korio.util.Extra
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.ISize
import com.soywiz.korma.geom.ScaleMode

@Singleton
class Views(
	val ag: AG,
	val injector: AsyncInjector,
	val input: Input
) : Extra by Extra.Mixin() {
	var lastId = 0
	val renderContext = RenderContext(ag)

	val nativeWidth get() = ag.backWidth
	val nativeHeight get() = ag.backHeight

	var virtualWidth = 640; internal set
	var virtualHeight = 480; internal set

	var actualVirtualWidth = 640; private set
	var actualVirtualHeight = 480; private set

	//var actualVirtualWidth = ag.backWidth
	//var actualVirtualHeight = ag.backHeight

	//var scaleMode: ScaleMode = ScaleMode.COVER
	//var scaleMode: ScaleMode = ScaleMode.NO_SCALE
	var scaleMode: ScaleMode = ScaleMode.SHOW_ALL
	var scaleAnchor = Anchor.MIDDLE_CENTER

	private val resizedEvent = View.StageResizedEvent(0, 0)

	fun container() = Container(this)
	val dummyTexture by lazy { texture(Bitmap32(1, 1)) }
	val transformedDummyTexture by lazy { TransformedTexture(dummyTexture) }
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

	fun update(dtMs: Int) {
		//println(this)
		//println("Update: $dtMs")
		input.frame.reset()
		stage.update(dtMs)
	}

	private val virtualSize = ISize()
	private val actualSize = ISize()
	private val targetSize = ISize()

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

		stage.x = ((actualVirtualWidth - virtualWidth) * anchor.sx) * ratioX
		stage.y = ((actualVirtualHeight - virtualHeight) * anchor.sy) * ratioY

		stage.handleEvent(resizedEvent.setSize(width, height))
	}
}

class Stage(views: Views) : Container(views) {
}

class ViewsLog(
	val injector: AsyncInjector = AsyncInjector(),
	val ag: LogAG = LogAG(),
	val input: Input = Input()
) {
	val views = Views(ag, injector, input)
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
