package com.soywiz.kmem.dyn

//expect class FunctionRef<T : Function<*>>

expect inline fun <reified T : Function<*>> DynamicLibrary.sfunc(name: String? = null): DynamicFunLibraryNotNull<T>
//expect inline fun <reified T : Function<*>> DynamicLibrary.sfuncNull(name: String? = null): DynamicFunLibraryNull<T>


//expect inline operator fun <R> KPointerTT<KFunctionTT<() -> R>>.invoke(): R
//expect inline operator fun <P1, R> KPointerTT<KFunctionTT<(P1) -> R>>.invoke(p1: P1): R
