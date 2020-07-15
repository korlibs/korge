package com.soywiz.korio.lang

import kotlin.reflect.*

class lazyVar<T : Any>(val callback: () -> T) {
	var current: T? = null

	operator fun getValue(obj: Any, property: KProperty<*>): T {
		if (current == null) {
			current = callback()
		}
		return current!!
	}

	operator fun setValue(obj: Any, property: KProperty<*>, value: T) {
		current = value
	}
}
