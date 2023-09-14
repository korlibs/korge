package korlibs.template.dynamic

import korlibs.template.util.KorteDeferred
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

open class JvmObjectMapper2 : ObjectMapper2 {
    class ClassReflectCache<T : Any>(val clazz: KClass<T>) {
        data class MyProperty(
            val name: String,
            val getter: Method? = null,
            val setter: Method? = null,
            val field: Field? = null
        )

        val jclass = clazz.java
        val methodsByName = jclass.allDeclaredMethods.associateBy { it.name }
        val fieldsByName = jclass.allDeclaredFields.associateBy { it.name }
        val potentialPropertyNamesFields = jclass.allDeclaredFields.map { it.name }
        val potentialPropertyNamesGetters =
            jclass.allDeclaredMethods.filter { it.name.startsWith("get") }.map { it.name.substring(3).decapitalize() }
        val potentialPropertyNames = (potentialPropertyNamesFields + potentialPropertyNamesGetters).toSet()
        val propByName = potentialPropertyNames.map { propName ->
            MyProperty(
                propName,
                methodsByName["get${propName.capitalize()}"],
                methodsByName["set${propName.capitalize()}"],
                fieldsByName[propName]
            )
        }.associateBy { it.name }
    }

    val KClass<*>.classInfo by WeakPropertyThis<KClass<*>, ClassReflectCache<*>> { ClassReflectCache(this) }

    override fun hasProperty(instance: Any, key: String): Boolean {
        if (instance is DynamicType<*>) return instance.dynamicShape.hasProp(key)
        return key in instance::class.classInfo.propByName
    }

    override fun hasMethod(instance: Any, key: String): Boolean {
        if (instance is DynamicType<*>) return instance.dynamicShape.hasMethod(key)
        return instance::class.classInfo.methodsByName[key] != null
    }

    override suspend fun invokeAsync(type: KClass<Any>, instance: Any?, key: String, args: List<Any?>): Any? {
        if (instance is DynamicType<*>) return instance.dynamicShape.callMethod(instance, key, args)
        val method = type.classInfo.methodsByName[key] ?: return null
        return method.invokeSuspend(instance, args)
    }

    override suspend fun set(instance: Any, key: Any?, value: Any?) {
        if (instance is DynamicType<*>) return instance.dynamicShape.setProp(instance, key, value)
        val prop = instance::class.classInfo.propByName[key] ?: return
        when {
            prop.setter != null -> prop.setter.invoke(instance, value)
            prop.field != null -> prop.field.set(instance, value)
            else -> Unit
        }
    }

    override suspend fun get(instance: Any, key: Any?): Any? {
        if (instance is DynamicType<*>) return instance.dynamicShape.getProp(instance, key)
        val prop = instance::class.classInfo.propByName[key] ?: return null
        return when {
            prop.getter != null -> prop.getter.invoke(instance)
            prop.field != null -> prop.field.get(instance)
            else -> null
        }
    }
}

private class WeakPropertyThis<T : Any, V>(val gen: T.() -> V) {
    val map = WeakHashMap<T, V>()

    operator fun getValue(obj: T, property: KProperty<*>): V = map.getOrPut(obj) { gen(obj) }
    operator fun setValue(obj: T, property: KProperty<*>, value: V) { map[obj] = value }
}

private val Class<*>.allDeclaredFields: List<Field>
    get() = this.declaredFields.toList() + (this.superclass?.allDeclaredFields?.toList() ?: listOf<Field>())

private fun Class<*>.isSubtypeOf(that: Class<*>) = that.isAssignableFrom(this)

private val Class<*>.allDeclaredMethods: List<Method>
    get() = this.declaredMethods.toList() + (this.superclass?.allDeclaredMethods?.toList() ?: listOf<Method>())

suspend fun Method.invokeSuspend(obj: Any?, args: List<Any?>): Any? {
    val method = this@invokeSuspend
    val cc = coroutineContext

    val lastParam = method.parameterTypes.lastOrNull()
    val margs = java.util.ArrayList(args)
    var deferred: KorteDeferred<Any?>? = null

    if (lastParam != null && lastParam.isAssignableFrom(Continuation::class.java)) {
        deferred = KorteDeferred<Any?>()
        margs += deferred.toContinuation(cc)
    }
    val result = method.invoke(obj, *margs.toTypedArray())
    return when (result) {
        COROUTINE_SUSPENDED -> deferred?.await()
        else -> result
    }
}

actual val Mapper2: ObjectMapper2 = JvmObjectMapper2()
