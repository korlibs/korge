package com.soywiz.korge.baseview

import com.soywiz.klock.TimeSpan
import com.soywiz.korge.annotations.KorgeExperimental
import com.soywiz.korge.component.*
import com.soywiz.korge.internal.KorgeInternal
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import kotlin.collections.set

open class BaseView {
    @KorgeInternal
    @PublishedApi
    internal var _components: Components? = null

    @KorgeInternal
    @PublishedApi
    internal val componentsSure: Components
        get() {
            if (_components == null) _components = Components()
            return _components!!
        }

    /** Creates a typed [T] component (using the [gen] factory function) if the [View] doesn't have any of that kind, or returns a component of that type if already attached */
//Deprecated("")
//inline fun <reified T : Component> ComponentContainer.getOrCreateComponent(gen: (BaseView) -> T): T = componentsSure.getOrCreateComponent(this, T::class, gen)
    inline fun <reified T : Component> getOrCreateComponentOther(gen: (BaseView) -> T): T = componentsSure.getOrCreateComponent(this, T::class, gen)
    inline fun <reified T : MouseComponent> getOrCreateComponentMouse(gen: (BaseView) -> T): T = componentsSure.getOrCreateComponent(this, T::class, gen)
    inline fun <reified T : KeyComponent> getOrCreateComponentKey(gen: (BaseView) -> T): T = componentsSure.getOrCreateComponent(this, T::class, gen)
    inline fun <reified T : GamepadComponent> getOrCreateComponentGamepad(gen: (BaseView) -> T): T = componentsSure.getOrCreateComponent(this, T::class, gen)
    inline fun <reified T : TouchComponent> getOrCreateComponentTouch(gen: (BaseView) -> T): T = componentsSure.getOrCreateComponent(this, T::class, gen)
    inline fun <reified T : EventComponent> getOrCreateComponentEvent(gen: (BaseView) -> T): T = componentsSure.getOrCreateComponent(this, T::class, gen)
    inline fun <reified T : UpdateComponentWithViews> getOrCreateComponentUpdateWithViews(gen: (BaseView) -> T): T = componentsSure.getOrCreateComponent(this, T::class, gen)
    inline fun <reified T : UpdateComponent> getOrCreateComponentUpdate(gen: (BaseView) -> T): T = componentsSure.getOrCreateComponent(this, T::class, gen)
    inline fun <reified T : ResizeComponent> getOrCreateComponentResize(gen: (BaseView) -> T): T = componentsSure.getOrCreateComponent(this, T::class, gen)
    inline fun <reified T : UpdateComponent> getComponentUpdate(): T? = componentsSure.getComponentUpdate<T>()
    @KorgeExperimental
    inline fun <reified T : UpdateComponent> getUpdateComponents(): List<T> = componentsSure.getUpdateComponents()

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
///** Removes a set of components of the type [c] from the view */
//@eprecated("")
//fun removeComponents(c: KClass<out Component>) { _components?.removeAll(c) }

    /** Removes all the components attached to this view */
    fun removeAllComponents(): Unit {
        _components?.removeAll()
    }

    /** Adds a component to this view */
    fun addComponent(c: Component): Component = componentsSure.add(c)
    fun addComponent(c: MouseComponent) = componentsSure.add(c)
    fun addComponent(c: KeyComponent) = componentsSure.add(c)
    fun addComponent(c: GamepadComponent) = componentsSure.add(c)
    fun addComponent(c: TouchComponent) = componentsSure.add(c)
    fun addComponent(c: EventComponent) = componentsSure.add(c)
    fun addComponent(c: UpdateComponentWithViews) = componentsSure.add(c)
    fun addComponent(c: UpdateComponent) = componentsSure.add(c)
    fun addComponent(c: ResizeComponent) = componentsSure.add(c)

    /** Registers a [block] that will be executed once in the next frame that this [View] is displayed with the [Views] singleton */
    fun deferWithViews(block: (views: Views) -> Unit) {
        addComponent(DeferWithViewsUpdateComponentWithViews(this@BaseView, block))
    }

    internal class DeferWithViewsUpdateComponentWithViews(override val view: BaseView, val block: (views: Views) -> Unit) :
        UpdateComponentWithViews {
        override fun update(views: Views, dt: TimeSpan) {
            block(views)
            detach()
        }
    }



    // region Properties
    private val _props = linkedMapOf<String, Any?>()

    /** Immutable map of custom String properties attached to this view. Should use [hasProp], [getProp] and [addProp] methods to control this */
    val props: Map<String, Any?> get() = _props

    /** Checks if this view has the [key] property */
    fun hasProp(key: String) = key in _props

    /** Gets the [key] property of this view as a [String] or [default] when not found */
    fun getPropString(key: String, default: String = "") = _props[key]?.toString() ?: default

    /** Gets the [key] property of this view as an [Double] or [default] when not found */
    fun getPropDouble(key: String, default: Double = 0.0): Double {
        val value = _props[key]
        if (value is Number) return value.toDouble()
        if (value is String) return value.toDoubleOrNull() ?: default
        return default
    }

    /** Gets the [key] property of this view as an [Int] or [default] when not found */
    fun getPropInt(key: String, default: Int = 0) = getPropDouble(key, default.toDouble()).toInt()

    /** Adds or replaces the property [key] with the [value] */
    fun addProp(key: String, value: Any?) {
        _props[key] = value
        //val componentGen = views.propsTriggers[key]
        //if (componentGen != null) {
        //	componentGen(this, key, value)
        //}
    }

    /** Adds a list of [values] properties at once */
    fun addProps(values: Map<String, Any?>) {
        for (pair in values) addProp(pair.key, pair.value)
    }
    // endregion

    // Returns the typed property associated with the provided key.
    // Crashes if the key is not found or if failed to cast to type.
    inline fun <reified T : Any> getProp(key: String): T {
        return getPropOrNull(key)!!
    }

    // Returns the typed property associated with the provided key or null if it doesn't exist
    // Crashes if failed to cast to type.
    inline fun <reified T : Any> getPropOrNull(key: String): T? {
        return props[key] as T?
    }

}
