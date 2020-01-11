package com.soywiz.korge.ui

import com.soywiz.korge.input.*
import com.soywiz.korge.view.*

inline fun Container.uiButton(
	width: Number = 128,
	height: Number = 64,
	skin: UISkin = defaultUISkin,
	block: @ViewsDslMarker UIButton.() -> Unit = {}
): UIButton = UIButton(width.toDouble(), height.toDouble(), skin).addTo(this).apply(block)

open class UIButton(
	width: Double = 128.0,
	height: Double = 64.0,
	skin: UISkin = DefaultUISkin
) : UIView(width, height) {

	var forcePressed by uiObservable(false) { updateState() }
	var skin: UISkin by uiObservable(skin) { updateState() }
	protected open val rect = ninePatch(skin.normal, width, height, 1.0 / 4.0, 1.0 / 4.0, 3.0 / 4.0, 3.0 / 4.0)

	private var bover by uiObservable(false) { updateState() }
	private var bpressing by uiObservable(false) { updateState() }

	override fun onEnabledChanged() {
		super.onEnabledChanged()
		updateState()
	}

	fun simulateHover() {
		bover = true
	}

	fun simulateOut() {
		bover = false
	}

	fun simulatePressing(value: Boolean) {
		bpressing = value
	}

	fun simulateDown() {
		bpressing = true
	}

	fun simulateUp() {
		bpressing = false
	}

	init {
		mouse {
			onOver {
				simulateHover()
			}
			onOut {
				simulateOut()
			}
			onDown {
				simulateDown()
			}
			onUpAnywhere {
				simulateUp()
			}
		}
		updateState()
	}

	private fun updateState() {
		when {
			!enabled -> {
				rect.tex = skin.disabled
			}
			bpressing || forcePressed -> {
				rect.tex = skin.down
			}
			bover -> {
				rect.tex = skin.hover
			}
			else -> {
				rect.tex = skin.normal
			}
		}
		onStateUpdated()
	}

	protected open fun onStateUpdated() {
	}

	override fun onSizeChanged() {
		super.onSizeChanged()
		rect.width = width
		rect.height = height
		updateState()
	}
}
