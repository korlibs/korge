package com.soywiz.kmem.dyn

import kotlin.reflect.*

actual inline fun <reified T : Function<*>> DynamicLibrary.func(name: String?): DynamicFunLibraryNotNull<T> = DynamicFun<T>(this, name, T::class, typeOf<T>())
//public fun <T : Function<*>> DynamicLibrary.funcNull(name: String? = null): DynamicFunLibraryNull<T> = DynamicFunLibraryNull<T>(this, name)

//actual inline fun <reified T : Function<*>> DynamicLibrary.sfunc(name: String? = null): DynamicFunLibrary<T>
//actual inline fun <reified T : Function<*>> DynamicLibrary.sfuncNull(name: String? = null): DynamicFunLibraryNull<T>


@OptIn(ExperimentalStdlibApi::class)
public open class DynamicFun<T : Function<*>>(
    library: DynamicSymbolResolver,
    name: String? = null,
    val clazz: KClass<T>,
    val funcType: KType
) : DynamicFunLibraryNotNull<T>(library, name) {
    override fun getValue(obj: Any?, property: KProperty<*>): KPointerTT<KFunctionTT<T>> {
        // @TODO: We can call the global scope maybe?
        //return { } as T
        TODO()
    }
}

//actual inline operator fun <R> KPointerTT<KFunctionTT<() -> R>>.invoke(): R = TODO()
//actual inline operator fun <P1, R> KPointerTT<KFunctionTT<(P1) -> R>>.invoke(p1: P1): R = TODO()
