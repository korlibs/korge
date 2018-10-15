package com.soywiz.korge.ui

import com.soywiz.korge.view.FixedSizeContainer

abstract class Widget(val factory: UIFactory, val skin: UISkin = factory.skin) : FixedSizeContainer(factory.views) {
	override var width: Double = 100.0
		set(value) {
			field = value
			updateSize()
		}
	override var height: Double = 32.0
		set(value) {
			field = value
			updateSize()
		}

	protected open fun updateSize() {
	}
}
