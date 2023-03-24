package korlibs.memory.dyn

import kotlinx.cinterop.CPointer
import platform.windows.FreeLibrary
import platform.windows.GetProcAddress
import platform.windows.HINSTANCE__
import platform.windows.LoadLibraryW

public actual open class DynamicLibraryBase actual constructor(public val names: List<String>) : DynamicSymbolResolver {
    val name: String get() = names.firstOrNull() ?: "At least one name should be provided"
    public val handle: CPointer<HINSTANCE__>? = names.firstNotNullOfOrNull { LoadLibraryW(it) }
        //?: error("Can't load '$names' library")
    init {
        if (handle == null) println("Couldn't load '$names' library")
    }
    public actual val isAvailable: Boolean get() = handle != null
    override fun getSymbol(name: String): KPointer? = when (handle) {
        null -> null
        else -> GetProcAddress(handle, name)
    }
    public actual fun close() {
        FreeLibrary(handle)
    }

    override fun toString(): String = "${this::class.simpleName}($name, $handle)"
}