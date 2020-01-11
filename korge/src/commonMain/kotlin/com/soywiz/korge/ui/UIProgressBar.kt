package com.soywiz.korge.ui

import com.soywiz.korge.view.*

inline fun Container.uiProgressBar(
	width: Number = 256.0,
	height: Number = 24.0,
	current: Number = 0.0,
	maximum: Number = 1.0,
	skin: UISkin = defaultUISkin,
	block: UIProgressBar.() -> Unit = {}
): UIProgressBar = UIProgressBar(
	width.toDouble(),
	height.toDouble(),
	current.toDouble(),
	maximum.toDouble(),
	skin
).also { addChild(it) }.also(block)

open class UIProgressBar(
	width: Double = 256.0,
	height: Double = 24.0,
	current: Double = 0.0,
	maximum: Double = 1.0,
	skin: UISkin = DefaultUISkin
) : UIView(width, height) {
	var current: Double by uiObservable(current) { onSizeChanged() }
	var maximum: Double by uiObservable(maximum) { onSizeChanged() }
	override var ratio: Double
		set(value) = run { current = value * maximum }
		get() = current / maximum

	private val bg = solidRect(width, height, skin.backColor)
	private val progress = uiTextButton(width, height, "", skin = skin).also { mouseEnabled = false }

	init {
		onSizeChanged()
	}

	override fun onSizeChanged() {
		bg.size(width, height)
		progress.forcePressed = true
		progress.size(width * ratio, height)
	}
}
