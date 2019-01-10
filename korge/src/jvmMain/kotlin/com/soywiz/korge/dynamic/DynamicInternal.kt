package com.soywiz.korge.dynamic

import com.soywiz.korio.util.*
import java.lang.reflect.*

internal actual object DynamicInternal {
	actual val global: Any? = Package.getPackage(null)

	private fun tryGetField(clazz: Class<*>, name: String): Field? {
		val field = runCatching { clazz.getDeclaredField(name) }.getOrNull()
		return when {
			field != null -> field.apply { isAccessible = true }
			clazz.superclass != null -> return tryGetField(clazz.superclass, name)
			else -> null
		}
	}

	private fun tryGetMethod(clazz: Class<*>, name: String): Method? {
		val field = runCatching { clazz.allDeclaredMethods.firstOrNull { it.name == name } }.getOrNull()
		return when {
			field != null -> field.apply { isAccessible = true }
			clazz.superclass != null -> return tryGetMethod(clazz.superclass, name)
			else -> null
		}
	}

	actual fun get(instance: Any?, key: String): Any? {
		if (instance == null) return null
		val field = tryGetField(instance.javaClass, key)
		return field?.get(instance)
	}

	actual fun invoke(instance: Any?, key: String, args: Array<out Any?>): Any? {
		if (instance == null) return null
		val method = tryGetMethod(instance.javaClass, key)
		return method?.invoke(instance, *args)
	}
}
