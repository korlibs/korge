package com.soywiz.korge.component

import com.soywiz.korge.view.*
import com.soywiz.korui.event.*
import com.soywiz.korui.input.*

interface Component {
	val view: View
}

fun <T : Component> T.attach() = this.apply { this.view.addComponent(this) }
fun <T : Component> T.detach() = this.apply { this.view.removeComponent(this) }

fun Component.removeFromView() = view.removeComponent(this)

interface TouchComponent : Component {
	fun onTouchEvent(views: Views, e: TouchEvent)
}

interface MouseComponent : Component {
	fun onMouseEvent(views: Views, event: MouseEvent)
}

interface KeyComponent : Component {
	fun onKeyEvent(views: Views, event: KeyEvent)
}

interface GamepadComponent : Component {
	fun onGamepadEvent(views: Views, event: GamePadButtonEvent)
	fun onGamepadEvent(views: Views, event: GamePadStickEvent)
	fun onGamepadEvent(views: Views, event: GamePadConnectionEvent)
}

interface EventComponent : Component {
	fun onEvent(event: Event)
}

interface UpdateComponentWithViews : Component {
	fun update(views: Views, ms: Double)
}

interface UpdateComponent : Component {
	fun update(ms: Double)
}

interface ResizeComponent : Component {
	fun resized(views: Views, width: Int, height: Int)
}

/*
open class Component(val view: View) : EventDispatcher by view, Cancellable {
	val detatchCloseables = arrayListOf<Closeable>()

	fun attach() = view.addComponent(this)
	fun dettach() = view.removeComponent(this)
	fun afterDetatch() {
		for (e in detatchCloseables) e.close()
		detatchCloseables.clear()
	}

	open fun update(dtMs: Int): Unit = Unit

	override fun <T : Event> addEventListener(clazz: KClass<T>, handler: (T) -> Unit): Closeable {
		val c = this.view.addEventListener<T>(clazz, handler)
		detatchCloseables += c
		return Closeable { detatchCloseables -= c }
	}

	override fun <T : Event> dispatch(clazz: KClass<T>, event: T) {
		this.view.dispatch(clazz, event)
	}

	override fun cancel(e: Throwable) {
		dettach()
	}
}
*/
