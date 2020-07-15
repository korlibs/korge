package com.soywiz.korte.dynamic

import kotlin.reflect.*

@Suppress("UNCHECKED_CAST")
open class ObjectMapper2 {
    val DynamicType<*>.dynamicShape: DynamicShape<Any?> get() = this.run { DynamicTypeScope.run { this.__dynamicShape as DynamicShape<Any?> } }

    open fun hasProperty(instance: Any, key: String): Boolean {
        if (instance is DynamicType<*>) return instance.dynamicShape.hasProp(key)
        return false
    }
    open fun hasMethod(instance: Any, key: String): Boolean {
        if (instance is DynamicType<*>) return instance.dynamicShape.hasMethod(key)
        return false
    }
    open suspend fun invokeAsync(type: KClass<Any>, instance: Any?, key: String, args: List<Any?>): Any? {
        if (instance is DynamicType<*>) return instance.dynamicShape.callMethod(instance, key, args)
        return null
    }
    open suspend fun set(instance: Any, key: Any?, value: Any?) {
        if (instance is DynamicType<*>) return instance.dynamicShape.setProp(instance, key, value)
    }
    open suspend fun get(instance: Any, key: Any?): Any? {
        if (instance is DynamicType<*>) return instance.dynamicShape.getProp(instance, key)
        return null
    }
}

expect val Mapper2: ObjectMapper2
