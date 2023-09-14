package korlibs.template.dynamic

import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
interface ObjectMapper2 {
    val DynamicType<*>.dynamicShape: DynamicShape<Any?> get() = this.run { DynamicTypeScope.run { this.__dynamicShape as DynamicShape<Any?> } }

    fun hasProperty(instance: Any, key: String): Boolean {
        if (instance is DynamicType<*>) return instance.dynamicShape.hasProp(key)
        return false
    }
    fun hasMethod(instance: Any, key: String): Boolean {
        if (instance is DynamicType<*>) return instance.dynamicShape.hasMethod(key)
        return false
    }
    suspend fun invokeAsync(type: KClass<Any>, instance: Any?, key: String, args: List<Any?>): Any? {
        if (instance is DynamicType<*>) return instance.dynamicShape.callMethod(instance, key, args)
        return null
    }
    suspend fun set(instance: Any, key: Any?, value: Any?) {
        if (instance is DynamicType<*>) return instance.dynamicShape.setProp(instance, key, value)
    }
    suspend fun get(instance: Any, key: Any?): Any? {
        if (instance is DynamicType<*>) return instance.dynamicShape.getProp(instance, key)
        return null
    }
    suspend fun accessAny(instance: Any?, key: Any?): Any? = when (instance) {
        null -> null
        is Dynamic2Gettable -> instance.dynamic2Get(key)
        is Map<*, *> -> instance[key]
        is Iterable<*> -> instance.toList()[Dynamic2.toInt(key)]
        else -> accessAnyObject(instance, key)
    }
    suspend fun accessAnyObject(instance: Any?, key: Any?): Any? {
        if (instance == null) return null
        val keyStr = DynamicContext { key.toDynamicString() }
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

expect val Mapper2: ObjectMapper2
