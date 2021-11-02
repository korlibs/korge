package com.soywiz.kmem

import kotlinx.cinterop.*
import platform.posix.RTLD_LAZY
import platform.posix.dlopen
import platform.posix.dlsym
import platform.posix.dlclose

actual open class DynamicLibraryBase actual constructor(val name: String) : DynamicSymbolResolver {
    val handle = dlopen(name, RTLD_LAZY)
    init {
        if (handle == null) println("Couldn't load '$name' library")
    }
    actual val isAvailable get() = handle != null
    override fun getSymbol(name: String): CPointer<CFunction<*>>? = if (handle == null) null else dlsym(handle, name)?.reinterpret()
    actual fun close() {
        dlclose(handle)
    }
    override fun toString(): String = "${this::class.simpleName}($name, $handle)"
}
