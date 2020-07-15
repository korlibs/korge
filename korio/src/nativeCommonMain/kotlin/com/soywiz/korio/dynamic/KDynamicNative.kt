package com.soywiz.korio.dynamic

import com.soywiz.korio.lang.*

internal actual object DynamicInternal {
	actual val global: Any? = null
	actual fun get(instance: Any?, key: String): Any? = unsupported("DynamicInternal.get")
	actual fun set(instance: Any?, key: String, value: Any?): Unit = unsupported("DynamicInternal.set")
	actual fun invoke(instance: Any?, key: String, args: Array<out Any?>): Any? = unsupported("DynamicInternal.invoke")
}
