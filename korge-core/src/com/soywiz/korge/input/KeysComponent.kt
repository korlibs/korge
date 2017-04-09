package com.soywiz.korge.input

import com.soywiz.korge.component.Component
import com.soywiz.korge.view.View
import com.soywiz.korio.async.Signal

class KeysComponent(view: View) : Component(view) {
	val onKeyDown = Signal<Keys>()
	val onKeyUp = Signal<Keys>()
}

val View.keys get() = this.getOrCreateComponent { KeysComponent(this) }
val View.onKeyDown get() = this.keys.onKeyDown
val View.onKeyUp get() = this.keys.onKeyUp
