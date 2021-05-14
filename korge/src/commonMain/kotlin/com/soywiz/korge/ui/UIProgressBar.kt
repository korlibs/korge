package com.soywiz.korge.ui

import com.soywiz.kmem.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.ktree.*
import com.soywiz.korui.*

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

    object Serializer : KTreeSerializerExt<UIProgressBar>("UIProgressBar", UIProgressBar::class, { UIProgressBar() }, {
        add(UIProgressBar::current)
        add(UIProgressBar::maximum)
        add(UIProgressBar::width)
        add(UIProgressBar::height)
    })
}
