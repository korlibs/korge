package com.soywiz.korio.lang

actual abstract class NativeThreadLocal<T> {
	actual abstract fun initialValue(): T
	private var value = initialValue()
	actual fun get(): T = value
	actual fun set(value: T) = run { this.value = value }
}
