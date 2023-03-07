package com.soywiz.korge.component

import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.korev.Event
import com.soywiz.korev.GamePadConnectionEvent
import com.soywiz.korev.GamePadUpdateEvent
import com.soywiz.korev.GestureEvent
import com.soywiz.korev.KeyEvent
import com.soywiz.korev.MouseEvent
import com.soywiz.korev.TouchEvent
import com.soywiz.korge.baseview.BaseView
import com.soywiz.korge.view.*
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.lang.CloseableCancellable

@Deprecated("Use events instead")
interface ComponentType<T : Component>

/**
 * An interface that allows to control the behaviour of a [View] after some events.
 * The most common case of Component is the
 */
@Deprecated("Use events instead")
interface Component : CloseableCancellable {
    val view: BaseView
    val type: ComponentType<out Component>

    override fun close() {
        this.view.removeComponent(this)
    }

    override fun cancel(e: Throwable) {
        this.view.removeComponent(this)
    }
}

@Deprecated("Use events instead")
interface TypedComponent<T : Component> : Component {
    override val type: ComponentType<T>
}

@Deprecated("Use events instead")
fun Component.cancellable(): CloseableCancellable = CloseableCancellable { detach() }

//Deprecated("Unoptimized")
@Deprecated("Use events instead")
fun <T : Component> T.attach(): T {
    this.view.addComponent(this)
    return this
}

//Deprecated("Unoptimized")
@Deprecated("Use events instead")
fun <T : Component> T.detach(): T {
    this.view.removeComponent(this)
    return this
}

@Deprecated("Use events instead")
fun Component.removeFromView() {
    close()
}

/**
 * Component whose [onTouchEvent] is called,
 * whenever a touch event happens.
 */
@Deprecated("Use events instead")
interface TouchComponent : TypedComponent<TouchComponent> {
    companion object : ComponentType<TouchComponent>
    override val type get() = Companion

    fun onTouchEvent(views: Views, e: TouchEvent)
}

@Deprecated("Use events instead")
interface GestureComponent : TypedComponent<GestureComponent> {
    companion object : ComponentType<GestureComponent>
    override val type get() = Companion

    fun onGestureEvent(views: Views, event: GestureEvent)
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
@Deprecated("Use events instead")
interface GamepadComponent : TypedComponent<GamepadComponent> {
    companion object : ComponentType<GamepadComponent>
    override val type get() = Companion

    fun onGamepadEvent(views: Views, event: GamePadUpdateEvent)
    fun onGamepadEvent(views: Views, event: GamePadConnectionEvent)
}

/**
 * Component whose [onEvent] method is called when an event has been triggered in that [View].
 */
@Deprecated("Use events instead")
interface EventComponent : TypedComponent<EventComponent> {
    companion object : ComponentType<EventComponent>
    override val type get() = Companion

    fun onEvent(event: Event)
}
