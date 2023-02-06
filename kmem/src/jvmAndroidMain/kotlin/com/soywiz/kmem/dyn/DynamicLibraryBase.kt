package com.soywiz.kmem.dyn

import com.sun.jna.*

public actual open class DynamicLibraryBase actual constructor(names: List<String>) : DynamicSymbolResolver {
    val library: NativeLibrary? = run {
        for (name in names) {
            val instance = NativeLibrary.getInstance(name)
            if (instance != null) return@run instance
        }
        null
    }

    actual val isAvailable: Boolean get() = false
    override fun getSymbol(name: String): KPointer? {
        return KPointerTT(library?.getGlobalVariableAddress(name))
    }
    actual fun close() {
        library?.close()
    }
}
