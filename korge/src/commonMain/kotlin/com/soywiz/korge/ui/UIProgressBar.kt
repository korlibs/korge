package com.soywiz.korge.ui

import com.soywiz.korge.view.*

@Deprecated("Kotlin/Native boxes inline+Number")
inline fun Container.uiProgressBar(
	width: Number, height: Number, current: Number, maximum: Number,
	skin: UISkin = defaultUISkin,
	block: @ViewDslMarker UIProgressBar.() -> Unit = {}
): UIProgressBar = uiProgressBar(width.toDouble(), height.toDouble(), current.toDouble(), maximum.toDouble(), skin, block)

inline fun Container.uiProgressBar(
    width: Double = 256.0,
    height: Double = 24.0,
    current: Double = 0.0,
    maximum: Double = 1.0,
    skin: UISkin = defaultUISkin,
    block: @ViewDslMarker UIProgressBar.() -> Unit = {}
): UIProgressBar = UIProgressBar(width, height, current, maximum, skin).addTo(this).apply(block)

open class UIProgressBar(
	width: Double = 256.0,
	height: Double = 24.0,
	current: Double = 0.0,
	maximum: Double = 1.0,
	skin: UISkin = DefaultUISkin
) : UIView(width, height) {

	var current by uiObservable(current) { updateState() }
	var maximum by uiObservable(maximum) { updateState() }
	var skin by uiObservable(skin) {
		background.color = it.backColor
		onSkinChanged()
	}

	override var ratio: Double
		set(value) { current = value * maximum }
		get() = current / maximum

	private val background = solidRect(width, height, skin.backColor)
	protected open val progressView: View =
		ninePatch(skin.normal, width * current / maximum, height, 1.0 / 4.0, 1.0 / 4.0, 3.0 / 4.0, 3.0 / 4.0)

	override fun onSizeChanged() {
		background.size(width, height)
		updateState()
	}

	override fun updateState() {
		progressView.size(width * ratio, height)
	}

	protected open fun onSkinChanged() {
		(progressView as? NinePatch)?.tex = skin.normal
		background.color = skin.backColor
	}
}
