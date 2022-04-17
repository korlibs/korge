package com.soywiz.kmem.dyn

public actual open class DynamicLibraryBase actual constructor(name: String) : DynamicSymbolResolver {
    actual val isAvailable: Boolean get() = false
    override fun getSymbol(name: String): KPointer? = TODO()
    actual fun close() {
    }
}
