package korlibs.template.dynamic

// @TODO: Hopefully someday: https://github.com/Kotlin/kotlinx.serialization/tree/dev
open class NativeObjectMapper2 : ObjectMapper2 {
    //override fun hasProperty(instance: Any, key: String): Boolean = TODO("Not supported in native yet")
    //override fun hasMethod(instance: Any, key: String): Boolean = TODO("Not supported in native yet")
    //override suspend fun invokeAsync(type: KClass<Any>, instance: Any?, key: String, args: List<Any?>) = TODO("Not supported in native yet")
    //override suspend fun set(instance: Any, key: Any?, value: Any?) = TODO("Not supported in native yet")
    //override suspend fun get(instance: Any, key: Any?): Any? = TODO("Not supported in native yet")
}

actual val Mapper2: ObjectMapper2 = NativeObjectMapper2()
