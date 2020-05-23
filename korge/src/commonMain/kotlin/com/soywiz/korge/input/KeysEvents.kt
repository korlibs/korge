package com.soywiz.korge.input

import com.soywiz.kds.*
import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korev.*
import kotlin.reflect.*

class KeysEvents(override val view: View) : KeyComponent {
    @PublishedApi
    internal lateinit var views: Views
    @PublishedApi
    internal val coroutineContext get() = views.coroutineContext

    val onKeyDown = AsyncSignal<KeyEvent>()
	val onKeyUp = AsyncSignal<KeyEvent>()
	val onKeyTyped = AsyncSignal<KeyEvent>()

	fun down(key: Key, callback: suspend (key: Key) -> Unit): Closeable = onKeyDown { e -> if (e.key == key) callback(key) }
	fun up(key: Key, callback: suspend (key: Key) -> Unit): Closeable = onKeyUp { e -> if (e.key == key) callback(key) }
	fun typed(key: Key, callback: suspend (key: Key) -> Unit): Closeable = onKeyTyped { e -> if (e.key == key) callback(key) }

	fun down(callback: suspend (key: Key) -> Unit): Closeable = onKeyDown { e -> callback(e.key) }
	fun up(callback: suspend (key: Key) -> Unit): Closeable = onKeyUp { e -> callback(e.key) }
	fun typed(callback: suspend (key: Key) -> Unit): Closeable = onKeyTyped { e -> callback(e.key) }

	override fun onKeyEvent(views: Views, event: KeyEvent) {
        this.views = views
		when (event.type) {
			KeyEvent.Type.TYPE -> launchImmediately(views.coroutineContext) { onKeyTyped.invoke(event) }
			KeyEvent.Type.DOWN -> launchImmediately(views.coroutineContext) { onKeyDown.invoke(event) }
			KeyEvent.Type.UP -> launchImmediately(views.coroutineContext) { onKeyUp.invoke(event) }
		}
	}
}

val View.keys by Extra.PropertyThis<View, KeysEvents> { this.getOrCreateComponent<KeysEvents> { KeysEvents(this) } }
inline fun <T> View.keys(callback: KeysEvents.() -> T): T = keys.run(callback)

@PublishedApi
internal inline fun <T : View?> T._keyEvent(prop: KProperty1<KeysEvents, AsyncSignal<KeyEvent>>, noinline handler: suspend (KeyEvent) -> Unit): T {
    this?.keys?.let { keys -> prop.get(keys).add(handler) }
    return this
}

inline fun <T : View?> T.onKeyDown(noinline handler: suspend (KeyEvent) -> Unit): T = _keyEvent(KeysEvents::onKeyDown, handler)
inline fun <T : View?> T.onKeyUp(noinline handler: suspend (KeyEvent) -> Unit): T = _keyEvent(KeysEvents::onKeyUp, handler)
inline fun <T : View?> T.onKeyTyped(noinline handler: suspend (KeyEvent) -> Unit): T = _keyEvent(KeysEvents::onKeyTyped, handler)
