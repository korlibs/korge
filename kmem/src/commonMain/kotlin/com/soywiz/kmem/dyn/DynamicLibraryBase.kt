package com.soywiz.kmem.dyn

public expect open class DynamicLibraryBase(name: String) : DynamicSymbolResolver {
    public val isAvailable: Boolean
    public fun close()
}
