package com.soywiz.korge.ui

import com.soywiz.korge.html.*
import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*

inline fun Container.uiButton(
	width: Number = 128,
	height: Number = 64,
	label: String = "Button",
	skin: UISkin = defaultUISkin,
	block: UIButton.() -> Unit = {}
): UIButton = UIButton(width.toDouble(), height.toDouble(), label, skin).addTo(this).apply(block)

open class UIButton(
	width: Double = 128.0,
	height: Double = 64.0,
	label: String = "Button",
	skin: UISkin = DefaultUISkin
) : UIView(width, height) {
	var forcePressed by uiObservable(false) { updateState() }
	var skin: UISkin by uiObservable(skin) { updateState() }
	var label by uiObservable(label) { updateState() }
	var textSize by uiObservable(16) { updateState() }
	var textColor by uiObservable(Colors.WHITE) { updateState() }
	var textAlignment by uiObservable(Html.Alignment.MIDDLE_CENTER) { updateState() }
	var shadowX by uiObservable(1) { updateState() }
	var shadowY by uiObservable(1) { updateState() }
	var shadowSize by uiObservable(16) { updateState() }
	var shadowColor by uiObservable(Colors.BLACK.withA(64)) { updateState() }
	var shadowVisible by uiObservable(true) { updateState() }
	protected open val rect = ninePatch(skin.normal, width, height, 1.0 / 4.0, 1.0 / 4.0, 3.0 / 4.0, 3.0 / 4.0)
	private val text = text(label)
	private val textShadow = text(label)
	private var bover by uiObservable(false) { updateState() }
	private var bpressing by uiObservable(false) { updateState() }

	override var mouseEnabled = true
		set(value) {
			field = value
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
			!mouseEnabled -> {
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
		text.format = Html.Format(face = skin.font, align = textAlignment, color = textColor, size = textSize)
		text.setTextBounds(Rectangle(0, 0, width, height))
		text.setText(label)
		textShadow.visible = shadowVisible
		textShadow.format = Html.Format(face = skin.font, align = textAlignment, color = shadowColor, size = shadowSize)
		textShadow.setTextBounds(Rectangle(0, 0, width, height))
		textShadow.setText(label)
		textShadow.position(shadowX, shadowY)
	}

	override fun onSizeChanged() {
		super.onSizeChanged()
		rect.width = width
		rect.height = height
		updateState()
	}
}
