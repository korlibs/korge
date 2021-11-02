package com.soywiz.kmem

import kotlinx.cinterop.*
import kotlin.native.concurrent.*
import kotlin.reflect.*

expect open class DynamicLibraryBase(name: String) : DynamicSymbolResolver {
    val isAvailable: Boolean
    fun close()
}

open class DynamicLibrary(name: String) : DynamicLibraryBase(name) {
    fun <T : Function<*>> func(name: String? = null) = DynamicFun<T>(this, name)
    fun <T : Function<*>> funcNull(name: String? = null) = DynamicFunNull<T>(this, name)
}

fun interface DynamicSymbolResolver {
    fun getSymbol(name: String): CPointer<CFunction<*>>?
}

abstract class DynamicFunBase<T : Function<*>>(val name: String? = null) {
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

abstract class DynamicFunLibrary<T : Function<*>>(val library: DynamicSymbolResolver, name: String? = null) : DynamicFunBase<T>(name) {
    override fun glGetProcAddressT(name: String): CPointer<CFunction<T>>? = library.getSymbol(name) as CPointer<CFunction<T>>?
}

open class DynamicFun<T : Function<*>>(library: DynamicSymbolResolver, name: String? = null) : DynamicFunLibrary<T>(library, name) {
    operator fun getValue(obj: Any?, property: KProperty<*>): CPointer<CFunction<T>> {
        val out = _getValue(property)
        if (out == null) {
            val message = "Can't find function '${getFuncName(property)}' in $this"
            println(message)
            error(message)
        }
        return out
    }
}

open class DynamicFunNull<T : Function<*>>(library: DynamicSymbolResolver, name: String? = null) : DynamicFunLibrary<T>(library, name) {
    operator fun getValue(obj: Any?, property: KProperty<*>): CPointer<CFunction<T>>? = _getValue(property)
}

