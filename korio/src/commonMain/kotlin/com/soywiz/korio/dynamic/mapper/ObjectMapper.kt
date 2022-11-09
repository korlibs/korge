package com.soywiz.korio.dynamic.mapper

import com.soywiz.kds.fastCastTo
import com.soywiz.kds.toLinkedMap
import com.soywiz.korio.dynamic.*
import com.soywiz.korio.lang.invalidArg
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.collections.Iterable
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.Set
import kotlin.collections.iterator
import kotlin.collections.linkedMapOf
import kotlin.collections.map
import kotlin.collections.set
import kotlin.collections.toList
import kotlin.collections.toMap
import kotlin.collections.toMutableList
import kotlin.collections.toSet
import kotlin.reflect.KClass

/**
 * Register classes and how to type and untype them.
 *
 * To Type means to generate typed domain-specific objects
 * While to untype meanas to convert those objects into supported generic primitives:
 * Bools, Numbers, Strings, Lists and Maps (json supported)
 */
class ObjectMapper {
    /** In JvmFallback mode, ignores the properties for serialization */
    @Target(AnnotationTarget.FIELD)
    annotation class DoNotSerialize

	val _typers = linkedMapOf<KClass<*>, TypeContext.(Any?) -> Any?>()
	val _untypers = linkedMapOf<KClass<*>, UntypeContext.(Any?) -> Any?>()
	var fallbackTyper: ((KClass<*>, Any) -> Any)? = null
	var fallbackUntyper: ((Any) -> Any)? = null

	@Suppress("NOTHING_TO_INLINE")
	class TypeContext(val mapper: ObjectMapper) {
		fun <T : Any> Any?.gen(clazz: KClass<T>): T = this@TypeContext.mapper.toTyped(clazz, this)
		fun <T : Any> Any?.genList(clazz: KClass<T>): ArrayList<T> = ArrayList(this.list.map { it.gen(clazz) }.toList())
		fun <T : Any> Any?.genSet(clazz: KClass<T>): HashSet<T> = HashSet(this.list.map { it.gen(clazz) }.toSet())
		fun <K : Any, V : Any> Any?.genMap(kclazz: KClass<K>, vclazz: KClass<V>): MutableMap<K, V> = this.map.map { it.key.gen(kclazz) to it.value.gen(vclazz) }.toLinkedMap()
		inline fun <reified T : Any> Any?.gen(): T = this@TypeContext.mapper.toTyped(T::class, this)
		inline fun <reified T : Any> Any?.genList(): ArrayList<T> = ArrayList(this.list.map { it.gen<T>() }.toList())
		inline fun <reified T : Any> Any?.genSet(): HashSet<T> = HashSet(this.list.map { it.gen<T>() }.toSet())
		inline fun <reified K : Any, reified V : Any> Any?.genMap(): MutableMap<K, V> = this.map.map { it.key.gen<K>() to it.value.gen<V>() }.toLinkedMap()

        val global get() = Dyn.global.value

        interface Invokable {
            fun invoke(name: String, args: Array<out Any?>): Any?
        }

        fun Any?.dynamicInvoke(name: String, vararg args: Any?): Any? = this.dyn.dynamicInvoke(name, *args).value
        operator fun Any?.set(key: Any?, value: Any?) = this.dyn.set(key, value)
        operator fun Any?.get(key: Any?): Any? = this.dyn.get(key).value
        val Any?.map: Map<Any?, Any?> get() = this.dyn.mapAny
        val Any?.list: List<Any?> get() = this.dyn.listAny
        val Any?.keys: List<Any?> get() = this.dyn.keysAny
        fun Any?.toNumber(): Number = this.dyn.toNumber()
        fun Any?.toBool(): Boolean = this.dyn.toBool()
        fun Any?.toByte(): Byte = this.dyn.toByte()
        fun Any?.toChar(): Char = this.dyn.toChar()
        fun Any?.toShort(): Short = this.dyn.toShort()
        fun Any?.toInt(): Int = this.dyn.toInt()
        fun Any?.toLong(): Long = this.dyn.toLong()
        fun Any?.toFloat(): Float = this.dyn.toFloat()
        fun Any?.toDouble(): Double = this.dyn.toDouble()
        fun Any?.toBoolOrNull(): Boolean? = this.dyn.toBoolOrNull()
        fun Any?.toIntOrNull(): Int? = this.dyn.toIntOrNull()
        fun Any?.toLongOrNull(): Long? = this.dyn.toLongOrNull()
        fun Any?.toDoubleOrNull(): Double? = this.dyn.toDoubleOrNull()
        fun Any?.toIntDefault(default: Int = 0): Int = this.dyn.toIntDefault(default)
        fun Any?.toLongDefault(default: Long = 0L): Long = this.dyn.toLongDefault(default)
        fun Any?.toFloatDefault(default: Float = 0f): Float = this.dyn.toFloatDefault(default)
        fun Any?.toDoubleDefault(default: Double = 0.0): Double = this.dyn.toDoubleDefault(default)

        val Any?.str: String get() = this.dyn.str
        val Any?.int: Int get() = this.dyn.int
        val Any?.bool: Boolean get() = this.dyn.bool
        val Any?.float: Float get() = this.dyn.float
        val Any?.double: Double get() = this.dyn.double
        val Any?.long: Long get() = this.dyn.long

        val Any?.intArray: IntArray get() = this.dyn.intArray
        val Any?.floatArray: FloatArray get() = this.dyn.floatArray
        val Any?.doubleArray: DoubleArray get() = this.dyn.doubleArray
        val Any?.longArray: LongArray get() = this.dyn.longArray
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
