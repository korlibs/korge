package com.soywiz.korio.dynamic

import com.soywiz.kds.*

@Suppress("DEPRECATION")
inline class Dyn(val value: Any?) {
    val isNull get() = value == null
    val isNotNull get() = value != null

    companion object {
        val global get() = DynamicInternal.global.dyn
    }

    interface Invokable {
        fun invoke(name: String, args: Array<out Any?>): Any?
    }

    fun dynamicInvoke(name: String, vararg args: Any?): Dyn = when (value) {
        null -> null.dyn
        is KDynamic.Invokable -> value.invoke(name, args).dyn
        is Invokable -> value.invoke(name, args).dyn
        else -> DynamicInternal.invoke(value, name, args).dyn
    }

    operator fun set(key: Dyn, value: Dyn) = set(key.value, value.value)
    operator fun set(key: Any?, value: Dyn) = set(key, value.value)
    operator fun set(key: Any?, value: Any?) {
        when (value) {
            is MutableMap<*, *> -> (this.value as MutableMap<Any?, Any?>)[key] = value
            is MutableList<*> -> (this.value as MutableList<Any?>)[key.dyn.toInt()] = value
            else -> DynamicInternal.set(this.value, key.toString(), value)
        }
    }

    operator fun get(key: Dyn): Dyn = get(key.value)
    operator fun get(key: Any?): Dyn = when (value) {
        null -> null.dyn
        is Map<*, *> -> (value as Map<Any?, Any?>)[key].dyn
        is List<*> -> value[key.dyn.toInt()].dyn
        else -> DynamicInternal.get(value, key.toString()).dyn
    }

    val mapAny: Map<Any?, Any?> get() = if (value is Map<*, *>) value as Map<Any?, Any?> else LinkedHashMap()
    val listAny: List<Any?> get() = if (value == null) listOf() else if (value is List<*>) value else if (value is Iterable<*>) value.toList() else listOf(value)
    val keysAny: List<Any?> get() = if (value is Map<*, *>) keys.toList() else listOf()

    val map: Map<Dyn, Dyn> get() = mapAny.map { it.key.dyn to it.value.dyn }.toMap()
    val list: List<Dyn> get() = listAny.map { it.dyn }
    val keys: List<Dyn> get() = keysAny.map { it.dyn }

    fun toNumber(): Number = when (value) {
        null -> 0
        is Boolean -> if (value) 1 else 0
        is Number -> value
        is String -> value.toIntSafe() ?: value.toDoubleSafe() ?: 0
        else -> 0
    }

    fun toBool(): Boolean = when (value) {
        is Boolean -> value
        is String -> when (value.toLowerCase()) {
            "", "0", "false", "NaN", "null", "undefined", "ko", "no" -> false
            else -> true
        }
        else -> toInt() != 0
    }

    fun toByte(): Byte = toNumber().toByte()
    fun toChar(): Char = when {
        value is Char -> value
        value is String && (value.length == 1) -> value.first()
        else -> toNumber().toChar()
    }

    fun toShort(): Short = toNumber().toShort()
    fun toInt(): Int = toNumber().toInt()
    fun toLong(): Long = toNumber().toLong()
    fun toFloat(): Float = toNumber().toFloat()
    fun toDouble(): Double = toNumber().toDouble()

    fun toBoolOrNull(): Boolean? = when (value) {
        is Boolean -> value
        is String -> value == "1" || value == "true" || value == "on"
        is Number -> toInt() != 0
        else -> null
    }

    fun toIntOrNull(): Int? = when (value) {
        is Number -> toInt()
        is String -> value.toIntSafe()
        else -> null
    }

    fun toLongOrNull(): Long? = when (value) {
        is Number -> toLong()
        is String -> value.toLongSafe()
        else -> null
    }

    fun toDoubleOrNull(): Double? = when (value) {
        is Number -> toDouble()
        is String -> value.toDoubleSafe()
        else -> null
    }

    fun toIntDefault(default: Int = 0): Int = when (value) {
        is Number -> toInt()
        is String -> value.toIntSafe(10) ?: default
        else -> default
    }

    fun toLongDefault(default: Long = 0L): Long = when (value) {
        is Number -> toLong()
        is String -> value.toLongSafe(10) ?: default
        else -> default
    }

    fun toFloatDefault(default: Float = 0f): Float = when (value) {
        is Number -> toFloat()
        is String -> toFloat()
        else -> default
    }

    fun toDoubleDefault(default: Double = 0.0): Double = when (value) {
        is Number -> toDouble()
        is String -> toDouble()
        else -> default
    }

    val str: String get() = toString()
    val int: Int get() = toIntDefault()
    val bool: Boolean get() = toBoolOrNull() ?: false
    val float: Float get() = toFloatDefault()
    val double: Double get() = toDoubleDefault()
    val long: Long get() = toLongDefault()

    val intArray: IntArray get() = value as? IntArray ?: (value as? IntArrayList)?.toIntArray() ?: list.map { it.dyn.int }.toIntArray()
    val floatArray: FloatArray get() = value as? FloatArray ?: (value as? FloatArrayList)?.toFloatArray() ?: list.map { it.dyn.float }.toFloatArray()
    val doubleArray: DoubleArray get() = value as? DoubleArray ?: (value as? DoubleArrayList)?.toDoubleArray() ?: list.map { it.dyn.double }.toDoubleArray()
    val longArray: LongArray get() = value as? LongArray ?: list.map { it.dyn.long }.toLongArray()
}

private fun String.toIntSafe(radix: Int = 10) = this.toIntOrNull(radix)
private fun String.toDoubleSafe() = this.toDoubleOrNull()
private fun String.toLongSafe(radix: Int = 10) = this.toLongOrNull(radix)

val Any?.dyn: Dyn get() = Dyn(this)
