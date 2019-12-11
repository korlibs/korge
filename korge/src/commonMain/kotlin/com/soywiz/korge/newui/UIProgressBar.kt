package com.soywiz.korge.newui

import com.soywiz.korge.view.*

inline fun Container.uiProgressBar(
	width: Number = 256.0,
	height: Number = 24.0,
	current: Number = 0.0,
	maximum: Number = 1.0,
	skin: UISkin = defaultUISkin,
	block: UIProgressBar.() -> Unit = {}
): UIProgressBar = UIProgressBar(width.toDouble(), height.toDouble(), current.toDouble(), maximum.toDouble(), skin).also { addChild(it) }.also(block)

open class UIProgressBar(
	width: Double = 256.0,
	height: Double = 24.0,
	current: Double = 0.0,
	maximum: Double = 1.0,
	skin: UISkin = DefaultUISkin
) : UIView(width, height) {
	var current: Double by uiObservable(current) { updatedSize() }
	var maximum: Double by uiObservable(maximum) { updatedSize() }
	override var ratio: Double
		set(value) = run { current = value * maximum }
		get() = current / maximum

	private val bg = solidRect(width, height, skin.backColor)
	private val progress = uiButton(width, height, "", skin = skin).also { mouseEnabled = false }

	init {
		updatedSize()
	}

	override fun updatedSize() {
		bg.size(width, height)
		progress.forcePressed = true
		progress.size(width * ratio, height)
	}
}
