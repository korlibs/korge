package com.soywiz.korge.scene

import com.soywiz.korge.render.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.interpolation.*

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

    /** [Transition] that will be used to render [prev] and [next] */
    private var transition: Transition = AlphaTransition
    private lateinit var transitionProcess: TransitionProcess
    val prev: View get() = this[0]
	val next: View get() = this[1]

    /** Moves [next] to [prev], sets [next] and starts the [ratio] to 0.0 to start the transition. */
	fun startNewTransition(next: View, transition: Transition = this.transition) {
        this.transition = transition
		this.ratio = 0.0
        this.transitionProcess = transition.create()
		setViews(this.next, next)
        this.transitionProcess.start(this.prev, this.next)
	}

    fun endTransition() {
        this.ratio = 1.0
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
			ratio <= 0.0 -> prev.render(ctx)
			ratio >= 1.0 -> next.render(ctx)
			else -> this.transitionProcess.render(ctx, prev, next, ratio)
		}
	}
}

interface TransitionProcess {
    fun start(prev: View, next: View) = Unit
    fun end(prev: View, next: View) = Unit
    fun render(ctx: RenderContext, prev: View, next: View, ratio: Double) = Unit
}

interface Transition {
    fun create(): TransitionProcess
}

fun TransitionProcess(render: (ctx: RenderContext, prev: View, next: View, ratio: Double) -> Unit): TransitionProcess =
    object : TransitionProcess {
        override fun render(ctx: RenderContext, prev: View, next: View, ratio: Double) = render(ctx, prev, next, ratio)
    }

fun Transition(render: (ctx: RenderContext, prev: View, next: View, ratio: Double) -> Unit): Transition {
    return object : Transition, TransitionProcess {
        override fun create(): TransitionProcess = this
        override fun render(ctx: RenderContext, prev: View, next: View, ratio: Double) = render(ctx, prev, next, ratio)
    }
}

fun TransitionCreate(block: () -> TransitionProcess): Transition = object : Transition {
    override fun create(): TransitionProcess = block()
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
    override fun create(): TransitionProcess {
        val process = this@withEasing.create()
        return object : TransitionProcess {
            override fun start(prev: View, next: View) = process.start(prev, next)
            override fun end(prev: View, next: View) = process.end(prev, next)
            override fun render(ctx: RenderContext, prev: View, next: View, ratio: Double) =
                process.render(ctx, prev, next, easing(ratio))
        }
    }

}

/** A [Transition] that will blend [prev] and [next] by adjusting its alphas */
val AlphaTransition = Transition { ctx, prev, next, ratio ->
	val prevAlpha = prev.alpha
	val nextAlpha = next.alpha
	try {
		prev.alpha = 1.0 - ratio
		next.alpha = ratio
		prev.render(ctx)
		next.render(ctx)
	} finally {
		prev.alpha = prevAlpha
		next.alpha = nextAlpha
	}
}

/**
 * A [Transition] that will use a [transition] [TransitionFilter.Transition] to show the new scene.
 */
fun MaskTransition(
    transition: TransitionFilter.Transition = TransitionFilter.Transition.CIRCULAR,
    reversed: Boolean = false,
    smooth: Boolean = true,
) = TransitionCreate {
    val filter = TransitionFilter(transition, reversed, smooth)
    TransitionProcess { ctx, prev, next, ratio ->
            filter.ratio = ratio
            prev.render(ctx)
            next.renderFiltered(ctx, filter)
    }
}
