package com.soywiz.korge.input

import com.soywiz.kds.*
import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korev.*

class KeysEvents(override val view: View) : KeyComponent {
	val onKeyDown = AsyncSignal<KeyEvent>()
	val onKeyUp = AsyncSignal<KeyEvent>()
	val onKeyTyped = AsyncSignal<KeyEvent>()

	fun down(key: Key, callback: (key: Key) -> Unit): Closeable = onKeyDown { e -> if (e.key == key) callback(key) }
	fun up(key: Key, callback: (key: Key) -> Unit): Closeable = onKeyUp { e -> if (e.key == key) callback(key) }
	fun typed(key: Key, callback: (key: Key) -> Unit): Closeable = onKeyTyped { e -> if (e.key == key) callback(key) }

	fun down(callback: (key: Key) -> Unit): Closeable = onKeyDown { e -> callback(e.key) }
	fun up(callback: (key: Key) -> Unit): Closeable = onKeyUp { e -> callback(e.key) }
	fun typed(callback: (key: Key) -> Unit): Closeable = onKeyTyped { e -> callback(e.key) }

	override fun onKeyEvent(views: Views, event: KeyEvent) {
		when (event.type) {
			KeyEvent.Type.TYPE -> launchImmediately(views.coroutineContext) { onKeyTyped.invoke(event) }
			KeyEvent.Type.DOWN -> launchImmediately(views.coroutineContext) { onKeyDown.invoke(event) }
			KeyEvent.Type.UP -> launchImmediately(views.coroutineContext) { onKeyUp.invoke(event) }
		}
	}
}

val View.keys by Extra.PropertyThis<View, KeysEvents> { this.getOrCreateComponent { KeysEvents(this) } }
inline fun <T> View.keys(callback: KeysEvents.() -> T): T = keys.run(callback)

inline fun <T : View?> T?.onKeyDown(noinline handler: suspend (KeyEvent) -> Unit) =
	this.apply { this?.keys?.onKeyDown?.add(handler) }

inline fun <T : View?> T?.onKeyUp(noinline handler: suspend (KeyEvent) -> Unit) =
	this.apply { this?.keys?.onKeyUp?.add(handler) }

inline fun <T : View?> T?.onKeyTyped(noinline handler: suspend (KeyEvent) -> Unit) =
	this.apply { this?.keys?.onKeyTyped?.add(handler) }
