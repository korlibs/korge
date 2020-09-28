package com.soywiz.korge.input

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korev.*
import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import kotlin.reflect.*

class KeysEvents(override val view: View) : KeyComponent {
    @PublishedApi
    internal lateinit var views: Views
    @PublishedApi
    internal val coroutineContext get() = views.coroutineContext

    val onKeyDown = AsyncSignal<KeyEvent>()
	val onKeyUp = AsyncSignal<KeyEvent>()
	val onKeyTyped = AsyncSignal<KeyEvent>()

    /** Executes [callback] on each frame when [key] is being pressed. When [dt] is provided, the [callback] is executed at that [dt] steps. */
    fun downFrame(key: Key, dt: TimeSpan = TimeSpan.NIL, callback: (ke: KeyEvent) -> Unit): Cancellable {
        val ke = KeyEvent()
        return view.addOptFixedUpdater(dt) { dt ->
            val views = view.stage?.views
            if (views != null) {
                val keys = views.keys
                if (keys[key]) {
                    ke.type = KeyEvent.Type.DOWN
                    ke.key = key
                    ke.keyCode = key.ordinal
                    ke.shift = keys[Key.LEFT_SHIFT] || keys[Key.RIGHT_SHIFT]
                    ke.ctrl = keys[Key.LEFT_CONTROL] || keys[Key.RIGHT_CONTROL]
                    ke.meta = keys[Key.META]
                    ke.deltaTime = dt
                    callback(ke)
                }
            }
            //if (view.input)
        }
    }

    fun down(callback: suspend (key: KeyEvent) -> Unit): Closeable = onKeyDown { e -> callback(e) }
    fun down(key: Key, callback: suspend (key: KeyEvent) -> Unit): Closeable = onKeyDown { e -> if (e.key == key) callback(e) }

    fun up(callback: suspend (key: KeyEvent) -> Unit): Closeable = onKeyUp { e -> callback(e) }
    fun up(key: Key, callback: suspend (key: KeyEvent) -> Unit): Closeable = onKeyUp { e -> if (e.key == key) callback(e) }

    fun typed(callback: suspend (key: KeyEvent) -> Unit): Closeable = onKeyTyped { e -> callback(e) }
    fun typed(key: Key, callback: suspend (key: KeyEvent) -> Unit): Closeable = onKeyTyped { e -> if (e.key == key) callback(e) }

    override fun onKeyEvent(views: Views, event: KeyEvent) {
        this.views = views
		when (event.type) {
			KeyEvent.Type.TYPE -> launchImmediately(views.coroutineContext) { onKeyTyped.invoke(event) }
			KeyEvent.Type.DOWN -> launchImmediately(views.coroutineContext) { onKeyDown.invoke(event) }
			KeyEvent.Type.UP -> launchImmediately(views.coroutineContext) { onKeyUp.invoke(event) }
		}
	}
}

val View.keys by Extra.PropertyThis<View, KeysEvents> { this.getOrCreateComponentKey<KeysEvents> { KeysEvents(this) } }
inline fun <T> View.keys(callback: KeysEvents.() -> T): T = keys.run(callback)

@PublishedApi
internal inline fun <T : View?> T._keyEvent(prop: KProperty1<KeysEvents, AsyncSignal<KeyEvent>>, noinline handler: suspend (KeyEvent) -> Unit): T {
    this?.keys?.let { keys -> prop.get(keys).add(handler) }
    return this
}

inline fun <T : View?> T.onKeyDown(noinline handler: suspend (KeyEvent) -> Unit): T = _keyEvent(KeysEvents::onKeyDown, handler)
inline fun <T : View?> T.onKeyUp(noinline handler: suspend (KeyEvent) -> Unit): T = _keyEvent(KeysEvents::onKeyUp, handler)
inline fun <T : View?> T.onKeyTyped(noinline handler: suspend (KeyEvent) -> Unit): T = _keyEvent(KeysEvents::onKeyTyped, handler)
