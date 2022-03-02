package com.soywiz.kmem

import kotlinx.cinterop.*
import platform.windows.*

public actual open class DynamicLibraryBase actual constructor(public val name: String) : DynamicSymbolResolver {
    public val handle: CPointer<HINSTANCE__>? = LoadLibraryW(name)
    init {
        if (handle == null) println("Couldn't load '$name' library")
    }
    public actual val isAvailable: Boolean get() = handle != null
    override fun getSymbol(name: String): CPointer<CFunction<*>>? = if (handle == null) null else GetProcAddress(handle, name)?.reinterpret()
    public actual fun close() {
        FreeLibrary(handle)
    }

    override fun toString(): String = "${this::class.simpleName}($name, $handle)"
}
