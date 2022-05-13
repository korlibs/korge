package com.soywiz.kmem.dyn

import kotlinx.cinterop.CPointer
import platform.windows.FreeLibrary
import platform.windows.GetProcAddress
import platform.windows.HINSTANCE__
import platform.windows.LoadLibraryW

public actual open class DynamicLibraryBase actual constructor(public val name: String) : DynamicSymbolResolver {
    public val handle: CPointer<HINSTANCE__>? = LoadLibraryW(name)
    init {
        if (handle == null) println("Couldn't load '$name' library")
    }
    public actual val isAvailable: Boolean get() = handle != null
    override fun getSymbol(name: String): KPointer? = when (handle) {
        null -> null
        else -> GetProcAddress(handle, name)?.rawValue
    }
    public actual fun close() {
        FreeLibrary(handle)
    }

    override fun toString(): String = "${this::class.simpleName}($name, $handle)"
}
