package com.soywiz.korio.util

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korio.dynamic.mapper.*
import com.soywiz.korio.lang.*
import java.lang.reflect.*
import java.util.*
import kotlin.reflect.*

// @TODO: This should use ASM library to create a class per class to be as fast as possible
class ClassFactory<T> private constructor(iclazz: Class<out T>, internal: kotlin.Boolean) {
	val clazz = when {
		List::class.java.isAssignableFrom(iclazz) -> ArrayList::class.java
		Map::class.java.isAssignableFrom(iclazz) -> HashMap::class.java
		else -> iclazz
	}

	init {
		//println("$iclazz -> $clazz")
	}

	companion object {
		val cache = LinkedHashMap<Class<*>, ClassFactory<*>>()
		@Suppress("UNCHECKED_CAST")
		operator fun <T> get(clazz: Class<out T>): ClassFactory<T> =
			cache.getOrPut(clazz) { ClassFactory(clazz, true) } as ClassFactory<T>

		fun <T : Any> getForInstance(obj: T): ClassFactory<T> = get(obj.javaClass)

		operator fun <T> invoke(clazz: Class<out T>): ClassFactory<T> = ClassFactory[clazz]

		fun createDummyUnchecked(clazz: Class<*>): Any {
			when (clazz) {
				java.lang.Boolean.TYPE -> return false
				java.lang.Byte.TYPE -> return 0.toByte()
				java.lang.Short.TYPE -> return 0.toShort()
				java.lang.Character.TYPE -> return 0.toChar()
				java.lang.Integer.TYPE -> return 0
				java.lang.Long.TYPE -> return 0L
				java.lang.Float.TYPE -> return 0f
				java.lang.Double.TYPE -> return 0.0
			}
			if (clazz.isArray) return java.lang.reflect.Array.newInstance(clazz.componentType, 0)
			if (clazz.isAssignableFrom(Set::class.java)) return HashSet<Any>()
			if (clazz.isAssignableFrom(List::class.java)) return ArrayList<Any>()
			if (clazz.isAssignableFrom(Map::class.java)) return LinkedHashMap<Any, Any>()
			if (clazz.isEnum) return clazz.enumConstants.first()
			return ClassFactory[clazz].createDummy()
		}
	}

	val constructor = clazz.declaredConstructors.sortedBy { it.parameterTypes.size }.firstOrNull()
		?: invalidOp("Can't find constructor for $clazz")
	val dummyArgs = createDummyArgs(constructor)
	val fields = clazz.declaredFields
		.filter { !Modifier.isTransient(it.modifiers) && !Modifier.isStatic(it.modifiers) }

	init {
		constructor.isAccessible = true
		fields.fastForEach { field ->
			field.isAccessible = true
		}
	}

	fun create(values: Any?): T {
		when (values) {
			is Map<*, *> -> {
				val instance = createDummy()
				fields.fastForEach { field ->
					if (values.containsKey(field.name)) {
						field.isAccessible = true
						field.set(instance, DynamicJvm.dynamicCast(values[field.name], field.type, field.genericType))
					}
				}
				return instance
			}
			else -> {
				return DynamicJvm.dynamicCast(values, clazz as Class<*>) as T
			}
		}
	}

	fun toMap(instance: T): Map<String, Any?> {
		return fields.map { it.name to it.get(instance) }.toMap()
	}

	@Suppress("UNCHECKED_CAST")
	fun createDummy(): T = constructor.newInstance(*dummyArgs) as T

	fun createDummyArgs(constructor: Constructor<*>): Array<Any> {
		return constructor.parameterTypes.map { createDummyUnchecked(it) }.toTypedArray()
	}
}

object JvmTyper {
	fun untype(obj: Any?): Any? = when (obj) {
		null -> obj
		is Boolean, is String, is Number -> obj
		is Iterable<*> -> obj.map { untype(it) }.toMutableList()
		is Map<*, *> -> obj.map { untype(it.key) to untype(it.value) }.toLinkedMap()
		else -> {
			if (obj.javaClass.isArray) {
				val len = java.lang.reflect.Array.getLength(obj)
				(0 until len).map { untype(java.lang.reflect.Array.get(obj, it)) }.toMutableList()
			} else {
				val out = LinkedHashMap<String, Any?>()
				val cf = ClassFactory.getForInstance(obj)
				cf.fields.fastForEach { field ->
					out[field.name] = untype(field.get(obj))
				}
				out
			}
		}
	}
}

fun ObjectMapper.jvmFallback() = this.apply {
	this.fallbackTyper = { clazz, obj ->
		val jclazz = (clazz as KClass<*>).java
		val cf = ClassFactory[jclazz]
		cf.create(obj)
	}
	this.fallbackUntyper = { obj ->
		JvmTyper.untype(obj)!!
	}
}