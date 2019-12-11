package com.soywiz.korge.newui

import com.soywiz.korge.html.*
import com.soywiz.korge.input.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import kotlin.properties.*

inline fun Container.uiButton(
	width: Number = 128,
	height: Number = 64,
	label: String = "Button",
	skin: UISkin = defaultUISkin,
	block: UIButton.() -> Unit = {}
): UIButton = UIButton(width.toDouble(), height.toDouble(), label, skin).also { addChild(it) }.apply(block)

open class UIButton(
	width: Double = 128.0,
	height: Double = 64.0,
	label: String = "Button",
	skin: UISkin = DefaultUISkin
) : UIView(width, height) {
	var forcePressed  by uiObservable(false) { updateState() }
	var skin: UISkin by uiObservable(skin) { updateState() }
	var label by uiObservable(label) { updateState() }
	private val rect = ninePatch(skin.normal, width, height, 16.0 / 64.0, 16.0 / 64.0, (64.0 - 16.0) / 64.0, (64.0 - 16.0) / 64.0) {}
	private val textShadow = text(label).also { it.position(1, 1) }
	private val text = text(label)
	private var bover by uiObservable(false) { updateState() }
	private var bpressing by uiObservable(false) { updateState() }

	// @TODO: Make mouseEnabled open
	//override var mouseEnabled = uiObservable(true) { updateState() }

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
		text.format = Html.Format(face = skin.font, align = Html.Alignment.MIDDLE_CENTER, color = Colors.WHITE)
		text.setTextBounds(Rectangle(0, 0, width, height))
		text.setText(label)
		textShadow.format = Html.Format(face = skin.font, align = Html.Alignment.MIDDLE_CENTER, color = Colors.BLACK.withA(64))
		textShadow.setTextBounds(Rectangle(0, 0, width, height))
		textShadow.setText(label)
	}

	override fun updatedSize() {
		super.updatedSize()
		rect.width = width
		rect.height = height
		updateState()
	}

	override fun renderInternal(ctx: RenderContext) {
		//alpha = if (mouseEnabled) 1.0 else 0.5
		super.renderInternal(ctx)
	}
}
