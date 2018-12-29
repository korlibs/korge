package com.soywiz.korge.scene

import com.soywiz.korge.render.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korma.interpolation.*

class TransitionView() : Container() {
	var transition: Transition = AlphaTransition
	val dummy1 = Container()
	val dummy2 = Container()

	init {
		addChild(dummy1)
		addChild(dummy2)
	}

	val prev: View get() = children[0]
	val next: View get() = children[1]

	fun startNewTransition(next: View) {
		this.ratio = 0.0
		setViews(this.next, next)
	}

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

class Transition(val render: (ctx: RenderContext, prev: View, next: View, ratio: Double) -> Unit)

fun Transition.withEasing(easing: Easing) = Transition { ctx, prev, next, ratio ->
	this@withEasing.render(ctx, prev, next, easing(ratio))
}

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
