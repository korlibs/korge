package com.soywiz.korge.view

import com.soywiz.korag.AG
import com.soywiz.korag.log.LogAG
import com.soywiz.korge.bitmapfont.BitmapFont
import com.soywiz.korge.bitmapfont.convert
import com.soywiz.korge.input.Input
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.Texture
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.font.BitmapFontGenerator
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.inject.Singleton
import com.soywiz.korio.util.Extra

@Singleton
class Views(
        val ag: AG,
        val injector: AsyncInjector,
        val input: Input
) : Extra by Extra.Mixin() {
    var lastId = 0
    val renderContext = RenderContext(ag)
    fun container() = Container(this)
	val dummyTexture by lazy {
		texture(Bitmap32(1, 1))
	}
	val dummyFont by lazy {
		BitmapFont(ag, 16, mapOf(), mapOf())
	}
	val defaultFont by lazy {
		//println(BitmapFontGenerator.LATIN_ALL)
		com.soywiz.korim.font.BitmapFontGenerator.generate("Arial", 16, BitmapFontGenerator.LATIN_ALL).convert(ag)
	}
	val fontRepository = FontRepository(this)

    val root: Container = container()
    fun render() {
        root.render(renderContext)
        renderContext.flush()
    }

    fun dump(emit: (String) -> Unit = ::println) = dumpView(root, emit)

    fun dumpView(view: View, emit: (String) -> Unit = ::println, indent: String = "") {
        emit("$indent$view")
        if (view is Container) {
            for (child in view.children) {
                dumpView(child, emit, "$indent ")
            }
        }
    }

    fun update(dtMs: Int) {
        input.frame.reset()
        root.update(dtMs)
    }
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

