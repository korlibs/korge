package com.soywiz.kmem.dyn

import kotlinx.cinterop.toKString
import platform.posix.RTLD_LAZY
import platform.posix.dlclose
import platform.posix.dlopen
import platform.posix.dlsym

internal val DEBUG_DYNAMIC_LIB = platform.posix.getenv("DEBUG_DYNAMIC_LIB")?.toKString() == "true"

public actual open class DynamicLibraryBase actual constructor(val names: List<String>) : DynamicSymbolResolver {
    var name: String = names.firstOrNull() ?: "unknown"
    val handle = names
        .flatMap {
            when {
                it.endsWith(".dylib", ignoreCase = true) -> listOf(it)
                it.endsWith(".dll", ignoreCase = true) -> listOf(it)
                else -> listOf(it, "$it.so", "$it.so.1", "$it.so.2", "$it.so.3", "$it.so.4", "$it.so.5", "$it.so.6", "$it.so.7", "$it.so.8", "$it.so.9")
            }
        }
        .firstNotNullOfOrNull { name ->
            this@DynamicLibraryBase.name = name
            val handle = dlopen(name, RTLD_LAZY)
            //println("name: $name, handle: $handle")
            handle
        } ?: error("Can't load $names")
    init {
        if (DEBUG_DYNAMIC_LIB) println("Loaded '$names'...$handle")
        if (handle == null) println("Couldn't load '$names' library")
    }
    public actual val isAvailable get() = handle != null
    override fun getSymbol(name: String): KPointer? {
        if (DEBUG_DYNAMIC_LIB) println("Requesting ${this.name}.$name...")
        val out = if (handle == null) null else dlsym(handle, name)
        if (DEBUG_DYNAMIC_LIB) println("Got ${this.name}.$name...$out")
        return out
    }
    public actual fun close() {
        dlclose(handle)
    }
    override fun toString(): String = "${this::class.simpleName}($name, $handle)"
}
