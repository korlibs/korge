package com.soywiz.korge.view

import com.soywiz.korag.AG
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.Texture
import com.soywiz.korim.geom.Point2d
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.inject.Singleton

@Singleton
class Views(val ag: AG, val injector: AsyncInjector) {
	val mouse = Point2d()
	var lastId = 0
	val renderContext = RenderContext(ag)
	fun container() = Container(this)
	fun image(tex: Texture, anchorX: Double = 0.0, anchorY: Double = anchorX) = Image(tex, anchorX, anchorY, this)

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
}

fun View.dump(emit: (String) -> Unit = ::println) = this.views.dumpView(this, emit)
fun View.dumpToString(): String {
	val out = arrayListOf<String>()
	dump { out += it }
	return out.joinToString("\n")
}