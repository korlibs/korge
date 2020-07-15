package com.soywiz.korio.dynamic

import com.soywiz.korio.util.*
import java.lang.reflect.*

internal actual object DynamicInternal {
	class JavaPackage(val name: String)

	actual val global: Any? = JavaPackage("")

	private fun tryGetField(clazz: Class<*>, name: String): Field? {
		val field = runCatching { clazz.getDeclaredField(name) }.getOrNull()
		return when {
			field != null -> field.apply { isAccessible = true }
			clazz.superclass != null -> return tryGetField(clazz.superclass, name)
			else -> null
		}
	}

	private fun tryGetMethod(clazz: Class<*>, name: String, args: Array<out Any?>?): Method? {
		val methods = clazz.allDeclaredMethods.filter { it.name == name }
		val method = when (methods.size) {
			0 -> null
			1 -> methods.first()
			else -> {
				if (args != null) {
					val methodsSameArity = methods.filter { it.parameterTypes.size == args.size }
					val argTypes = args.map { it!!::class.javaObjectType }
					methodsSameArity.firstOrNull {
						it.parameterTypes.toList().zip(argTypes).all {
							it.first.kotlin.javaObjectType.isAssignableFrom(it.second)
						}
					}
				} else {
					methods.first()
				}
			}
		}
		return when {
			method != null -> method.apply { isAccessible = true }
			clazz.superclass != null -> return tryGetMethod(clazz.superclass, name, args)
			else -> null
		}
	}

	actual fun get(instance: Any?, key: String): Any? {
		if (instance == null) return null

		val static = instance is Class<*>
		val clazz: Class<*> = if (static) instance as Class<*> else instance.javaClass

		if (instance is JavaPackage) {
			val path = "${instance.name}.$key".trim('.')
			return try {
				java.lang.Class.forName(path)
			} catch (e: ClassNotFoundException) {
				JavaPackage(path)
			}
		}
		val method = tryGetMethod(clazz, "get${key.capitalize()}", null)
		if (method != null) {
			return method.invoke(if (static) null else instance)
		}
		val field = tryGetField(clazz, key)
		if (field != null) {
			return field.get(if (static) null else instance)
		}
		return null
	}

	actual fun set(instance: Any?, key: String, value: Any?) {
		if (instance == null) return

		val static = instance is Class<*>
		val clazz: Class<*> = if (static) instance as Class<*> else instance.javaClass

		val method = tryGetMethod(clazz, "set${key.capitalize()}", null)
		if (method != null) {
			method.invoke(if (static) null else instance, value)
			return
		}
		val field = tryGetField(clazz, key)
		if (field != null) {
			field.set(if (static) null else instance, value)
			return
		}
	}

	actual fun invoke(instance: Any?, key: String, args: Array<out Any?>): Any? {
		if (instance == null) return null
		val method = tryGetMethod(if (instance is Class<*>) instance else instance.javaClass, key, args)
		return method?.invoke(if (instance is Class<*>) null else instance, *args)
	}
}
