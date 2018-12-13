package com.soywiz.korge.dynamic

internal actual object DynamicInternal {
	actual val global: Any? = null

	actual fun get(instance: Any?, key: String): Any? {
		TODO()
	}
	actual fun invoke(instance: Any?, key: String, args: Array<out Any?>): Any? {
		TODO()
	}
}
