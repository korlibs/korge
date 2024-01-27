package korlibs.io.dynamic

internal actual object DynamicInternal : DynApi {
	actual override fun get(instance: Any?, key: String): Any? = throw UnsupportedOperationException("DynamicInternal.get")
    actual override fun set(instance: Any?, key: String, value: Any?): Unit = throw UnsupportedOperationException("DynamicInternal.set")
    actual override fun invoke(instance: Any?, key: String, args: Array<out Any?>): Any? = throw UnsupportedOperationException("DynamicInternal.invoke")
}
