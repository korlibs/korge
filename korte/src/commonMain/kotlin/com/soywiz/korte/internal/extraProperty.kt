package com.soywiz.korte.internal

import kotlin.reflect.*

internal class extraProperty<R, T : Any>(val getExtraMap: R.() -> MutableMap<String, Any>, val name: String? = null, val default: () -> T) {
	inline operator fun getValue(thisRef: R, property: KProperty<*>): T =
		getExtraMap(thisRef)[name ?: property.name] as T? ?: default()

	inline operator fun setValue(thisRef: R, property: KProperty<*>, value: T): Unit = run {
		getExtraMap(thisRef)[name ?: property.name] = value
	}
}
