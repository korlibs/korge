@file:Suppress("PackageDirectoryMismatch")

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

object KorteDynamic2 {
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

    suspend fun accessAny(instance: Any?, key: Any?, mapper: KorteObjectMapper2): Any? = mapper.accessAny(instance, key)

    suspend fun setAny(instance: Any?, key: Any?, value: Any?, mapper: KorteObjectMapper2): Unit {
        when (instance) {
            null -> Unit
            is KorteDynamic2Settable -> {
                instance.dynamic2Set(key, value)
                Unit
            }
            is MutableMap<*, *> -> {
                (instance as MutableMap<Any?, Any?>).set(key, value)
                Unit
            }
            is MutableList<*> -> {
                (instance as MutableList<Any?>)[toInt(key)] = value
                Unit
            }
            else -> {
                KorteDynamicContext {
                    when {
                        mapper.hasProperty(instance, key.toDynamicString()) -> {
                            mapper.set(instance, key, value)
                            Unit
                        }
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
                Unit
            }
        }
    }

    suspend fun callAny(any: Any?, args: List<Any?>, mapper: KorteObjectMapper2): Any? =
        callAny(any, "invoke", args, mapper = mapper)

    suspend fun callAny(any: Any?, methodName: Any?, args: List<Any?>, mapper: KorteObjectMapper2): Any? = when (any) {
        null -> null
        (any is KorteDynamic2Callable) -> (any as KorteDynamic2Callable).dynamic2Call(methodName, args)
        else -> mapper.invokeAsync(any::class as KClass<Any>, any, KorteDynamicContext { methodName.toDynamicString() }, args)
    }

    //fun dynamicCast(any: Any?, target: KClass<*>): Any? = TODO()
}

interface KorteDynamicContext {
    companion object {
        @PublishedApi internal val Instance = object : KorteDynamicContext { }

        inline operator fun <T> invoke(callback: KorteDynamicContext.() -> T): T = callback(Instance)
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
    fun Any?.toDynamicString() = KorteDynamic2.toString(this)
    fun Any?.toDynamicBool() = KorteDynamic2.toBool(this)
    fun Any?.toDynamicInt() = KorteDynamic2.toInt(this)
    fun Any?.toDynamicLong() = KorteDynamic2.toLong(this)
    fun Any?.toDynamicDouble() = KorteDynamic2.toDouble(this)
    fun Any?.toDynamicNumber() = KorteDynamic2.toNumber(this)
    fun Any?.toDynamicList() = KorteDynamic2.toList(this)
    fun Any?.dynamicLength() = KorteDynamic2.length(this)
    // @TODO: Bug JVM IR 1.5.0-RC: https://youtrack.jetbrains.com/issue/KT-46223
    suspend fun Any?.dynamicGet(key: Any?, mapper: KorteObjectMapper2): Any? = KorteDynamic2.accessAny(this, key, mapper)

    // @TODO: Bug JVM IR 1.5.0-RC: https://youtrack.jetbrains.com/issue/KT-46223
    suspend fun Any?.dynamicSet(key: Any?, value: Any?, mapper: KorteObjectMapper2) =
        KorteDynamic2.setAny(this, key, value, mapper)

    // @TODO: Bug JVM IR 1.5.0-RC: https://youtrack.jetbrains.com/issue/KT-46223
    suspend fun Any?.dynamicCall(vararg args: Any?, mapper: KorteObjectMapper2) =
        KorteDynamic2.callAny(this, args.toList(), mapper = mapper)

    // @TODO: Bug JVM IR 1.5.0-RC: https://youtrack.jetbrains.com/issue/KT-46223
    suspend fun Any?.dynamicCallMethod(methodName: Any?, vararg args: Any?, mapper: KorteObjectMapper2) =
        KorteDynamic2.callAny(this, methodName, args.toList(), mapper = mapper)
//suspend internal fun Any?.dynamicCastTo(target: KClass<*>) = Dynamic2.dynamicCast(this, target)

}

interface KorteDynamic2Gettable {
    suspend fun dynamic2Get(key: Any?): Any?
}

interface KorteDynamic2Settable {
    suspend fun dynamic2Set(key: Any?, value: Any?)
}

interface KorteDynamic2Callable {
    suspend fun dynamic2Call(methodName: Any?, params: List<Any?>): Any?
}

//interface Dynamic2Iterable {
//    suspend fun dynamic2Iterate(): Iterable<Any?>
//}

interface KorteDynamicShapeRegister<T> {
    fun register(prop: KProperty<*>): KorteDynamicShapeRegister<T>
    fun register(callable: KCallable<*>): KorteDynamicShapeRegister<T>
    fun register(name: String, callback: suspend T.(args: List<Any?>) -> Any?): KorteDynamicShapeRegister<T>
    fun register(vararg items: KProperty<*>) = this.apply { for (item in items) register(item) }
    fun register(vararg items: KCallable<*>, dummy: Unit = Unit) = this.apply { for (item in items) register(item) }
}

class KorteDynamicShape<T> : KorteDynamicShapeRegister<T> {
    private val propertiesByName = LinkedHashMap<String, KProperty<*>>()
    private val methodsByName = LinkedHashMap<String, KCallable<*>>()
    private val smethodsByName = LinkedHashMap<String, suspend T.(args: List<Any?>) -> Any?>()

    override fun register(prop: KProperty<*>) = this.apply { propertiesByName[prop.name] = prop }
    override fun register(name: String, callback: suspend T.(args: List<Any?>) -> Any?): KorteDynamicShapeRegister<T> = this.apply { smethodsByName[name] = callback }
    override fun register(callable: KCallable<*>) = this.apply { methodsByName[callable.name] = callable }

    fun hasProp(key: String): Boolean = key in propertiesByName
    fun hasMethod(key: String): Boolean = key in methodsByName || key in smethodsByName
    fun getProp(instance: T, key: Any?): Any? = (propertiesByName[key] as? KProperty1<Any?, Any?>?)?.get(instance)
    fun setProp(instance: T, key: Any?, value: Any?) { (propertiesByName[key] as? KMutableProperty1<Any?, Any?>?)?.set(instance, value) }

    @Suppress("RedundantSuspendModifier")
    suspend fun callMethod(instance: T, key: Any?, args: List<Any?>): Any? {
        val smethod = smethodsByName[key]
        if (smethod != null) {
            return smethod(instance, args)
        }

        val method = methodsByName[key]
        if (method != null) {
            //println("METHOD: ${method.name} : $method : ${method::class}")
            return when (method) {
                is KFunction0<*> -> method.invoke()
                is KFunction1<*, *> -> (method as KFunction1<T, Any?>).invoke(instance)
                is KFunction2<*, *, *> -> (method as KFunction2<T, Any?, Any?>).invoke(instance, args[0])
                is KFunction3<*, *, *, *> -> (method as KFunction3<T, Any?, Any?, Any?>).invoke(instance, args[0], args[1])
                is KFunction4<*, *, *, *, *> -> (method as KFunction4<T, Any?, Any?, Any?, Any?>).invoke(instance, args[0], args[1], args[2])
                else -> error("TYPE not a KFunction")
            }
        }

        //println("Can't find method: $key in $instance :: smethods=$smethodsByName, methods=$methodsByName")
        return null
    }
}

object KorteDynamicTypeScope

fun <T> KorteDynamicType(callback: KorteDynamicShapeRegister<T>.() -> Unit): KorteDynamicType<T> = object : KorteDynamicType<T> {
    val shape = KorteDynamicShape<T>().apply(callback)
    override val KorteDynamicTypeScope.__dynamicShape: KorteDynamicShape<T> get() = shape
}

interface KorteDynamicType<T> {
    val KorteDynamicTypeScope.__dynamicShape: KorteDynamicShape<T>
}

@Suppress("UNCHECKED_CAST")
interface KorteObjectMapper2 {
    val KorteDynamicType<*>.dynamicShape: KorteDynamicShape<Any?> get() = this.run { KorteDynamicTypeScope.run { this.__dynamicShape as KorteDynamicShape<Any?> } }

    fun hasProperty(instance: Any, key: String): Boolean {
        if (instance is KorteDynamicType<*>) return instance.dynamicShape.hasProp(key)
        return false
    }
    fun hasMethod(instance: Any, key: String): Boolean {
        if (instance is KorteDynamicType<*>) return instance.dynamicShape.hasMethod(key)
        return false
    }
    suspend fun invokeAsync(type: KClass<Any>, instance: Any?, key: String, args: List<Any?>): Any? {
        if (instance is KorteDynamicType<*>) return instance.dynamicShape.callMethod(instance, key, args)
        return null
    }
    suspend fun set(instance: Any, key: Any?, value: Any?) {
        if (instance is KorteDynamicType<*>) return instance.dynamicShape.setProp(instance, key, value)
    }
    suspend fun get(instance: Any, key: Any?): Any? {
        if (instance is KorteDynamicType<*>) return instance.dynamicShape.getProp(instance, key)
        return null
    }
    suspend fun accessAny(instance: Any?, key: Any?): Any? = when (instance) {
        null -> null
        is KorteDynamic2Gettable -> instance.dynamic2Get(key)
        is Map<*, *> -> instance[key]
        is Iterable<*> -> instance.toList()[KorteDynamic2.toInt(key)]
        else -> accessAnyObject(instance, key)
    }
    suspend fun accessAnyObject(instance: Any?, key: Any?): Any? {
        if (instance == null) return null
        val keyStr = KorteDynamicContext { key.toDynamicString() }
        return when {
            hasProperty(instance, keyStr) -> {
                //println("Access dynamic property : $keyStr")
                get(instance, key)
            }
            hasMethod(instance, keyStr) -> {
                //println("Access dynamic method : $keyStr")
                invokeAsync(instance::class as KClass<Any>, instance as Any?, keyStr, listOf())
            }
            else -> {
                //println("Access dynamic null : '$keyStr'")
                null
            }
        }
    }
}

expect val KorteMapper2: KorteObjectMapper2
