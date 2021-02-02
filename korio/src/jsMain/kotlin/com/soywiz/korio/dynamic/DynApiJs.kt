package com.soywiz.korio.dynamic

internal actual object DynamicInternal : DynApi {
	override val global: Any? = js("(typeof global !== 'undefined') ? global : window")

    override fun get(instance: Any?, key: String): Any? {
		return (instance.asDynamic())[key]
	}

    override fun set(instance: Any?, key: String, value: Any?) {
		(instance.asDynamic())[key] = value
	}

    override fun invoke(instance: Any?, key: String, args: Array<out Any?>): Any? {
		return (instance.asDynamic())[key].apply(instance, args)
	}
}
