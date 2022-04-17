package com.soywiz.kmem.dyn

import com.soywiz.kmem.atomic.*
import kotlin.reflect.*

public open class DynamicLibrary(name: String) : DynamicLibraryBase(name) {
}

public fun interface DynamicSymbolResolver {
    public fun getSymbol(name: String): KPointer?
}

public abstract class DynamicFunBase<T : Function<*>>(public val name: String? = null) {
    private var _set = KmemAtomicRef(false)
    private var _value = KmemAtomicRef<KPointer?>(null)

    protected fun getFuncName(property: KProperty<*>): String = name ?: property.name.removeSuffix("Ext")

    protected abstract fun getProcAddress(name: String): KPointer?

    protected fun _getValue(property: KProperty<*>): KPointer? {
        if (!_set.value) {
            _value.value = getProcAddress(getFuncName(property))
            _set.value = true
        }
        return _value.value
    }
}

public abstract class DynamicFunLibrary<T : Function<*>>(public val library: DynamicSymbolResolver, name: String? = null) : DynamicFunBase<T>(name) {
    override fun getProcAddress(name: String): KPointer? = library.getSymbol(name)

    override fun toString(): String = "DynamicFunLibrary($library)"
}
