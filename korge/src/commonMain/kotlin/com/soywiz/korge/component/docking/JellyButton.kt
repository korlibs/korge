package com.soywiz.korge.component.docking

import com.soywiz.klock.*
import com.soywiz.korge.input.*
import com.soywiz.korge.time.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*

class JellyButton(val view: View?, var targetScale: Double = 1.5) {
	val hitTest = view["hitTest"] ?: view
	val content = view["content"] ?: view
	val initialScale = content?.scale ?: 1.0
	var down = false
	var over = false

	//val thread = AsyncThread()

	init {
		if (hitTest != content) {
			hitTest?.alpha = 0.0
		}
		//println("----------------")
		//println(hitTest?.globalBounds)
		//println(content?.globalBounds)
		//println(view?.globalBounds)

		hitTest?.onOver {
			over = true
			updateState()
		}
		hitTest?.onOut {
			over = false
			updateState()
		}
		hitTest?.onDown {
			down = true
			updateState()
		}
		hitTest?.onUpAnywhere {
			down = false
			updateState()
		}
	}

	suspend private fun updateState() {
		if (content == null) return
		val scale = when {
			down -> 1.0 / targetScale
			over -> targetScale
			else -> 1.0
		}
		content.tween(content::scale[initialScale * scale], time = 200.milliseconds, easing = Easing.EASE_OUT_ELASTIC)
	}

	fun onClick(callback: suspend () -> Unit) {
		hitTest?.onClick { callback() }
	}
}

fun View?.jellyButton(targetScale: Double = 1.5) = JellyButton(this, targetScale)
