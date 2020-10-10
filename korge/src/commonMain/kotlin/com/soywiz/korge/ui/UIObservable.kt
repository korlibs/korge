package com.soywiz.korge.ui

import kotlin.reflect.*

inline fun <T> uiObservable(value: T, noinline observe: (T) -> Unit) = UIObservable(value, observe)

class UIObservable<T>(val initial: T, val observe: (T) -> Unit) {
	var currentValue = initial

	operator fun getValue(obj: Any, prop: KProperty<*>): T {
		return currentValue
	}

	operator fun setValue(obj: Any, prop: KProperty<*>, value: T) {
        if (currentValue != value) {
            currentValue = value
            observe(value)
        }
	}
}
