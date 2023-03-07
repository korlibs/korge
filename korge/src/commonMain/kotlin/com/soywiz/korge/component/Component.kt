package com.soywiz.korge.component

import com.soywiz.korge.baseview.*
import com.soywiz.korge.view.*
import com.soywiz.korio.lang.*

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
