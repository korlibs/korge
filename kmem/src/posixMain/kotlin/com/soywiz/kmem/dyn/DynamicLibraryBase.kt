package com.soywiz.kmem.dyn

import kotlinx.cinterop.toKString
import platform.posix.RTLD_LAZY
import platform.posix.dlclose
import platform.posix.dlopen
import platform.posix.dlsym

internal val DEBUG_DYNAMIC_LIB = platform.posix.getenv("DEBUG_DYNAMIC_LIB")?.toKString() == "true"

public actual open class DynamicLibraryBase actual constructor(val names: List<String>) : DynamicSymbolResolver {
    var name: String = names.firstOrNull() ?: "unknown"
    val handle = names.firstNotNullOfOrNull { name ->
        this@DynamicLibraryBase.name = name
        dlopen(name, RTLD_LAZY)
    }
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
