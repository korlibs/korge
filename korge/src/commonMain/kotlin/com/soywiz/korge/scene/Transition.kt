package com.soywiz.korge.scene

import com.soywiz.korge.render.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
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
    var transition: Transition = AlphaTransition
    val prev: View get() = this[0]
	val next: View get() = this[1]

    /** Moves [next] to [prev], sets [next] and starts the [ratio] to 0.0 to start the transition. */
	fun startNewTransition(next: View) {
		this.ratio = 0.0
		setViews(this.next, next)
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
			else -> transition.render(ctx, prev, next, ratio)
		}
	}
}

/** Wraps a [render] method handling the render method of [TransitionView] receiving [Transition.prev] and [Transition.next] views */
class Transition(val render: (ctx: RenderContext, prev: View, next: View, ratio: Double) -> Unit)

/** Creates a new [Transition] with an [Easing] specified */
fun Transition.withEasing(easing: Easing) = Transition { ctx, prev, next, ratio ->
	this@withEasing.render(ctx, prev, next, easing(ratio))
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
