package korlibs.io.dynamic

import korlibs.io.jsGlobalDynamic

internal actual object DynamicInternal : DynApi {
	override val global: Any get() = jsGlobalDynamic

    override fun get(instance: Any?, key: String): Any? = (instance.asDynamic())[key]
    override fun set(instance: Any?, key: String, value: Any?) { (instance.asDynamic())[key] = value }
    override fun invoke(instance: Any?, key: String, args: Array<out Any?>): Any? =
        (instance.asDynamic())[key].apply(instance, args)
}
