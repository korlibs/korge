package org.luaj.vm2.internal

import org.luaj.vm2.io.*
import kotlin.reflect.*

internal expect object JSystem {
    val out: LuaWriter
    val err: LuaWriter
    val `in`: LuaBinInput
    fun exit(code: Int)
    fun getProperty(key: String, def: String? = null): String?
    fun gc()
    fun totalMemory(): Long
    fun freeMemory(): Long
    fun InstantiateClassByName(name: String): Any?
    fun StartNativeThread(runnable: () -> Unit, name: String)
    fun Object_notify(obj: Any)
    fun Object_wait(obj: Any)
    fun Object_wait(obj: Any, time: Long)
    fun Class_portableName(clazz: KClass<*>): String
    fun Class_isInstancePortable(clazz: KClass<*>, ins: Any): Boolean
    fun Class_getResourceAsStreamPortable(clazz: KClass<*>, res: String): LuaBinInput?
    val supportStatic: Boolean
}

expect open class IOException : Exception {
    constructor()
    constructor(message: String)
}

expect open class EOFException : IOException {
    constructor()
    constructor(message: String)
}

expect class InterruptedException : Exception

expect class WeakReference<T>(value: T) {
    fun get(): T?
}
