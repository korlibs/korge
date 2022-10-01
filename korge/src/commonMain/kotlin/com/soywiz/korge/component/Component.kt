package com.soywiz.korge.component

import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.korev.Event
import com.soywiz.korev.GamePadConnectionEvent
import com.soywiz.korev.GamePadUpdateEvent
import com.soywiz.korev.KeyEvent
import com.soywiz.korev.MouseEvent
import com.soywiz.korev.TouchEvent
import com.soywiz.korge.baseview.BaseView
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.lang.CloseableCancellable

interface ComponentType<T : Component>

/**
 * An interface that allows to control the behaviour of a [View] after some events.
 * The most common case of Component is the [UpdateComponent]
 */
interface Component : Closeable {
    val view: BaseView
    val type: ComponentType<out Component>

    override fun close() {
        this.view.removeComponent(this)
    }
}

interface TypedComponent<T : Component> : Component {
    override val type: ComponentType<T>
}

fun Component.cancellable(): CloseableCancellable = CloseableCancellable { detach() }

//Deprecated("Unoptimized")
fun <T : Component> T.attach(): T {
    this.view.addComponent(this)
    return this
}

//Deprecated("Unoptimized")
fun <T : Component> T.detach(): T {
    this.view.removeComponent(this)
    return this
}

fun Component.removeFromView() {
    close()
}

/**
 * Component whose [onTouchEvent] is called,
 * whenever a touch event happens.
 */
interface TouchComponent : TypedComponent<TouchComponent> {
    companion object : ComponentType<TouchComponent>
    override val type get() = Companion

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
interface MouseComponent : TypedComponent<MouseComponent> {
    companion object : ComponentType<MouseComponent>
    override val type get() = Companion

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
interface KeyComponent : TypedComponent<KeyComponent> {
    companion object : ComponentType<KeyComponent>
    override val type get() = Companion

    fun Views.onKeyEvent(event: KeyEvent)
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
interface GamepadComponent : TypedComponent<GamepadComponent> {
    companion object : ComponentType<GamepadComponent>
    override val type get() = Companion

    fun onGamepadEvent(views: Views, event: GamePadUpdateEvent)
    fun onGamepadEvent(views: Views, event: GamePadConnectionEvent)
}

/**
 * Component whose [onEvent] method is called when an event has been triggered in that [View].
 */
interface EventComponent : TypedComponent<EventComponent> {
    companion object : ComponentType<EventComponent>
    override val type get() = Companion

    fun onEvent(event: Event)
}

/**
 * Component whose [update] method is called each frame
 * with the delta milliseconds that has passed since the last frame.
 *
 * It is like [UpdateComponent] but includes a reference to the [Views] itself.
 */
interface UpdateComponentWithViews : TypedComponent<UpdateComponentWithViews> {
    companion object : ComponentType<UpdateComponentWithViews>
    override val type get() = Companion

    fun update(views: Views, dt: TimeSpan)
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
interface UpdateComponent : TypedComponent<UpdateComponent> {
    companion object : ComponentType<UpdateComponent>
    override val type get() = Companion

    fun update(dt: TimeSpan)
}

abstract class FixedUpdateComponent(override val view: BaseView, val step: TimeSpan, val maxAccumulated: Int = 10) : UpdateComponent {
    var accumulated = 0.milliseconds
    final override fun update(dt: TimeSpan) {
        accumulated += dt
        if (accumulated >= step * maxAccumulated) {
            accumulated = step * maxAccumulated
        }
        while (accumulated >= step) {
            accumulated -= step
            update()
        }
    }
    abstract fun update(): Unit
}

/**
 * Component whose [resized] method is called everytime the game window
 * has been resized.
 */
interface ResizeComponent : TypedComponent<ResizeComponent> {
    override val type get() = Companion

    /**
     * Includes the [Views] singleton. [width],[height] are [Views.nativeWidth],[Views.nativeHeight].
     */
    fun resized(views: Views, width: Int = views.nativeWidth, height: Int = views.nativeHeight)

    companion object : ComponentType<ResizeComponent> {
        operator fun invoke(view: BaseView, block: Views.(width: Int, height: Int) -> Unit): ResizeComponent =
            object : ResizeComponent {
                override val view: BaseView = view
                override fun resized(views: Views, width: Int, height: Int) {
                    block(views, width, height)
                }
            }
    }
}

fun <T : BaseView> T.onStageResized(firstTrigger: Boolean = true, block: Views.(width: Int, height: Int) -> Unit): T = this.apply {
    if (firstTrigger) {
        deferWithViews { views -> block(views, views.actualVirtualWidth, views.actualVirtualHeight) }
    }
    addComponent(ResizeComponent(this, block))
}

/*
open class Component(val view: BaseView) : EventDispatcher by view, Cancellable {
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
