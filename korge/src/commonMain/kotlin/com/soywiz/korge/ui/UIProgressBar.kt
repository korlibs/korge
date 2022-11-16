package com.soywiz.korge.ui

import com.soywiz.kmem.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.property.*

inline fun Container.uiProgressBar(
    width: Double = 256.0,
    height: Double = 24.0,
    current: Double = 0.0,
    maximum: Double = 100.0,
    block: @ViewDslMarker UIProgressBar.() -> Unit = {}
): UIProgressBar = UIProgressBar(width, height, current, maximum).addTo(this).apply(block)

open class UIProgressBar(
	width: Double = 256.0,
	height: Double = 24.0,
	current: Double = 0.0,
	maximum: Double = 100.0,
) : UIView(width, height), ViewLeaf {
    @ViewProperty(min = 0.0, max = 100.0)
	var current by uiObservable(current) { updateState() }
    @ViewProperty(min = 0.0, max = 100.0)
	var maximum by uiObservable(maximum) { updateState() }

	override var ratio: Double
		set(value) { current = value * maximum }
		get() = (current / maximum).clamp01()

	private val background = solidRect(width, height, buttonBackColor)
	protected open val progressView: NinePatchEx =
		ninePatch(buttonNormal, width * (current / maximum).clamp01(), height)

    override fun renderInternal(ctx: RenderContext) {
        background.size(width, height)
        progressView.size(width * ratio, height)
        progressView.ninePatch = buttonNormal
        background.color = buttonBackColor
        super.renderInternal(ctx)
    }
}
