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
