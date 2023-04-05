package korlibs.korge.ui

import korlibs.korge.render.*
import korlibs.korge.style.*
import korlibs.korge.view.*
import korlibs.korge.view.property.*
import korlibs.memory.*

inline fun Container.uiProgressBar(
    width: Float = 256f,
    height: Float = 24f,
    current: Float = 0f,
    maximum: Float = 100f,
    block: @ViewDslMarker UIProgressBar.() -> Unit = {}
): UIProgressBar = UIProgressBar(width, height, current, maximum).addTo(this).apply(block)

open class UIProgressBar(
	width: Float = 256f,
	height: Float = 24f,
	current: Float = 0f,
	maximum: Float = 100f,
) : UIView(width, height), ViewLeaf {
    @ViewProperty(min = 0.0, max = 100.0)
	var current: Float by uiObservable(current) { updateState() }
    @ViewProperty(min = 0.0, max = 100.0)
	var maximum: Float by uiObservable(maximum) { updateState() }

	override var ratio: Float
		set(value) { current = value * maximum }
		get() = (current / maximum).clamp01()

    override fun renderInternal(ctx: RenderContext) {
        styles.uiProgressBarRenderer.render(ctx)
        super.renderInternal(ctx)
    }
}
