package com.soywiz.korge.component

import com.soywiz.kds.iterators.*
import com.soywiz.klock.hr.*
import com.soywiz.klock.milliseconds
import com.soywiz.korge.view.*
import com.soywiz.korev.*
import kotlin.reflect.*

/**
 * An interface that allows to control the behaviour of a [View] after some events.
 * The most common case of Component is the [UpdateComponent]
 */
interface Component {
    val view: View
}

@Deprecated("Unoptimized")
fun <T : Component> T.attach(): T {
    this.view.addComponent(this); return this
}

fun MouseComponent.attach(): MouseComponent {
    this.view.addComponent(this); return this
}

fun KeyComponent.attach(): KeyComponent {
    this.view.addComponent(this); return this
}

fun GamepadComponent.attach(): GamepadComponent {
    this.view.addComponent(this); return this
}

fun TouchComponent.attach(): TouchComponent {
    this.view.addComponent(this); return this
}

fun EventComponent.attach(): EventComponent {
    this.view.addComponent(this); return this
}

fun UpdateComponentWithViews.attach(): UpdateComponentWithViews {
    this.view.addComponent(this); return this
}

fun UpdateComponent.attach(): UpdateComponent {
    this.view.addComponent(this); return this
}

fun ResizeComponent.attach(): ResizeComponent {
    this.view.addComponent(this); return this
}

@Deprecated("Unoptimized")
fun <T : Component> T.detach(): T {
    this.view.removeComponent(this); return this
}

fun MouseComponent.detach(): MouseComponent {
    this.view.removeComponent(this); return this
}

fun KeyComponent.detach(): KeyComponent {
    this.view.removeComponent(this); return this
}

fun GamepadComponent.detach(): GamepadComponent {
    this.view.removeComponent(this); return this
}

fun TouchComponent.detach(): TouchComponent {
    this.view.removeComponent(this); return this
}

fun EventComponent.detach(): EventComponent {
    this.view.removeComponent(this); return this
}

fun UpdateComponentWithViews.detach(): UpdateComponentWithViews {
    this.view.removeComponent(this); return this
}

fun UpdateComponent.detach(): UpdateComponent {
    this.view.removeComponent(this); return this
}

fun ResizeComponent.detach(): ResizeComponent {
    this.view.removeComponent(this); return this
}

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
    fun update(views: Views, dt: HRTimeSpan)
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
    fun update(dt: HRTimeSpan)
}

abstract class FixedUpdateComponent(override val view: View, val step: HRTimeSpan, val maxAccumulated: Int = 10) : UpdateComponent {
    var accumulated = 0.hrSeconds
    final override fun update(dt: HRTimeSpan) {
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
interface ResizeComponent : Component {
    /**
     * Includes the [Views] singleton. [width],[height] are [Views.nativeWidth],[Views.nativeHeight].
     */
    fun resized(views: Views, width: Int = views.nativeWidth, height: Int = views.nativeHeight)

    companion object {
        operator fun invoke(view: View, block: Views.(width: Int, height: Int) -> Unit): ResizeComponent =
            object : ResizeComponent {
                override val view: View = view
                override fun resized(views: Views, width: Int, height: Int) {
                    block(views, width, height)
                }
            }
    }
}

fun <T : View> T.onStageResized(firstTrigger: Boolean = true, block: Views.(width: Int, height: Int) -> Unit): T = this.apply {
    if (firstTrigger) {
        deferWithViews { views -> block(views, views.actualVirtualWidth, views.actualVirtualHeight) }
    }
    addComponent(ResizeComponent(this, block))
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

class Components {
    var mouse: ArrayList<MouseComponent>? = null
    var key: ArrayList<KeyComponent>? = null
    var gamepad: ArrayList<GamepadComponent>? = null
    var touch: ArrayList<TouchComponent>? = null
    var event: ArrayList<EventComponent>? = null
    var updateWV: ArrayList<UpdateComponentWithViews>? = null
    var update: ArrayList<UpdateComponent>? = null
    var resize: ArrayList<ResizeComponent>? = null
    var other: ArrayList<Component>? = null

    val emouse: ArrayList<MouseComponent>
        get() {
            if (mouse == null) mouse = arrayListOf(); return mouse!!
        }
    val ekey: ArrayList<KeyComponent>
        get() {
            if (key == null) key = arrayListOf(); return key!!
        }
    val egamepad: ArrayList<GamepadComponent>
        get() {
            if (gamepad == null) gamepad = arrayListOf(); return gamepad!!
        }
    val etouch: ArrayList<TouchComponent>
        get() {
            if (touch == null) touch = arrayListOf(); return touch!!
        }
    val eevent: ArrayList<EventComponent>
        get() {
            if (event == null) event = arrayListOf(); return event!!
        }
    val eupdateWV: ArrayList<UpdateComponentWithViews>
        get() {
            if (updateWV == null) updateWV = arrayListOf(); return updateWV!!
        }
    val eupdate: ArrayList<UpdateComponent>
        get() {
            if (update == null) update = arrayListOf(); return update!!
        }
    val eresize: ArrayList<ResizeComponent>
        get() {
            if (resize == null) resize = arrayListOf(); return resize!!
        }
    val eother: ArrayList<Component>
        get() {
            if (other == null) other = arrayListOf(); return other!!
        }

    fun getArrayComponentOrNull(c: Component): ArrayList<out Component>? = when (c) {
        is MouseComponent -> mouse
        is KeyComponent -> key
        is GamepadComponent -> gamepad
        is TouchComponent -> touch
        is EventComponent -> event
        is UpdateComponentWithViews -> updateWV
        is UpdateComponent -> update
        is ResizeComponent -> resize
        else -> other
    }

    fun getArrayComponent(c: Component): ArrayList<out Component> = when (c) {
        is MouseComponent -> emouse
        is KeyComponent -> ekey
        is GamepadComponent -> egamepad
        is TouchComponent -> etouch
        is EventComponent -> eevent
        is UpdateComponentWithViews -> eupdateWV
        is UpdateComponent -> eupdate
        is ResizeComponent -> eresize
        else -> eother
    }

    fun remove(c: Component) = getArrayComponentOrNull(c)?.remove(c)
    fun remove(c: MouseComponent) = mouse?.remove(c)
    fun remove(c: KeyComponent) = key?.remove(c)
    fun remove(c: GamepadComponent) = gamepad?.remove(c)
    fun remove(c: TouchComponent) = touch?.remove(c)
    fun remove(c: EventComponent) = event?.remove(c)
    fun remove(c: UpdateComponentWithViews) = updateWV?.remove(c)
    fun remove(c: UpdateComponent) = update?.remove(c)
    fun remove(c: ResizeComponent) = resize?.remove(c)

    @Suppress("UNCHECKED_CAST")
    fun add(c: Component): Component {
        (getArrayComponent(c) as ArrayList<Component>).plusAssign(c); return c
    }

    fun add(c: MouseComponent): MouseComponent {
        emouse.plusAssign(c); return c
    }

    fun add(c: KeyComponent): KeyComponent {
        ekey.plusAssign(c); return c
    }

    fun add(c: GamepadComponent): GamepadComponent {
        egamepad.plusAssign(c); return c
    }

    fun add(c: TouchComponent): TouchComponent {
        etouch.plusAssign(c); return c
    }

    fun add(c: EventComponent): EventComponent {
        eevent.plusAssign(c); return c
    }

    fun add(c: UpdateComponentWithViews): UpdateComponentWithViews {
        eupdateWV.plusAssign(c); return c
    }

    fun add(c: UpdateComponent): UpdateComponent {
        eupdate.plusAssign(c); return c
    }

    fun add(c: ResizeComponent): ResizeComponent {
        eresize.plusAssign(c); return c
    }

    fun removeAll() {
        mouse?.clear()
        key?.clear()
        gamepad?.clear()
        touch?.clear()
        event?.clear()
        updateWV?.clear()
        update?.clear()
        resize?.clear()
        other?.clear()
    }

    fun removeAll(c: KClass<out Component>) {
        when (c) {
            MouseComponent::class -> mouse?.clear()
            KeyComponent::class -> key?.clear()
            GamepadComponent::class -> gamepad?.clear()
            TouchComponent::class -> touch?.clear()
            EventComponent::class -> event?.clear()
            UpdateComponentWithViews::class -> updateWV?.clear()
            UpdateComponent::class -> update?.clear()
            ResizeComponent::class -> resize?.clear()
            else -> other?.removeAll { it::class == c }
        }
    }

    inline fun <reified T : Component> getOrCreateComponent(
        view: View,
        array: ArrayList<T>,
        clazz: KClass<out T>,
        gen: (View) -> T
    ): T {
        var component: T? = findFirstComponentOfType(array, clazz)
        if (component == null) {
            component = gen(view)
            array += component
        }
        return component
    }

    inline fun <reified T : Component> getOrCreateComponent(view: View, clazz: KClass<out T>, gen: (View) -> T): T =
        getOrCreateComponent(view, eother, clazz, gen) as T

    inline fun <reified T : MouseComponent> getOrCreateComponent(
        view: View,
        clazz: KClass<out T>,
        gen: (View) -> T
    ): T = getOrCreateComponent(view, emouse, clazz, gen) as T

    inline fun <reified T : KeyComponent> getOrCreateComponent(view: View, clazz: KClass<out T>, gen: (View) -> T): T =
        getOrCreateComponent(view, ekey, clazz, gen) as T

    inline fun <reified T : GamepadComponent> getOrCreateComponent(
        view: View,
        clazz: KClass<out T>,
        gen: (View) -> T
    ): T = getOrCreateComponent(view, egamepad, clazz, gen) as T

    inline fun <reified T : TouchComponent> getOrCreateComponent(
        view: View,
        clazz: KClass<out T>,
        gen: (View) -> T
    ): T = getOrCreateComponent(view, etouch, clazz, gen) as T

    inline fun <reified T : EventComponent> getOrCreateComponent(
        view: View,
        clazz: KClass<out T>,
        gen: (View) -> T
    ): T = getOrCreateComponent(view, eevent, clazz, gen) as T

    inline fun <reified T : UpdateComponentWithViews> getOrCreateComponent(
        view: View,
        clazz: KClass<out T>,
        gen: (View) -> T
    ): T = getOrCreateComponent(view, eupdateWV, clazz, gen) as T

    inline fun <reified T : UpdateComponent> getOrCreateComponent(
        view: View,
        clazz: KClass<out T>,
        gen: (View) -> T
    ): T = getOrCreateComponent(view, eupdate, clazz, gen) as T

    inline fun <reified T : ResizeComponent> getOrCreateComponent(
        view: View,
        clazz: KClass<out T>,
        gen: (View) -> T
    ): T = getOrCreateComponent(view, eresize, clazz, gen) as T

    inline fun <reified T : UpdateComponent> getComponentUpdate(): T? = findFirstComponentOfType(eupdate, T::class) as T?

    fun <T : Component> findFirstComponentOfType(array: ArrayList<T>, clazz: KClass<out T>): T? {
        array.fastForEach { if (it::class == clazz) return it }
        return null
    }
}
