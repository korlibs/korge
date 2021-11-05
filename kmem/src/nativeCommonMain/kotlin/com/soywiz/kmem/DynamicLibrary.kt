package com.soywiz.kmem

import kotlinx.cinterop.*
import kotlin.native.concurrent.*
import kotlin.reflect.*

public expect open class DynamicLibraryBase(name: String) : DynamicSymbolResolver {
    public val isAvailable: Boolean
    public fun close()
}

public open class DynamicLibrary(name: String) : DynamicLibraryBase(name) {
    public fun <T : Function<*>> func(name: String? = null): DynamicFun<T> = DynamicFun<T>(this, name)
    public fun <T : Function<*>> funcNull(name: String? = null): DynamicFunNull<T> = DynamicFunNull<T>(this, name)
}

public fun interface DynamicSymbolResolver {
    public fun getSymbol(name: String): CPointer<CFunction<*>>?
}

public abstract class DynamicFunBase<T : Function<*>>(public val name: String? = null) {
    private var _set = AtomicInt(0)
    private var _value = AtomicReference<CPointer<CFunction<T>>?>(null)

    protected fun getFuncName(property: KProperty<*>): String = name ?: property.name.removeSuffix("Ext")

    protected abstract fun glGetProcAddressT(name: String): CPointer<CFunction<T>>?

    protected fun _getValue(property: KProperty<*>): CPointer<CFunction<T>>? {
        if (_set.value == 0) {
            _value.value = glGetProcAddressT(getFuncName(property))
            _set.value = 1
        }
        return _value.value
    }
}

public abstract class DynamicFunLibrary<T : Function<*>>(public val library: DynamicSymbolResolver, name: String? = null) : DynamicFunBase<T>(name) {
    override fun glGetProcAddressT(name: String): CPointer<CFunction<T>>? = library.getSymbol(name) as CPointer<CFunction<T>>?
}

public open class DynamicFun<T : Function<*>>(library: DynamicSymbolResolver, name: String? = null) : DynamicFunLibrary<T>(library, name) {
    public operator fun getValue(obj: Any?, property: KProperty<*>): CPointer<CFunction<T>> {
        val out = _getValue(property)
        if (out == null) {
            val message = "Can't find function '${getFuncName(property)}' in $this"
            println(message)
            error(message)
        }
        return out
    }
}

public open class DynamicFunNull<T : Function<*>>(library: DynamicSymbolResolver, name: String? = null) : DynamicFunLibrary<T>(library, name) {
    public operator fun getValue(obj: Any?, property: KProperty<*>): CPointer<CFunction<T>>? = _getValue(property)
}

