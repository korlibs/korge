package com.soywiz.korge.newui

import com.soywiz.kds.*
import com.soywiz.korge.component.*
import com.soywiz.korge.input.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import kotlin.properties.*

open class UIView(
	width: Double = 90.0,
	height: Double = 32.0
) : Container() {
	override var width: Double by uiObservable(width) { updatedSize() }
	override var height: Double by uiObservable(height) { updatedSize() }

	open var uiEnabled by uiObservable(true) { updateEnabled() }
	open var uiDisabled: Boolean
		set(value) = run { uiEnabled = !value }
		get() = !uiEnabled

	fun enable(set: Boolean = true) = run { uiEnabled = set }
	fun disable() = run { uiEnabled = false }

	protected open fun updatedSize() {
	}

	protected open fun updateEnabled() {
		mouseEnabled = uiEnabled
		// @TODO: Shouldn't change alpha
		alpha = if (uiEnabled) 1.0 else 0.7
	}

	override fun renderInternal(ctx: RenderContext) {
		registerUISupportOnce()
		super.renderInternal(ctx)
	}

	private var registered = false
	private fun registerUISupportOnce() {
		if (registered) return
		val stage = stage ?: return
		registered = true
		if (stage.getExtra("uiSupport") == true) return
		stage.setExtra("uiSupport", true)
		stage.keys {
			onKeyDown {

			}
		}
		stage?.getOrCreateComponent { stage ->
			object : UpdateComponentWithViews {
				override val view: View = stage
				override fun update(views: Views, ms: Double) {
				}
			}
		}
	}

}
