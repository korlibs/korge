package korlibs.memory.dyn

import korlibs.memory.dyn.*

public actual open class DynamicLibraryBase actual constructor(names: List<String>) : DynamicSymbolResolver {
    actual val isAvailable: Boolean get() = false
    override fun getSymbol(name: String): KPointer? = TODO()
    actual fun close() {
    }
}
