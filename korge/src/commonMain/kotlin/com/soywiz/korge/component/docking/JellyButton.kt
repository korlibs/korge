package com.soywiz.korge.component.docking

import com.soywiz.klock.*
import com.soywiz.korge.input.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.time.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korma.interpolation.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

class JellyButton(val view: View?, val coroutineContext: CoroutineContext, var targetScale: Double = 1.5) {
	val hitTest = view["hitTest"].firstOrNull ?: view
	val content = view["content"].firstOrNull ?: view
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

	private fun updateState() {
		if (content == null) return
		val scale = when {
			down -> 1.0 / targetScale
			over -> targetScale
			else -> 1.0
		}
		CoroutineScope(coroutineContext).launchImmediately {
			content.tween(content::scale[initialScale * scale], time = 200.milliseconds, easing = Easing.EASE_OUT_ELASTIC)
		}
	}

	suspend fun onClick(callback: suspend () -> Unit) {
		hitTest?.mouse?.click?.addSuspend { callback() }
	}
}

suspend fun View?.jellyButton(targetScale: Double = 1.5) = JellyButton(this, coroutineContext, targetScale)
