package com.soywiz.kmem

import kotlinx.cinterop.*
import platform.windows.*

actual open class DynamicLibraryBase actual constructor(val name: String) : DynamicSymbolResolver {
    val handle = LoadLibraryW(name)
    init {
        if (handle == null) println("Couldn't load '$name' library")
    }
    actual val isAvailable get() = handle != null
    override fun getSymbol(name: String): CPointer<CFunction<*>>? = if (handle == null) null else GetProcAddress(handle, name)?.reinterpret()
    actual fun close() {
        FreeLibrary(handle)
    }

    override fun toString(): String = "${this::class.simpleName}($name, $handle)"
}
