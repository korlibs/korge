package korlibs.template.dynamic

import kotlin.reflect.KCallable
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3
import kotlin.reflect.KFunction4
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

interface DynamicShapeRegister<T> {
    fun register(prop: KProperty<*>): DynamicShapeRegister<T>
    fun register(callable: KCallable<*>): DynamicShapeRegister<T>
    fun register(name: String, callback: suspend T.(args: List<Any?>) -> Any?): DynamicShapeRegister<T>
    fun register(vararg items: KProperty<*>) = this.apply { for (item in items) register(item) }
    fun register(vararg items: KCallable<*>, dummy: Unit = Unit) = this.apply { for (item in items) register(item) }
}


class DynamicShape<T> : DynamicShapeRegister<T> {
    private val propertiesByName = LinkedHashMap<String, KProperty<*>>()
    private val methodsByName = LinkedHashMap<String, KCallable<*>>()
    private val smethodsByName = LinkedHashMap<String, suspend T.(args: List<Any?>) -> Any?>()

    override fun register(prop: KProperty<*>) = this.apply { propertiesByName[prop.name] = prop }
    override fun register(name: String, callback: suspend T.(args: List<Any?>) -> Any?): DynamicShapeRegister<T> = this.apply { smethodsByName[name] = callback }
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

object DynamicTypeScope

fun <T> DynamicType(callback: DynamicShapeRegister<T>.() -> Unit): DynamicType<T> = object : DynamicType<T> {
    val shape = DynamicShape<T>().apply(callback)
    override val DynamicTypeScope.__dynamicShape: DynamicShape<T> get() = shape

}

interface DynamicType<T> {
    val DynamicTypeScope.__dynamicShape: DynamicShape<T>
}
