package com.soywiz.korge.dynamic

internal expect object DynamicInternal {
	val global: Any?
	fun get(instance: Any?, key: String): Any?
	fun invoke(instance: Any?, key: String, args: Array<out Any?>): Any?
}

object KgDynamic {
	inline operator fun <T> invoke(callback: KgDynamic.() -> T): T = callback(KgDynamic)
	val global get() = DynamicInternal.global
	operator fun Any?.get(key: String): Any? = DynamicInternal.get(this, key)
	fun Any?.dynamicInvoke(name: String, vararg args: Any?): Any? = DynamicInternal.invoke(this, name, args)
}
