package com.soywiz.korge.gradle

import groovy.lang.*
import java.lang.reflect.*

object GroovyDynamic {
    inline operator fun <T> invoke(callback: GroovyDynamic.() -> T): T = callback(GroovyDynamic)

    operator fun Any?.get(key: String): Any? {
        if (this == null) return null

        if (this is Map<*, *>) {
            return (this as Map<Any?, Any?>)[key]
        }

        if (this is GroovyObject) {
            return this.getProperty(key)
        }

        val field = tryGetField(this.javaClass, key)
        if (field != null) {
            return field.get(this)
        }

        return null
    }

    private fun tryGetField(clazz: Class<*>, name: String): Field? {
        val field = runCatching { clazz.getDeclaredField(name) }.getOrNull()
        return when {
            field != null -> field.apply { isAccessible = true }
            clazz.superclass != null -> return tryGetField(clazz.superclass, name)
            else -> null
        }
    }

    //private fun tryGetMethod(clazz: Class<*>, name: String): Method? {
    //    val field = runCatching { clazz.allDeclaredMethods.firstOrNull { it.name == name } }.getOrNull()
    //    return when {
    //        field != null -> field.apply { isAccessible = true }
    //        clazz.superclass != null -> return tryGetMethod(clazz.superclass, name)
    //        else -> null
    //    }
    //}


}