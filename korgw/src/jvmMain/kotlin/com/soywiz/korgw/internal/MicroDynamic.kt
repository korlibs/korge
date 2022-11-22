package com.soywiz.korgw.internal

import java.lang.reflect.Modifier

// @TODO: Use DynamicJvm from KorIO once updated to >= 1.9.4
internal object MicroDynamic {
    inline fun invokeCatching(block: MicroDynamic.() -> Unit): Unit {
        try {
            block()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
    inline operator fun <T> invoke(block: MicroDynamic.() -> T): T = block(this)
    fun getClass(fqname: String) = try { Class.forName(fqname) } catch (e: Throwable) { null }

    operator fun Any?.invoke(methodName: String, vararg args: Any?): Any? {
        if (this == null) return null
        val clazz = if (this is Class<*>) this else this::class.java
        val method = clazz.methods
            .firstOrNull {
                it.name == methodName
                    && it.parameterTypes.size == args.size
                    && it.parameterTypes.withIndex().all {
                        val type = args[it.index]?.let { it::class.javaObjectType }
                        it.value.kotlin.javaObjectType.isAssignableFrom(type)
                    }
            }
            ?: return null
        method.isAccessible = true
        //println("CALLING METHOD $method")
        return when {
            Modifier.isStatic(method.modifiers) -> method.invoke(null, *args)
            else -> method.invoke(this, *args)
        }
    }
}
