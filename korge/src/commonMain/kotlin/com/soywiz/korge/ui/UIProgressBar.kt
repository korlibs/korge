package com.soywiz.korge.ui

import com.soywiz.kmem.clamp01
import com.soywiz.korge.debug.uiCollapsibleSection
import com.soywiz.korge.debug.uiEditableValue
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.NinePatchEx
import com.soywiz.korge.view.ViewDslMarker
import com.soywiz.korge.view.ViewLeaf
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.ninePatch
import com.soywiz.korge.view.size
import com.soywiz.korge.view.solidRect
import com.soywiz.korui.UiContainer

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

	var current by uiObservable(current) { updateState() }
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

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiCollapsibleSection(this@UIProgressBar::class.simpleName!!) {
            uiEditableValue(::current, min = 0.0, max = 100.0, clamp = false)
            uiEditableValue(::maximum, min = 1.0, max = 100.0, clamp = false)
        }
        super.buildDebugComponent(views, container)
    }
}
