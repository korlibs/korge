package com.soywiz.korge.util

import kotlin.reflect.*

class Observable<T>(val initial: T, val before: (T) -> Unit = {}, val after: (T) -> Unit = {}) {
	var currentValue = initial

	operator fun getValue(obj: Any, prop: KProperty<*>): T {
		return currentValue
	}

	operator fun setValue(obj: Any, prop: KProperty<*>, value: T) {
        before(value)
		currentValue = value
		after(value)
	}
}
