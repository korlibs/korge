package com.soywiz.korte.dynamic

import kotlin.coroutines.*
import kotlin.reflect.*

open class JsObjectMapper2 : ObjectMapper2() {
    override fun hasProperty(instance: Any, key: String): Boolean {
        val tof = jsTypeOf(instance.asDynamic()[key])
        return tof !== "undefined" && tof !== "function"
    }

    override fun hasMethod(instance: Any, key: String): Boolean {
        return jsTypeOf(instance.asDynamic()[key]) !== "undefined"
    }

    override suspend fun invokeAsync(type: KClass<Any>, instance: Any?, key: String, args: List<Any?>): Any? {
        val function = instance.asDynamic()[key] ?: return super.invokeAsync(type, instance, key, args)
        //val function = instance.asDynamic()[key] ?: return null
        return suspendCoroutine<Any?> { c ->
            val arity: Int = function.length.unsafeCast<Int>()
            val rargs = when {
                args.size != arity -> args + listOf(c)
                else -> args
            }
            try {
                val result = function.apply(instance, rargs.toTypedArray())
                if (result != kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED) {
                    c.resume(result)
                }
            } catch (e: Throwable) {
                c.resumeWithException(e)
            }
        }
    }

    override suspend fun set(instance: Any, key: Any?, value: Any?) {
        instance.asDynamic()[key] = value
    }

    override suspend fun get(instance: Any, key: Any?): Any? {
        return instance.asDynamic()[key]
    }
}

actual val Mapper2: ObjectMapper2 = JsObjectMapper2()
