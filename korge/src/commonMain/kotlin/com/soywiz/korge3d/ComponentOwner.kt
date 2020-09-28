package com.soywiz.korge3d

import com.soywiz.korge.component.detach
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korge3d.component.*
import kotlin.reflect.KClass

abstract class ComponentOwner {

    internal var _components: Components? = null

    internal val componentsSure: Components
        get() {
            if (_components == null) _components = Components()
            return _components!!
        }

    /** Adds a component to this view */
    fun addComponent(c: com.soywiz.korge3d.component.Component): com.soywiz.korge3d.component.Component = componentsSure.add(c)
    fun addComponent(c: com.soywiz.korge3d.component.MouseComponent) = componentsSure.add(c)
    fun addComponent(c: com.soywiz.korge3d.component.KeyComponent) = componentsSure.add(c)
    fun addComponent(c: com.soywiz.korge3d.component.GamepadComponent) = componentsSure.add(c)
    fun addComponent(c: com.soywiz.korge3d.component.TouchComponent) = componentsSure.add(c)
    fun addComponent(c: com.soywiz.korge3d.component.EventComponent) = componentsSure.add(c)
    fun addComponent(c: com.soywiz.korge3d.component.UpdateComponentWithViews) = componentsSure.add(c)
    fun addComponent(c: com.soywiz.korge3d.component.UpdateComponent) = componentsSure.add(c)
    fun addComponent(c: com.soywiz.korge3d.component.ResizeComponent) = componentsSure.add(c)

    /** Removes a specific [c] component from the view */
    fun removeComponent(c: Component) {
        _components?.remove(c)
    }

    fun removeComponent(c: MouseComponent) {
        _components?.remove(c)
    }

    fun removeComponent(c: KeyComponent) {
        _components?.remove(c)
    }

    fun removeComponent(c: GamepadComponent) {
        _components?.remove(c)
    }

    fun removeComponent(c: TouchComponent) {
        _components?.remove(c)
    }

    fun removeComponent(c: EventComponent) {
        _components?.remove(c)
    }

    fun removeComponent(c: UpdateComponentWithViews) {
        _components?.remove(c)
    }

    fun removeComponent(c: UpdateComponent) {
        _components?.remove(c)
    }

    fun removeComponent(c: ResizeComponent) {
        _components?.remove(c)
    }

    //fun removeComponents(c: KClass<out Component>) { components?.removeAll { it.javaClass.isSubtypeOf(c) } }
    /** Removes a set of components of the type [c] from the view */
    @Deprecated("")
    fun removeComponents(c: KClass<out Component>) {
        _components?.removeAll(c)
    }

    /** Removes all the components attached to this view */
    fun removeAllComponents(): Unit {
        _components?.removeAll()
    }


    /** Registers a [block] that will be executed once in the next frame that this [View] is displayed with the [Views] singleton */
    fun deferWithViews(block: (views: Views) -> Unit) {
        addComponent(DeferWithViewsUpdateComponentWithViews((this as View3D), block))
    }

    internal class DeferWithViewsUpdateComponentWithViews(override val view: View3D, val block: (views: Views) -> Unit) :
        com.soywiz.korge3d.component.UpdateComponentWithViews {
        override fun update(views: Views, ms: Double) {
            block(views)
            detach()
        }
    }
}
