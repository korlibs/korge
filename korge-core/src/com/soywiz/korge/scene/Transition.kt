package com.soywiz.korge.scene

import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.tween.Easing
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korma.Matrix2d

class TransitionView(views: Views) : Container(views) {
	var transition: Transition = AlphaTransition
	val dummy1 = views.container()
	val dummy2 = views.container()

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

	override fun render(ctx: RenderContext, m: Matrix2d) {
		when {
			ratio <= 0.0 -> prev.render(ctx, m)
			ratio >= 1.0 -> next.render(ctx, m)
			else -> transition.render(ctx, m, prev, next, ratio)
		}
	}
}

class Transition(val render: (ctx: RenderContext, m: Matrix2d, prev: View, next: View, ratio: Double) -> Unit)

fun Transition.withEasing(easing: Easing) = Transition { ctx, m, prev, next, ratio ->
	this@withEasing.render(ctx, m, prev, next, easing(ratio))
}

val AlphaTransition = Transition { ctx, m, prev, next, ratio ->
	val prevAlpha = prev.alpha
	val nextAlpha = next.alpha
	try {
		prev.alpha = 1.0 - ratio
		next.alpha = ratio
		prev.render(ctx, m)
		next.render(ctx, m)
	} finally {
		prev.alpha = prevAlpha
		next.alpha = nextAlpha
	}
}
