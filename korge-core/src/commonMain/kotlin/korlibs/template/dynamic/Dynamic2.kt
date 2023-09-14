package korlibs.template.dynamic

import korlibs.template.internal.*
import kotlin.math.*
import kotlin.reflect.*

//expect class DynamicBase {
//	//fun getFields(obj: Any?): List<String>
//	//fun getMethods(obj: Any?): List<String>
//	//fun invoke(obj: Any?, name: String, args: List<Any?>): Any?
//	//fun getFunctionArity(obj: Any?, name: String): Int
//	fun dynamicGet(obj: Any?, name: String): Any?
//	fun dynamicSet(obj: Any?, name: String, value: Any?): Unit
//}

object Dynamic2 {
    fun binop(l: Any?, r: Any?, op: String): Any? = when (op) {
        "+" -> {
            when (l) {
                is String -> l.toString() + toString(r)
                is Iterable<*> -> toIterable(l) + toIterable(r)
                else -> toDouble(l) + toDouble(r)
            }
        }
        "-" -> toDouble(l) - toDouble(r)
        "*" -> toDouble(l) * toDouble(r)
        "/" -> toDouble(l) / toDouble(r)
        "%" -> toDouble(l) % toDouble(r)
        "**" -> toDouble(l).pow(toDouble(r))
        "&" -> toInt(l) and toInt(r)
        "|" -> toInt(l) or toInt(r)
        "^" -> toInt(l) xor toInt(r)
        "&&" -> toBool(l) && toBool(r)
        "||" -> toBool(l) || toBool(r)
        "and" -> toBool(l) && toBool(r)
        "or" -> toBool(l) || toBool(r)
        "==" -> when {
            l is Number && r is Number -> l.toDouble() == r.toDouble()
            l is String || r is String -> l.toString() == r.toString()
            else -> l == r
        }
        "!=" -> when {
            l is Number && r is Number -> l.toDouble() != r.toDouble()
            l is String || r is String -> l.toString() != r.toString()
            else -> l != r
        }
        "===" -> l === r
        "!==" -> l !== r
        "<" -> compare(l, r) < 0
        "<=" -> compare(l, r) <= 0
        ">" -> compare(l, r) > 0
        ">=" -> compare(l, r) >= 0
        "in" -> contains(r, l)
        "contains" -> contains(l, r)
        "?:" -> if (toBool(l)) l else r
        else -> error("Not implemented binary operator '$op'")
    }

    fun unop(r: Any?, op: String): Any? = when (op) {
        "+" -> r
        "-" -> -toDouble(r)
        "~" -> toInt(r).inv()
        "!" -> !toBool(r)
        else -> error("Not implemented unary operator $op")
    }

    fun contains(collection: Any?, element: Any?): Boolean {
        if (collection == element) return true
        return when (collection) {
            is String -> collection.contains(element.toString())
            is Set<*> -> element in collection
            else -> element in toList(collection)
        }
    }

    fun compare(l: Any?, r: Any?): Int {
        if (l is Number && r is Number) {
            return l.toDouble().compareTo(r.toDouble())
        }
        val lc = toComparable(l)
        val rc = toComparable(r)
        if (lc::class.isInstance(rc)) {
            return lc.compareTo(rc)
        } else {
            return -1
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun toComparable(it: Any?): Comparable<Any?> = when (it) {
        null -> 0 as Comparable<Any?>
        is Comparable<*> -> it as Comparable<Any?>
        else -> it.toString() as Comparable<Any?>
    }

    fun toBool(it: Any?): Boolean = when (it) {
        null -> false
        else -> toBoolOrNull(it) ?: true
    }

    fun toBoolOrNull(it: Any?): Boolean? = when (it) {
        null -> null
        is Boolean -> it
        is Number -> it.toDouble() != 0.0
        is String -> it.isNotEmpty() && it != "0" && it != "false"
        else -> null
    }

    fun toNumber(it: Any?): Number = when (it) {
        null -> 0.0
        is Number -> it
        else -> it.toString().toNumber()
    }

    fun String.toNumber(): Number = (this.toIntOrNull() as? Number?) ?: this.toDoubleOrNull() ?: Double.NaN

    fun toInt(it: Any?): Int = toNumber(it).toInt()
    fun toLong(it: Any?): Long = toNumber(it).toLong()
    fun toDouble(it: Any?): Double = toNumber(it).toDouble()

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
        is Iterable<*> -> "[" + value.map { toString(it) }.joinToString(", ") + "]"
        is Map<*, *> -> "{" + value.map { toString(it.key).quote() + ": " + toString(it.value) }.joinToString(", ") + "}"
        else -> value.toString()
    }

    fun length(subject: Any?): Int = when (subject) {
        null -> 0
        is Array<*> -> subject.size
        is List<*> -> subject.size
        is Map<*, *> -> subject.size
        is Iterable<*> -> subject.count()
        else -> subject.toString().length
    }

    fun toList(it: Any?): List<*> = toIterable(it).toList()

    fun toIterable(it: Any?): Iterable<*> = when (it) {
        null -> listOf<Any?>()
        //is Dynamic2Iterable -> it.dynamic2Iterate()
        is Iterable<*> -> it
        is CharSequence -> it.toList()
        is Map<*, *> -> it.toList()
        else -> listOf<Any?>()
    }

    suspend fun accessAny(instance: Any?, key: Any?, mapper: ObjectMapper2): Any? = mapper.accessAny(instance, key)

    suspend fun setAny(instance: Any?, key: Any?, value: Any?, mapper: ObjectMapper2): Unit = when (instance) {
        null -> Unit
        is Dynamic2Settable -> instance.dynamic2Set(key, value)
        is MutableMap<*, *> -> (instance as MutableMap<Any?, Any?>).set(key, value)
        is MutableList<*> -> (instance as MutableList<Any?>)[toInt(key)] = value
        else -> {
            DynamicContext {
                when {
                    mapper.hasProperty(instance, key.toDynamicString()) -> mapper.set(instance, key, value)
                    mapper.hasMethod(instance, key.toDynamicString()) -> {
                        mapper.invokeAsync(
                            instance::class as KClass<Any>,
                            instance as Any?,
                            key.toDynamicString(),
                            listOf(value)
                        )
                        Unit
                    }
                    else -> Unit
                }
            }
        }
    }

    suspend fun callAny(any: Any?, args: List<Any?>, mapper: ObjectMapper2): Any? =
        callAny(any, "invoke", args, mapper = mapper)

    suspend fun callAny(any: Any?, methodName: Any?, args: List<Any?>, mapper: ObjectMapper2): Any? = when (any) {
        null -> null
        (any is Dynamic2Callable) -> (any as Dynamic2Callable).dynamic2Call(methodName, args)
        else -> mapper.invokeAsync(any::class as KClass<Any>, any, DynamicContext { methodName.toDynamicString() }, args)
    }

    //fun dynamicCast(any: Any?, target: KClass<*>): Any? = TODO()
}

interface DynamicContext {
    companion object {
        @PublishedApi internal val Instance = object : DynamicContext { }

        inline operator fun <T> invoke(callback: DynamicContext.() -> T): T = callback(Instance)
    }

    operator fun Number.compareTo(other: Number): Int = this.toDouble().compareTo(other.toDouble())

    fun combineTypes(a: Any?, b: Any?): Any? {
        if (a == null || b == null) return null
        if (a is Number && b is Number) {
            if (a is Double || b is Double) return 0.0
            if (a is Float || b is Float) return 0f
            if (a is Long || b is Long) return 0L
            if (a is Int || b is Int) return 0
            return 0.0
        }
        return a
    }

    fun Any?.toDynamicCastToType(other: Any?) = when (other) {
        is Boolean -> this.toDynamicBool()
        is Int -> this.toDynamicInt()
        is Long -> this.toDynamicLong()
        is Float -> this.toDynamicDouble().toFloat()
        is Double -> this.toDynamicDouble()
        is String -> this.toDynamicString()
        else -> this
    }
    fun Any?.toDynamicString() = Dynamic2.toString(this)
    fun Any?.toDynamicBool() = Dynamic2.toBool(this)
    fun Any?.toDynamicInt() = Dynamic2.toInt(this)
    fun Any?.toDynamicLong() = Dynamic2.toLong(this)
    fun Any?.toDynamicDouble() = Dynamic2.toDouble(this)
    fun Any?.toDynamicNumber() = Dynamic2.toNumber(this)
    fun Any?.toDynamicList() = Dynamic2.toList(this)
    fun Any?.dynamicLength() = Dynamic2.length(this)
    // @TODO: Bug JVM IR 1.5.0-RC: https://youtrack.jetbrains.com/issue/KT-46223
    suspend fun Any?.dynamicGet(key: Any?, mapper: ObjectMapper2): Any? = Dynamic2.accessAny(this, key, mapper)

    // @TODO: Bug JVM IR 1.5.0-RC: https://youtrack.jetbrains.com/issue/KT-46223
    suspend fun Any?.dynamicSet(key: Any?, value: Any?, mapper: ObjectMapper2) =
        Dynamic2.setAny(this, key, value, mapper)

    // @TODO: Bug JVM IR 1.5.0-RC: https://youtrack.jetbrains.com/issue/KT-46223
    suspend fun Any?.dynamicCall(vararg args: Any?, mapper: ObjectMapper2) =
        Dynamic2.callAny(this, args.toList(), mapper = mapper)

    // @TODO: Bug JVM IR 1.5.0-RC: https://youtrack.jetbrains.com/issue/KT-46223
    suspend fun Any?.dynamicCallMethod(methodName: Any?, vararg args: Any?, mapper: ObjectMapper2) =
        Dynamic2.callAny(this, methodName, args.toList(), mapper = mapper)
//suspend internal fun Any?.dynamicCastTo(target: KClass<*>) = Dynamic2.dynamicCast(this, target)

}

interface Dynamic2Gettable {
    suspend fun dynamic2Get(key: Any?): Any?
}

interface Dynamic2Settable {
    suspend fun dynamic2Set(key: Any?, value: Any?)
}

interface Dynamic2Callable {
    suspend fun dynamic2Call(methodName: Any?, params: List<Any?>): Any?
}

//interface Dynamic2Iterable {
//    suspend fun dynamic2Iterate(): Iterable<Any?>
//}
