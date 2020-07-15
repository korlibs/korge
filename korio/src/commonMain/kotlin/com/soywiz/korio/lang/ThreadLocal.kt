package com.soywiz.korio.lang

import kotlin.reflect.*

class threadLocal<T>(val gen: () -> T) {
	val local = object : NativeThreadLocal<T>() {
		override fun initialValue(): T = gen()
	}

	inline operator fun getValue(thisRef: Any?, property: KProperty<*>): T = local.get()
	inline operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T): Unit = local.set(value)
}

expect abstract class NativeThreadLocal<T>() {
	abstract fun initialValue(): T
	fun get(): T
	fun set(value: T)
}

