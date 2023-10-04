package korlibs.korge.scene

import korlibs.image.bitmap.*
import korlibs.korge.render.*
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.korge.view.filter.*
import korlibs.korge.view.property.*
import korlibs.math.interpolation.*
import kotlin.native.concurrent.*

/**
 * A View that will render [prev] and [next] views using the specified [transition].
 * You can set the [prev] and [next] views by calling [setViews].
 */
class TransitionView() : Container() {
    init {
        // Dummy instances to always have two [View] instances
        dummyView()
        dummyView()
    }

    @ViewProperty(min = 0.0, max = 1.0, clampMin = false, clampMax = false)
    var ratio: Ratio = Ratio.ZERO

    /** [Transition] that will be used to render [prev] and [next] */
    internal var transition: Transition = AlphaTransition
        private set
    private lateinit var transitionProcess: TransitionProcess
    val prev: View get() = this[0]
	val next: View get() = this[1]

    /** Moves [next] to [prev], sets [next] and starts the [ratio] to 0.0 to start the transition. */
	fun startNewTransition(next: View, transition: Transition = this.transition) {
        this.transition = transition
		this.ratio = Ratio.ZERO
        this.transitionProcess = transition.create()
		setViews(this.next, next)
        this.transitionProcess.start(this.prev, this.next)
	}

    override fun toString(): String = super.toString() + ":ratio=$ratio:transition=$transition"

    fun endTransition() {
        this.ratio = Ratio.ONE

        this.transitionProcess.end(this.prev, this.next)
        setViews(dummyView(), next)
    }

    /** Changes the views with [prev] and [next] */
	fun setViews(prev: View, next: View) {
		this.removeChildren()
		this.addChild(prev)
		this.addChild(next)
	}

	override fun renderInternal(ctx: RenderContext) {
		when {
			ratio <= Ratio.ZERO -> prev.render(ctx)
			ratio >= Ratio.ONE -> next.render(ctx)
			else -> this.transitionProcess.render(ctx, prev, next, ratio)
		}
	}
}

interface TransitionProcess {
    fun start(prev: View, next: View) = Unit
    fun end(prev: View, next: View) = Unit
    fun render(ctx: RenderContext, prev: View, next: View, ratio: Ratio) = Unit
}

interface Transition {
    fun create(): TransitionProcess
}

fun TransitionProcess(name: String = "Transition", render: (ctx: RenderContext, prev: View, next: View, ratio: Ratio) -> Unit): TransitionProcess =
    object : TransitionProcess {
        override fun render(ctx: RenderContext, prev: View, next: View, ratio: Ratio) = render(ctx, prev, next, ratio)
        override fun toString(): String = name
    }

fun Transition(name: String = "Transition", render: (ctx: RenderContext, prev: View, next: View, ratio: Ratio) -> Unit): Transition {
    return object : Transition, TransitionProcess {
        override fun create(): TransitionProcess = this
        override fun render(ctx: RenderContext, prev: View, next: View, ratio: Ratio) = render(ctx, prev, next, ratio)
        override fun toString(): String = name
    }
}

fun TransitionCreate(name: String = "Transition", block: () -> TransitionProcess): Transition = object : Transition {
    override fun create(): TransitionProcess = block()
    override fun toString(): String = name
}

/*
/** Wraps a [render] method handling the render method of [TransitionView] receiving [Transition.prev] and [Transition.next] views */
class Transition(
    val start: (prev: View, next: View) -> Unit = { prev, next -> },
    val end: (prev: View, next: View) -> Unit = { prev, next -> },
    val render: (ctx: RenderContext, prev: View, next: View, ratio: Double) -> Unit
)
*/

/** Creates a new [Transition] with an [Easing] specified */
fun Transition.withEasing(easing: Easing) = object : Transition {
    override fun toString(): String = "${this@withEasing}.withEasing"

    override fun create(): TransitionProcess {
        val process = this@withEasing.create()
        return object : TransitionProcess {
            override fun start(prev: View, next: View) = process.start(prev, next)
            override fun end(prev: View, next: View) = process.end(prev, next)
            override fun render(ctx: RenderContext, prev: View, next: View, ratio: Ratio) =
                process.render(ctx, prev, next, easing(ratio))
        }
    }

}

/** A [Transition] that will blend [prev] and [next] by adjusting its alphas */
@SharedImmutable
val AlphaTransition = Transition("AlphaTransition") { ctx, prev, next, ratio ->
	val prevAlpha = prev.alphaF
	val nextAlpha = next.alphaF
	try {
		prev.alphaF = 1f - ratio.toFloat()
		next.alphaF = ratio.toFloat()
		prev.render(ctx)
		next.render(ctx)
	} finally {
		prev.alphaF = prevAlpha
		next.alphaF = nextAlpha
	}
}

/**
 * A [Transition] that will use a [transition] [TransitionFilter.Transition] to show the new scene.
 */
fun MaskTransition(
    transition: TransitionFilter.Transition = TransitionFilter.Transition.CIRCULAR,
    reversed: Boolean = false,
    spread: Double = 1.0,
    filtering: Boolean = true,
) = TransitionCreate("MaskTransition") {
    val filter = TransitionFilter(transition, reversed, spread, filtering = filtering)
    TransitionProcess("MaskTransition") { ctx, prev, next, ratio ->
            filter.ratio = ratio
            prev.render(ctx)
            next.renderFiltered(ctx, filter)
    }
}
