package korlibs.korge.ui

import korlibs.korge.render.*
import korlibs.korge.style.*
import korlibs.korge.view.*
import korlibs.korge.view.property.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*

inline fun Container.uiProgressBar(
    size: Size = Size(256, 24),
    current: Number = 0.0,
    maximum: Number = 100.0,
    block: @ViewDslMarker UIProgressBar.() -> Unit = {}
): UIProgressBar = UIProgressBar(size, current.toDouble(), maximum.toDouble()).addTo(this).apply(block)

open class UIProgressBar(
    size: Size = Size(256, 24),
    current: Double = 0.0,
    maximum: Double = 100.0,
) : UIView(size), ViewLeaf {
    @ViewProperty(min = 0.0, max = 100.0)
	var current: Double by uiObservable(current) { updateState() }
    @ViewProperty(min = 0.0, max = 100.0)
	var maximum: Double by uiObservable(maximum) { updateState() }

    /** Property used for interpolable views like morph shapes, progress bars etc. */
    @ViewProperty(min = 0.0, max = 1.0, clampMin = false, clampMax = false)
	var ratio: Ratio
		set(value) { current = value * maximum }
		get() = Ratio(current, maximum).clamped

    override fun renderInternal(ctx: RenderContext) {
        styles.uiProgressBarRenderer.render(ctx)
        super.renderInternal(ctx)
    }

    override fun copyPropsFrom(source: View) {
        super.copyPropsFrom(source)
        this.ratio = (source as? UIProgressBar?)?.ratio ?: Ratio.ZERO
    }

}
