package com.soywiz.korge.component

import com.soywiz.korge.view.*
import com.soywiz.korev.*

/**
 * An interface that allows to control the behaviour of a [View] after some events.
 * The most common case of Component is the [UpdateComponent]
 */
interface Component {
	val view: View
}

fun <T : Component> T.attach() = this.apply { this.view.addComponent(this) }
fun <T : Component> T.detach() = this.apply { this.view.removeComponent(this) }

fun Component.removeFromView() = view.removeComponent(this)

/**
 * Component whose [onTouchEvent] is called,
 * whenever a touch event happens.
 */
interface TouchComponent : Component {
	fun onTouchEvent(views: Views, e: TouchEvent)
}

/**
 * Component whose [onMouseEvent] is called,
 * whenever a mouse event happens.
 *
 * **Notice** that this class produces raw mouse events.
 * You would normally add mouse handlers by executing:
 *
 * ```kotlin
 * view.mouse {
 *     down { ... }
 *     up { ... }
 *     click { ... }
 *     ...
 * }
 * ```
 */
interface MouseComponent : Component {
	fun onMouseEvent(views: Views, event: MouseEvent)
}

/**
 * Component whose [onKeyEvent] is called,
 * whenever a key is pressed, released or typed.
 *
 * You would normally add key handlers by executing:
 *
 * ```kotlin
 * view.keys {
 *     press { ... }
 *     down { ... }
 *     up { ... }
 * }
 */
interface KeyComponent : Component {
	fun onKeyEvent(views: Views, event: KeyEvent)
}

/**
 * Component whose [onGamepadEvent] is called, whenever
 * a gamepad event occurs in the application (updated a frame, or connected a gamepad).
 *
 * You would normally add gamepad handlers by executing:
 *
 * ```kotlin
 * view.gamepad {
 *     down(...) { }
 *     up(...) { }
 *     button(...) {}
 *     stick(...) {}
 *     connected {  }
 *     disconnected {  }
 * }
 * ```
 */
interface GamepadComponent : Component {
	fun onGamepadEvent(views: Views, event: GamePadUpdateEvent)
	fun onGamepadEvent(views: Views, event: GamePadConnectionEvent)

	@Deprecated("") fun onGamepadEvent(views: Views, event: GamePadButtonEvent)
	@Deprecated("") fun onGamepadEvent(views: Views, event: GamePadStickEvent)
}

/**
 * Component whose [onEvent] method is called when an event has been triggered in that [View].
 */
interface EventComponent : Component {
	fun onEvent(event: Event)
}

/**
 * Component whose [update] method is called each frame
 * with the delta milliseconds that has passed since the last frame.
 *
 * It is like [UpdateComponent] but includes a reference to the [Views] itself.
 */
interface UpdateComponentWithViews : Component {
	fun update(views: Views, ms: Double)
}

/**
 * Component whose [update] method is called each frame
 * with the delta milliseconds that has passed since the last frame.
 *
 * In the case you need the [Views] object, you can use [UpdateComponentWithViews] instead.
 *
 * The typical way of adding an update component to a view is by calling:
 *
 * ```kotlin
 * view.addUpdater { dt -> ... }
 * ```
 */
interface UpdateComponent : Component {
	fun update(ms: Double)
}

/**
 * Component whose [resized] method is called everytime the game window
 * has been resized.
 */
interface ResizeComponent : Component {
    /**
     * Includes the [Views] singleton. [width],[height] are [Views.nativeWidth],[Views.nativeHeight].
     */
	fun resized(views: Views, width: Int = views.nativeWidth, height: Int = views.nativeHeight)
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
