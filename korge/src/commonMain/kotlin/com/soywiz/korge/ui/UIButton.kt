package com.soywiz.korge.ui

import com.soywiz.korge.input.*
import com.soywiz.korge.view.*

@Deprecated("Kotlin/Native boxes inline+Number")
inline fun Container.uiButton(
	width: Number,
	height: Number,
	skin: UISkin = defaultUISkin,
	block: @ViewsDslMarker UIButton.() -> Unit = {}
): UIButton = uiButton(width.toDouble(), height.toDouble(), skin, block)

inline fun Container.uiButton(
    width: Double = 128.0,
    height: Double = 64.0,
    skin: UISkin = defaultUISkin,
    block: @ViewsDslMarker UIButton.() -> Unit = {}
): UIButton = UIButton(width, height, skin).addTo(this).apply(block)

open class UIButton(
	width: Double = 128.0,
	height: Double = 64.0,
	skin: UISkin = DefaultUISkin
) : UIView(width, height) {

	var forcePressed by uiObservable(false) { updateState() }
	var skin: UISkin by uiObservable(skin) { updateState() }
	protected open val rect = ninePatch(skin.normal, width, height, 10.0 / 64.0, 10.0 / 64.0, 54.0 / 64.0, 54.0 / 64.0)

	protected var bover by uiObservable(false) { updateState() }
	protected var bpressing by uiObservable(false) { updateState() }

	fun simulateOver() {
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
				simulateOver()
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
	}

	override fun updateState() {
		rect.tex = when {
			!enabled -> skin.disabled
			bpressing || forcePressed -> skin.down
			bover -> skin.over
			else -> skin.normal
		}
	}

	override fun onSizeChanged() {
		super.onSizeChanged()
		rect.width = width
		rect.height = height
		updateState()
	}
}
