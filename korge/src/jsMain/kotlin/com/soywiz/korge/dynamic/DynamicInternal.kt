package com.soywiz.korge.dynamic

internal actual object DynamicInternal {
	actual val global: Any? = js("(typeof global !== 'undefined') ? global : window")

	actual fun get(instance: Any?, key: String): Any? {
		return (instance.asDynamic())[key]
	}

	actual fun invoke(instance: Any?, key: String, args: Array<out Any?>): Any? {
		return (instance.asDynamic())[key].apply(instance, args)
	}
}
