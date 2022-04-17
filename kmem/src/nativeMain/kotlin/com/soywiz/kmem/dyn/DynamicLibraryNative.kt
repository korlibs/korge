package com.soywiz.kmem.dyn

import kotlinx.cinterop.*
import kotlin.native.internal.NativePtr
import kotlin.reflect.*

public fun <T : Function<*>> DynamicLibrary.func(name: String? = null): DynamicFun<T> = DynamicFun<T>(this, name)
public fun <T : Function<*>> DynamicLibrary.funcNull(name: String? = null): DynamicFunNull<T> = DynamicFunNull<T>(this, name)

public open class DynamicFun<T : Function<*>>(library: DynamicSymbolResolver, name: String? = null) : DynamicFunLibrary<T>(library, name) {
    public operator fun getValue(obj: Any?, property: KProperty<*>): CPointer<CFunction<T>> {
        val out: NativePtr? = _getValue(property)
        if (out == null || out == NativePtr.NULL) {
            val message = "Can't find function '${getFuncName(property)}' in $this"
            println(message)
            error(message)
        }
        return interpretCPointer(out) ?: error("DynamicFun.getValue null: ${getFuncName(property)}")
    }
}

public open class DynamicFunNull<T : Function<*>>(library: DynamicSymbolResolver, name: String? = null) : DynamicFunLibrary<T>(library, name) {
    public operator fun getValue(obj: Any?, property: KProperty<*>): CPointer<CFunction<T>>? = _getValue(property)?.let { interpretCPointer(it) }
}

