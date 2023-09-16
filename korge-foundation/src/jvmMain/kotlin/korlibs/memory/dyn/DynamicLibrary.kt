package korlibs.memory.dyn

import kotlin.reflect.*

public open class DynamicLibrary(vararg names: String?) : DynamicLibraryBase(names.filterNotNull()) {
}

public fun interface DynamicSymbolResolver {
    public fun getSymbol(name: String): KPointer?
}

public abstract class DynamicFunBase<T : Function<*>>(public val name: String? = null) {
    private var _set = false
    private var _value: KPointer? = null

    protected fun getFuncName(property: KProperty<*>): String = name ?: property.name.removeSuffix("Ext")

    protected abstract fun getProcAddress(name: String): KPointer?

    protected fun _getValue(property: KProperty<*>): KPointer? {
        if (!_set) {
            _value = getProcAddress(getFuncName(property))
            _set = true
        }
        return _value
    }
}

public abstract class DynamicFunLibrary<T : Function<*>>(public val library: DynamicSymbolResolver, name: String? = null) : DynamicFunBase<T>(name) {
    override fun getProcAddress(name: String): KPointer? = library.getSymbol(name)

    override fun toString(): String = "DynamicFunLibrary($library)"
}

public abstract class DynamicFunLibraryNotNull<T : Function<*>>(library: DynamicSymbolResolver, name: String? = null) : DynamicFunLibrary<T>(library, name) {
    abstract operator fun getValue(obj: Any?, property: KProperty<*>): KPointerTT<KFunctionTT<T>>
}

public abstract class DynamicFunLibraryNull<T : Function<*>>(library: DynamicSymbolResolver, name: String? = null) : DynamicFunLibrary<T>(library, name) {
    abstract operator fun getValue(obj: Any?, property: KProperty<*>): KPointerTT<KFunctionTT<T>>?
}
