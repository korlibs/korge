package com.soywiz.kmem.dyn

public expect open class DynamicLibraryBase(names: List<String>) : DynamicSymbolResolver {
    public val isAvailable: Boolean
    public fun close()
}
