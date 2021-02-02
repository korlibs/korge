package com.soywiz.korio.dynamic

internal actual object DynamicInternal : DynApi {
	override fun get(instance: Any?, key: String): Any? = throw UnsupportedOperationException("DynamicInternal.get")
	override fun set(instance: Any?, key: String, value: Any?): Unit = throw UnsupportedOperationException("DynamicInternal.set")
	override fun invoke(instance: Any?, key: String, args: Array<out Any?>): Any? = throw UnsupportedOperationException("DynamicInternal.invoke")
}
