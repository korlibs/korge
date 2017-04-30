package com.soywiz.korge.component

import com.soywiz.korge.event.addEventListener
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korio.util.Cancellable

open class Component(val view: View) {
	val detatchCancellables = arrayListOf<Cancellable>()

	val views: Views get() = view.views
	fun attach() = view.addComponent(this)
	fun dettach() = view.removeComponent(this)
	fun afterDetatch() {
		for (e in detatchCancellables) e.cancel()
		detatchCancellables.clear()
	}

	open fun update(dtMs: Int): Unit = Unit

	inline fun <reified T : Any> addEventListener(noinline handler: (T) -> Unit) {
		detatchCancellables += this.view.addEventListener<T>(handler)
	}
}
