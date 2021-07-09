package com.soywiz.korio.dynamic.mapper

import com.soywiz.kds.*
import com.soywiz.korio.dynamic.*
import com.soywiz.korio.lang.*
import kotlin.reflect.*

/**
 * Register classes and how to type and untype them.
 *
 * To Type means to generate typed domain-specific objects
 * While to untype meanas to convert those objects into supported generic primitives:
 * Bools, Numbers, Strings, Lists and Maps (json supported)
 */
class ObjectMapper {
	val _typers = linkedMapOf<KClass<*>, TypeContext.(Any?) -> Any?>()
	val _untypers = linkedMapOf<KClass<*>, UntypeContext.(Any?) -> Any?>()
	var fallbackTyper: ((KClass<*>, Any) -> Any)? = null
	var fallbackUntyper: ((Any) -> Any)? = null

	@Suppress("NOTHING_TO_INLINE")
	class TypeContext(val mapper: ObjectMapper) : KDynamic() {
		fun <T : Any> Any?.gen(clazz: KClass<T>): T = this@TypeContext.mapper.toTyped(clazz, this)
		fun <T : Any> Any?.genList(clazz: KClass<T>): ArrayList<T> = ArrayList(this.list.map { it.gen(clazz) }.toList())
		fun <T : Any> Any?.genSet(clazz: KClass<T>): HashSet<T> = HashSet(this.list.map { it.gen(clazz) }.toSet())
		fun <K : Any, V : Any> Any?.genMap(kclazz: KClass<K>, vclazz: KClass<V>): MutableMap<K, V> = this.map.map { it.key.gen(kclazz) to it.value.gen(vclazz) }.toLinkedMap()
		inline fun <reified T : Any> Any?.gen(): T = this@TypeContext.mapper.toTyped(T::class, this)
		inline fun <reified T : Any> Any?.genList(): ArrayList<T> = ArrayList(this.list.map { it.gen<T>() }.toList())
		inline fun <reified T : Any> Any?.genSet(): HashSet<T> = HashSet(this.list.map { it.gen<T>() }.toSet())
		inline fun <reified K : Any, reified V : Any> Any?.genMap(): MutableMap<K, V> = this.map.map { it.key.gen<K>() to it.value.gen<V>() }.toLinkedMap()
	}

	@Suppress("NOTHING_TO_INLINE")
	class UntypeContext(val map: ObjectMapper) {
		inline fun <reified T : Any> T.gen(): Any? = map.toUntyped(this)
		inline fun Boolean.gen(): Any? = this
		inline fun String.gen(): Any? = this
		inline fun Number.gen(): Any? = this
		inline fun <reified T : Any> Iterable<T>.gen(): List<Any?> = this.map { it.gen() }
		inline fun <reified K : Any, reified V : Any> Map<K, V>.gen(): Map<Any?, Any?> =
			this.map { it.key.gen() to it.value.gen() }.toMap()
	}

	private val typeCtx = TypeContext(this)
	private val untypeCtx = UntypeContext(this)

	fun <T : Any> registerType(clazz: KClass<T>, generate: TypeContext.(Any?) -> T) = this.apply {
		//if (clazz == null) error("Clazz is null!")
		_typers[clazz] = generate
	}

	fun <T : Any> toTyped(clazz: KClass<T>, obj: Any?): T {
		val typer = _typers[clazz]
		return when {
			typer != null -> typer(typeCtx, obj).fastCastTo<T>()
			fallbackTyper != null && obj != null -> fallbackTyper!!(clazz, obj).fastCastTo<T>()
			else -> invalidArg("Unregistered $clazz")
		}
	}

	init {
		registerType(Boolean::class) { it.toBool() }
		registerType(Byte::class) { it.toByte() }
		registerType(Char::class) { it.toChar() }
		registerType(Short::class) { it.toShort() }
		registerType(Int::class) { it.toInt() }
		registerType(Long::class) { it.toLong() }
		registerType(Float::class) { it.toFloat() }
		registerType(Double::class) { it.toDouble() }
		//registerType(Number::class) { it.toNumber() } // @TODO: This produces an undefined error in kotlin-js
		registerType(Set::class) { it.list.toSet() }
		registerType(List::class) { it.list }
		registerType(MutableList::class) { it.list.toMutableList() }
		registerType(String::class) { it?.toString() ?: "null" }
	}

	inline fun <reified T : Any> toUntyped(obj: T?): Any? = toUntyped(T::class, obj)

	fun <T : Any> getKey(clazz: KClass<T>, obj: T?, key: String): Any? {
		return (toUntyped(clazz, obj) as Map<String, Any?>)[key]
	}

	fun <T : Any> toUntyped(clazz: KClass<T>, obj: T?): Any? = when (obj) {
		null -> obj
		is Boolean -> obj
		is Number -> obj
		is String -> obj
		is Iterable<*> -> ArrayList(obj.map { toUntyped(it!!) })
		is Map<*, *> -> obj.map { toUntyped(it.key!!) to toUntyped(it.value) }.toLinkedMap()
		else -> {
			val unt = _untypers[clazz]
			if ((unt == null) && (fallbackUntyper != null)) {
				fallbackUntyper?.invoke(obj)
			} else if ((unt == null)) {
				println("Untypers: " + _untypers.size)
				for (u in _untypers) {
					println(" - " + u.key)
				}

				invalidArg("Don't know how to untype $clazz")
			} else {
				unt.invoke(untypeCtx, obj)
			}
		}
	}

	fun <T : Enum<T>> registerEnum(clazz: KClass<T>, values: Array<T>) {
		val nameToString = values.map { it.name to it }.toMap()
		registerType(clazz) { nameToString[it.toString()]!! }
	}

	inline fun <reified T : Any> registerType(noinline generate: TypeContext.(Any?) -> T) =
		registerType(T::class, generate)

	inline fun <reified T : Enum<T>> registerEnum(values: Array<T>) = registerEnum(T::class, values)
	fun <T : Any> registerUntype(clazz: KClass<T>, untyper: UntypeContext.(T) -> Any?) {
		_untypers[clazz] = untyper as UntypeContext.(Any?) -> Any?
	}

	fun <T : Enum<T>> registerUntypeEnum(clazz: KClass<T>) = registerUntype(clazz) { it.name }
	inline fun <reified T : Any> registerUntype(noinline untyper: UntypeContext.(T) -> Any?) =
		registerUntype(T::class, untyper)

	inline fun <reified T : Enum<T>> registerUntypeEnum() = registerUntype(T::class) { it.name }

	//inline fun <reified T> registerUntypeObj(vararg props: KPro<T>) = registerUntype(T::class, untyper)

	inline fun <T> scoped(callback: () -> T): T {
		val oldTypers = _typers.toMap()
		val oldUntypers = _untypers.toMap()
		try {
			return callback()
		} finally {
			_typers.clear()
			_typers.putAll(oldTypers)
			_untypers.clear()
			_untypers.putAll(oldUntypers)
		}
	}
}

val Mapper = ObjectMapper()
