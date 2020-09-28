package com.soywiz.korge.ui

import com.soywiz.kds.*
import com.soywiz.klock.TimeSpan
import com.soywiz.korge.baseview.*
import com.soywiz.korge.component.*
import com.soywiz.korge.input.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*

open class UIView(
	width: Double = 90.0,
	height: Double = 32.0
) : Container() {

	override var width: Double by uiObservable(width) { onSizeChanged() }
	override var height: Double by uiObservable(height) { onSizeChanged() }

	var enabled
		get() = mouseEnabled
		set(value) {
			mouseEnabled = value
			updateState()
		}

	fun enable(set: Boolean = true) {
		enabled = set
	}

	fun disable() {
		enabled = false
	}

	protected open fun onSizeChanged() {
	}

	open fun updateState() {
	}

	override fun renderInternal(ctx: RenderContext) {
		registerUISupportOnce()
		super.renderInternal(ctx)
	}

	private var registered = false
	private fun registerUISupportOnce() {
		if (registered) return
		val stage = stage ?: return
		registered = true
		if (stage.getExtra("uiSupport") == true) return
		stage.setExtra("uiSupport", true)
		stage.keys {
			onKeyDown {

			}
		}
		stage.getOrCreateComponentUpdateWithViews<DummyUpdateComponentWithViews> { stage ->
            DummyUpdateComponentWithViews(stage)
		}
	}
}

internal class DummyUpdateComponentWithViews(override val view: BaseView) : UpdateComponentWithViews {
    override fun update(views: Views, dt: TimeSpan) {
    }
}
