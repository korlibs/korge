package com.soywiz.korge.intellij.util

import com.soywiz.korio.async.Signal
import kotlin.reflect.KProperty

class ObservableProperty<T>(val initial: T, var adjust: (T) -> T = { it }) {
	val changed = Signal<T>()

	fun addAdjuster(adjust: (T) -> T) {
		val oldAdjust = this.adjust
		this.adjust = { adjust(oldAdjust(it)) }
	}

	var value: T = adjust(initial)
		set(value) {
			val newValue = adjust(value)
			if (field != newValue) {
				field = newValue
				changed(field)
			}
		}

	fun trigger() = changed(value)

	operator fun invoke(handler: (T) -> Unit) {
		changed(handler)
	}

	operator fun getValue(obj: Any?, property: KProperty<*>): T {
		return value
	}

	operator fun setValue(obj: Any?, property: KProperty<*>, value: T) {
		this.value = value
	}
}
