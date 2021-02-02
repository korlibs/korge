package com.soywiz.korio.dynamic

import com.soywiz.kds.*
import com.soywiz.korio.util.*
import kotlin.math.*

val Any?.dyn: Dyn get() = Dyn(this)

@Suppress("DEPRECATION")
inline class Dyn(val value: Any?) : Comparable<Dyn> {
    val dyn get() = this
    val isNull get() = value == null
    val isNotNull get() = value != null

    @Suppress("UNCHECKED_CAST")
    fun toComparable(): Comparable<Any?> = when (value) {
        null -> 0 as Comparable<Any?>
        is Comparable<*> -> value as Comparable<Any?>
        else -> value.toString() as Comparable<Any?>
    }

    //fun unop(op: String): Dyn = unop(this, op)
    //fun binop(op: String, r: Dyn): Dyn = binop(this, r, op)

    operator fun unaryMinus(): Dyn = (-toDouble()).dyn
    operator fun unaryPlus(): Dyn = this
    fun inv(): Dyn = toInt().inv().dyn
    fun not(): Dyn = (!toBool()).dyn

    operator fun plus(r: Dyn): Dyn {
        val l = this
        val out: Any? = when (l.value) {
            is String -> l.toString() + r.toString()
            is Iterable<*> -> l.toIterableAny() + r.toIterableAny()
            else -> l.toDouble() + r.toDouble()
        }
        return out.dyn
    }
    operator fun minus(r: Dyn): Dyn = (this.toDouble() - r.toDouble()).dyn
    operator fun times(r: Dyn): Dyn = (this.toDouble() * r.toDouble()).dyn
    operator fun div(r: Dyn): Dyn = (this.toDouble() / r.toDouble()).dyn
    operator fun rem(r: Dyn): Dyn = (this.toDouble() % r.toDouble()).dyn
    infix fun pow(r: Dyn): Dyn = (this.toDouble().pow(r.toDouble())).dyn
    infix fun bitAnd(r: Dyn): Dyn = (this.toInt() and r.toInt()).dyn
    infix fun bitOr(r: Dyn): Dyn = (this.toInt() or r.toInt()).dyn
    infix fun bitXor(r: Dyn): Dyn = (this.toInt() xor r.toInt()).dyn
    /** Logical AND */
    infix fun and(r: Dyn): Boolean = (this.toBool() && r.toBool())
    /** Logical OR */
    infix fun or(r: Dyn): Boolean = (this.toBool() || r.toBool())

    /** Equal */
    infix fun eq(r: Dyn): Boolean = when {
        this.value is Number && r.value is Number -> this.toDouble() == r.toDouble()
        this.value is String || r.value is String -> this.toString() == r.toString()
        else -> this.value == r.value
    }
    /** Not Equal */
    infix fun ne(r: Dyn): Boolean = when {
        this.value is Number && r.value is Number -> this.toDouble() != r.toDouble()
        this.value is String || r.value is String -> this.toString() != r.toString()
        else -> this.value != r.value
    }
    /** Strict EQual */
    infix fun seq(r: Dyn): Boolean = this.value === r.value
    /** Strict Not Equal */
    infix fun sne(r: Dyn): Boolean = this.value !== r.value
    /** Less Than */
    infix fun lt(r: Dyn): Boolean = compare(this, r) < 0
    /** Less or Equal */
    infix fun le(r: Dyn): Boolean = compare(this, r) <= 0
    /** Greater Than */
    infix fun gt(r: Dyn): Boolean = compare(this, r) > 0
    /** Greater or Equal */
    infix fun ge(r: Dyn): Boolean = compare(this, r) >= 0
    operator fun contains(r: Dyn): Boolean {
        val collection = this
        val element = r
        if (collection.value == element.value) return true
        return when (collection.value) {
            is String -> collection.value.contains(element.value.toString())
            is Set<*> -> element.value in collection.value
            else -> element.value in collection.toListAny()
        }
    }
    fun coalesce(default: Dyn): Dyn = if (this.isNotNull) this else default

    override fun compareTo(other: Dyn): Int {
        val l = this
        val r = other
        if (l.value is Number && r.value is Number) {
            return l.value.toDouble().compareTo(r.value.toDouble())
        }
        val lc = l.toComparable()
        val rc = r.toComparable()
        return if (lc::class.isInstance(rc)) lc.compareTo(rc) else -1
    }

    override fun toString(): String = value.toString()

    companion object {
        val global get() = dynApi.global.dyn

        fun compare(l: Dyn, r: Dyn): Int = l.compareTo(r)
        fun contains(collection: Dyn, element: Dyn): Boolean = element in collection

        /*
        fun unop(r: Dyn, op: String): Dyn = when (op) {
            "+" -> +r
            "-" -> -r
            "~" -> r.inv()
            "!" -> r.not()
            else -> error("Not implemented unary operator '$op'")
        }

        fun binop(l: Dyn, r: Dyn, op: String): Dyn {
            return when (op) {
                "+" -> (l + r)
                "-" -> (l - r)
                "*" -> (l * r)
                "/" -> (l / r)
                "%" -> (l % r)
                "**" -> (l pow r)
                "&" -> (l bitAnd r)
                "|" -> (l bitOr r)
                "^" -> (l bitXor r)
                "&&" -> (l and r).dyn
                "and" -> (l and r).dyn
                "||" -> (l or r).dyn
                "or" -> (l or r).dyn
                "==" -> (l eq r).dyn
                "!=" -> (l ne r).dyn
                "===" -> (l seq r).dyn
                "!==" -> (l sne r).dyn
                "<" -> (l lt r).dyn
                "<=" -> (l le r).dyn
                ">" -> (l gt r).dyn
                ">=" -> (l ge r).dyn
                "in" -> r.contains(l).dyn
                "contains" -> l.contains(r).dyn
                "?:" -> l.coalesce(r)
                else -> error("Not implemented binary operator '$op'")
            }
        }
         */
    }

    fun toList(): List<Dyn> = toList().map { it.dyn }
    fun toIterable(): Iterable<Dyn> = toIterableAny().map { it.dyn }

    fun toListAny(): List<*> = toIterableAny().toList()

    fun toIterableAny(): Iterable<*> = when (value) {
        null -> listOf<Any?>()
        //is Dynamic2Iterable -> it.dynamic2Iterate()
        is Iterable<*> -> value
        is CharSequence -> value.toList()
        is Map<*, *> -> value.toList()
        else -> listOf<Any?>()
    }

    interface Invokable {
        fun invoke(name: String, args: Array<out Any?>): Any?
    }

    interface SuspendInvokable {
        suspend fun invoke(name: String, args: Array<out Any?>): Any?
    }

    fun dynamicInvoke(name: String, vararg args: Any?): Dyn = when (value) {
        null -> null.dyn
        is Invokable -> value.invoke(name, args).dyn
        else -> dynApi.invoke(value, name, args).dyn
    }

    suspend fun suspendDynamicInvoke(name: String, vararg args: Any?): Dyn = when (value) {
        null -> null.dyn
        is Invokable -> value.invoke(name, args).dyn
        is SuspendInvokable -> value.invoke(name, args).dyn
        else -> dynApi.suspendInvoke(value, name, args).dyn
    }

    operator fun set(key: Dyn, value: Dyn) = set(key.value, value.value)
    operator fun set(key: Any?, value: Dyn) = set(key, value.value)
    operator fun set(key: Any?, value: Any?) {
        when (value) {
            is MutableMap<*, *> -> (this.value as MutableMap<Any?, Any?>)[key] = value
            is MutableList<*> -> (this.value as MutableList<Any?>)[key.dyn.toInt()] = value
            else -> dynApi.set(this.value, key.toString(), value)
        }
    }

    operator fun get(key: Dyn): Dyn = get(key.value)
    operator fun get(key: Any?): Dyn = when (value) {
        null -> null.dyn
        is Map<*, *> -> (value as Map<Any?, Any?>)[key].dyn
        is List<*> -> value[key.dyn.toInt()].dyn
        else -> dynApi.get(value, key.toString()).dyn
    }

    suspend fun suspendSet(key: Dyn, value: Dyn) = suspendSet(key.value, value.value)
    suspend fun suspendSet(key: Any?, value: Dyn) = suspendSet(key, value.value)
    suspend fun suspendSet(key: Any?, value: Any?) {
        when (value) {
            is MutableMap<*, *> -> (this.value as MutableMap<Any?, Any?>)[key] = value
            is MutableList<*> -> (this.value as MutableList<Any?>)[key.dyn.toInt()] = value
            else -> dynApi.suspendSet(this.value, key.toString(), value)
        }
    }

    suspend fun suspendGet(key: Dyn): Dyn = suspendGet(key.value)
    suspend fun suspendGet(key: Any?): Dyn = when (value) {
        null -> null.dyn
        is Map<*, *> -> (value as Map<Any?, Any?>)[key].dyn
        is List<*> -> value[key.dyn.toInt()].dyn
        else -> dynApi.suspendGet(value, key.toString()).dyn
    }

    val mapAny: Map<Any?, Any?> get() = if (value is Map<*, *>) value as Map<Any?, Any?> else LinkedHashMap()
    val listAny: List<Any?> get() = if (value == null) listOf() else if (value is List<*>) value else if (value is Iterable<*>) value.toList() else listOf(value)
    val keysAny: List<Any?> get() = if (value is Map<*, *>) keys.toList() else listOf()

    val map: Map<Dyn, Dyn> get() = mapAny.map { it.key.dyn to it.value.dyn }.toMap()
    val list: List<Dyn> get() = listAny.map { it.dyn }
    val keys: List<Dyn> get() = keysAny.map { it.dyn }

    fun String.toNumber(): Number = (this.toIntOrNull() as? Number?) ?: this.toDoubleOrNull() ?: Double.NaN

    fun toBool(extraStrings: Boolean = true): Boolean = when (value) {
        null -> false
        is Boolean -> value
        else -> toBoolOrNull(extraStrings) ?: true
    }

    fun toBoolOrNull(extraStrings: Boolean = true): Boolean? = when (value) {
        null -> null
        is Boolean -> value
        is Number -> toDouble() != 0.0
        is String -> {
            if (extraStrings) {
                when (value.toLowerCase()) {
                    "", "0", "false", "NaN", "null", "undefined", "ko", "no" -> false
                    else -> true
                }
            } else {
                value.isNotEmpty() && value != "0" && value != "false"
            }
        }
        else -> null
    }

    fun toNumber(): Number = when (value) {
        null -> 0
        is Number -> value
        is Boolean -> if (value) 1 else 0
        is String -> value.toIntSafe() ?: value.toDoubleSafe() ?: 0
        //else -> it.toString().toNumber()
        else -> value.toString().toNumber()
    }

    fun toString(value: Any?): String = when (value) {
        null -> ""
        is String -> value
        is Double -> {
            if (value == value.toInt().toDouble()) {
                value.toInt().toString()
            } else {
                value.toString()
            }
        }
        is Iterable<*> -> "[" + value.joinToString(", ") { toString(it) } + "]"
        is Map<*, *> -> "{" + value.map { toString(it.key).quote() + ": " + toString(it.value) }.joinToString(", ") + "}"
        else -> value.toString()
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
