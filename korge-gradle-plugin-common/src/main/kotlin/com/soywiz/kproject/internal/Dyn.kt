package com.soywiz.kproject.internal

import groovy.lang.*
import java.lang.reflect.*
import kotlin.math.*

val Any?.dyn: Dyn get() = if (this is Dyn) this else Dyn(this)
val Dyn.dyn: Dyn get() = this

fun <R> Dyn.orNull(block: (Dyn) -> R): R? {
    return if (this.isNotNull) block(this) else null
}

@Suppress("DEPRECATION")
inline class Dyn(val value: Any?) : Comparable<Dyn> {
    val dyn get() = this
    val isNull get() = value == null
    val isNotNull get() = value != null

    inline fun <T> casted(): T = value as T

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
    operator fun contains(r: String): Boolean = contains(r.dyn)
    operator fun contains(r: Number): Boolean = contains(r.dyn)
    operator fun contains(r: Dyn): Boolean {
        val collection = this
        val element = r
        if (collection.value == element.value) return true
        return when (collection.value) {
            is String -> collection.value.contains(element.value.toString())
            is Set<*> -> element.value in collection.value
            is Map<*, *> -> element.value in collection.value
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

    override fun toString(): String = toString(value)

    fun toStringOrNull(): String? {
        return if (this.isNotNull) {
            toString()
        } else {
            null
        }
    }

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

    fun toList(): List<Dyn> = toListAny().map { it.dyn }
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
        fun invokeOrThrow(name: String, args: Array<out Any?>): Any? = invoke(name, args)
    }

    interface SuspendInvokable {
        suspend fun invoke(name: String, args: Array<out Any?>): Any?
    }

    fun dynamicInvoke(name: String, vararg args: Any?): Dyn = when (value) {
        null -> null.dyn
        is Invokable -> value.invoke(name, args).dyn
        else -> dynApi.invoke(value, name, args).dyn
    }

    fun dynamicInvokeOrThrow(name: String, vararg args: Any?): Dyn = when (value) {
        null -> error("Can't invoke '$name' on null")
        is Invokable -> value.invokeOrThrow(name, args).dyn
        else -> dynApi.invokeOrThrow(value, name, args).dyn
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
        when (this.value) {
            is MutableMap<*, *> -> (this.value as MutableMap<Any?, Any?>)[key] = value
            is MutableList<*> -> (this.value as MutableList<Any?>)[key.dyn.toInt()] = value
            else -> dynApi.set(this.value, key.toString(), value)
        }
    }

    operator fun get(key: Dyn): Dyn = get(key.value)
    operator fun get(key: Any?): Dyn = _getOrThrow(key, doThrow = false)

    fun getOrNull(key: Any?): Dyn? = _getOrThrow(key, doThrow = false).orNull
    fun getOrThrow(key: Any?): Dyn = _getOrThrow(key, doThrow = true)

    private fun _getOrThrow(key: Any?, doThrow: Boolean): Dyn = when (value) {
        null -> if (doThrow) throw NullPointerException("Trying to access '$key'") else null.dyn
        is Map<*, *> -> (value as Map<Any?, Any?>)[key].dyn
        is GroovyObject -> value.getProperty(key.toString()).dyn
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

    val orNull: Dyn? get() = value?.dyn
    val mapAny: Map<Any?, Any?> get() = if (value is Map<*, *>) value as Map<Any?, Any?> else LinkedHashMap()
    val listAny: List<Any?> get() = if (value == null) listOf() else if (value is List<*>) value else if (value is Iterable<*>) value.toList() else listOf(value)
    val keysAny: List<Any?> get() = if (value is Map<*, *>) value.keys.toList() else listOf()

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

    private fun toString(value: Any?): String = when (value) {
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

    val intArray: IntArray get() = value as? IntArray ?: (value as? List<Int>)?.toIntArray() ?: list.map { it.dyn.int }.toIntArray()
    val floatArray: FloatArray get() = value as? FloatArray ?: (value as? List<Float>)?.toFloatArray() ?: list.map { it.dyn.float }.toFloatArray()
    val doubleArray: DoubleArray get() = value as? DoubleArray ?: (value as? List<Double>)?.toDoubleArray() ?: list.map { it.dyn.double }.toDoubleArray()
    val longArray: LongArray get() = value as? LongArray ?: list.map { it.dyn.long }.toLongArray()
}

private fun String.toIntSafe(radix: Int = 10) = this.toIntOrNull(radix)
private fun String.toDoubleSafe() = this.toDoubleOrNull()
private fun String.toLongSafe(radix: Int = 10) = this.toLongOrNull(radix)

internal object dynApi {
    class JavaPackage(val name: String)

    val global: Any? = JavaPackage("")

    private fun tryGetField(clazz: Class<*>, name: String): Field? {
        val field = runCatching { clazz.getDeclaredField(name) }.getOrNull()
        return when {
            field != null -> field.apply { isAccessible = true }
            clazz.superclass != null -> return tryGetField(clazz.superclass, name)
            else -> null
        }
    }

    private fun tryGetMethod(clazz: Class<*>, name: String, args: Array<out Any?>?): Method? {
        val methods = (clazz.interfaces + clazz).flatMap { it.allDeclaredMethods.filter { it.name == name } }
        val method = when (methods.size) {
            0 -> null
            1 -> methods.first()
            else -> {
                if (args != null) {
                    val methodsSameArity = methods.filter { it.parameterTypes.size == args.size }
                    val argTypes = args.map { if (it == null) null else it::class.javaObjectType }
                    methodsSameArity.firstOrNull {
                        it.parameterTypes.toList().zip(argTypes).all {
                            (it.second == null) || it.first.kotlin.javaObjectType.isAssignableFrom(it.second)
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

    fun get(instance: Any?, key: String): Any? = getBase(instance, key, doThrow = false)

    fun set(instance: Any?, key: String, value: Any?) {
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

    fun getOrThrow(instance: Any?, key: String): Any? {
        return getBase(instance, key, doThrow = true)
    }

    fun invoke(instance: Any?, key: String, args: Array<out Any?>): Any? {
        return invokeBase(instance, key, args, doThrow = false)
    }

    fun invokeOrThrow(instance: Any?, key: String, args: Array<out Any?>): Any? {
        return invokeBase(instance, key, args, doThrow = true)
    }

    suspend fun suspendGet(instance: Any?, key: String): Any? = get(instance, key)
    suspend fun suspendSet(instance: Any?, key: String, value: Any?): Unit = set(instance, key, value)
    suspend fun suspendInvoke(instance: Any?, key: String, args: Array<out Any?>): Any? = invoke(instance, key, args)

    fun getBase(instance: Any?, key: String, doThrow: Boolean): Any? {
        if (instance == null) {
            if (doThrow) error("Can't get '$key' on null")
            return null
        }

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
        if (doThrow) {
            error("Can't find suitable fields or getters for '$key'")
        }
        return null
    }

    fun invokeBase(instance: Any?, key: String, args: Array<out Any?>, doThrow: Boolean): Any? {
        if (instance == null) {
            if (doThrow) error("Can't invoke '$key' on null")
            return null
        }
        val method = tryGetMethod(if (instance is Class<*>) instance else instance.javaClass, key, args)
        if (method == null) {
            if (doThrow) error("Can't find method '$key' on ${instance::class}")
            return null
        }
        return try {
            method.invoke(if (instance is Class<*>) null else instance, *args)
        } catch (e: InvocationTargetException) {
            throw e.targetException ?: e
        }
    }
}

private val Class<*>.allDeclaredFields: List<Field>
    get() = this.declaredFields.toList() + (this.superclass?.allDeclaredFields?.toList() ?: listOf<Field>())

private fun Class<*>.isSubtypeOf(that: Class<*>) = that.isAssignableFrom(this)

private val Class<*>.allDeclaredMethods: List<Method>
    get() = this.declaredMethods.toList() + (this.superclass?.allDeclaredMethods?.toList() ?: listOf<Method>())
