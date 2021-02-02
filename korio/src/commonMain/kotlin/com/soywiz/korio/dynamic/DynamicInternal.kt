package com.soywiz.korio.dynamic

internal expect object DynamicInternal {
    val global: Any?
    fun get(instance: Any?, key: String): Any?
    fun set(instance: Any?, key: String, value: Any?)
    fun invoke(instance: Any?, key: String, args: Array<out Any?>): Any?
}
