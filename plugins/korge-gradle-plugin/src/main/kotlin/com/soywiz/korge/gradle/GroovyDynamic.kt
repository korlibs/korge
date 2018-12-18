package com.soywiz.korge.gradle

import groovy.lang.*
import java.lang.reflect.*

internal operator fun Any?.get(key: String): Any? {
    if (this == null) return null

    if (this is Map<*, *>) {
        return (this as Map<Any?, Any?>)[key]
    }

    if (this is GroovyObject) {
        return this.getProperty(key)
    }

    val field = tryGetField(this.javaClass, key)
    if (field != null) return field.get(this)

    val method = tryGetEmptyMethod(this.javaClass, "get" + key.capitalize())
    if (method != null) return method.invoke(this)

    //if (this is Iterable<*>) {
    //    return this.map { it[key] }
    //}

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

private fun tryGetMethod(clazz: Class<*>, name: String): Method? {
    val field = runCatching { clazz.declaredMethods.firstOrNull { it.name == name } }.getOrNull()
    return when {
        field != null -> field.apply { isAccessible = true }
        clazz.superclass != null -> return tryGetMethod(clazz.superclass, name)
        else -> null
    }
}

private fun tryGetEmptyMethod(clazz: Class<*>, name: String): Method? {
    val field = runCatching { clazz.getDeclaredMethod(name) }.getOrNull()
    return when {
        field != null -> field.apply { isAccessible = true }
        clazz.superclass != null -> return tryGetMethod(clazz.superclass, name)
        else -> null
    }
}

fun <T> GroovyClosure(owner: Any?, callback: () -> T): Closure<T> {
    return object : Closure<T>(owner) {
        override fun call(): T {
            return callback()
        }
    }
}
