package com.soywiz.korge.input

import com.soywiz.korge.component.Component
import com.soywiz.korge.event.addEventListener
import com.soywiz.korge.view.*
import com.soywiz.korio.async.AsyncSignal
import com.soywiz.korio.async.go
import com.soywiz.korio.util.Extra

class KeysComponent(view: View) : Component(view) {
	val onKeyDown = AsyncSignal<KeyDownEvent>()
	val onKeyUp = AsyncSignal<KeyUpEvent>()
	val onKeyTyped = AsyncSignal<KeyTypedEvent>()

	init {
		this.detatchCancellables += view.addEventListener<KeyDownEvent> { go { onKeyDown(it) } }
		this.detatchCancellables += view.addEventListener<KeyUpEvent> { go { onKeyUp(it) } }
		this.detatchCancellables += view.addEventListener<KeyTypedEvent> { go { onKeyTyped(it) } }
	}
}

val View.keys by Extra.PropertyThis<View, KeysComponent> { this.getOrCreateComponent { KeysComponent(this) } }

inline fun <T : View?> T?.onKeyDown(noinline handler: suspend (KeyDownEvent) -> Unit) = this.apply { this?.keys?.onKeyDown?.add(handler) }
inline fun <T : View?> T?.onKeyUp(noinline handler: suspend (KeyUpEvent) -> Unit) = this.apply { this?.keys?.onKeyUp?.add(handler) }
inline fun <T : View?> T?.onKeyTyped(noinline handler: suspend (KeyTypedEvent) -> Unit) = this.apply { this?.keys?.onKeyTyped?.add(handler) }
