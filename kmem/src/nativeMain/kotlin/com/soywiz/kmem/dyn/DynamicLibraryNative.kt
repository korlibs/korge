package com.soywiz.kmem.dyn

import kotlinx.cinterop.*
import kotlin.native.internal.NativePtr
import kotlin.reflect.*

public actual inline fun <reified T : Function<*>> DynamicLibrary.func(name: String?): DynamicFunLibraryNotNull<T> = DynamicFun<T>(this, name)
//public actual inline fun <reified T : Function<*>> DynamicLibrary.sfuncNull(name: String?): DynamicFunLibraryNull<T> = DynamicFunNull<T>(this, name)

//public inline fun <reified T : Function<*>> DynamicLibrary.func(name: String? = null): DynamicFun<T> = DynamicFun<T>(this, name)
public inline fun <reified T : Function<*>> DynamicLibrary.funcNull(name: String? = null): DynamicFunNull<T> = DynamicFunNull<T>(this, name)

public open class DynamicFun<T : Function<*>>(library: DynamicSymbolResolver, name: String? = null) : DynamicFunLibraryNotNull<T>(library, name) {
    override fun getValue(obj: Any?, property: KProperty<*>): KPointerTT<KFunctionTT<T>> {
        val out: NativePtr? = _getValue(property)?.rawValue
        if (out == null || out == NativePtr.NULL) {
            val message = "Can't find function '${getFuncName(property)}' in $this"
            println(message)
            error(message)
        }
        return interpretCPointer(out) ?: error("DynamicFun.getValue null: ${getFuncName(property)}")
    }
}

public open class DynamicFunNull<T : Function<*>>(library: DynamicSymbolResolver, name: String? = null) : DynamicFunLibraryNull<T>(library, name) {
    //public operator fun getValue(obj: Any?, property: KProperty<*>): CPointer<CFunction<T>>? = _getValue(property)?.let { interpretCPointer(it.rawValue) }
    override fun getValue(obj: Any?, property: KProperty<*>): KPointerTT<KFunctionTT<T>>? {
        return _getValue(property)?.let { interpretCPointer(it.rawValue) }
    }
}

// @TODO: This doesn't work in K/N: type R of com.soywiz.kmem.dyn.invoke  of return value is not supported here: doesn't correspond to any C type
//actual inline operator fun <R> CPointer<CFunction<() -> R>>.invoke(): R = this.invoke2()
//actual inline operator fun <P1, R> CPointer<CFunction<(P1) -> R>>.invoke(p1: P1): R = this.invoke2(p1)
