package korlibs.memory.dyn

import kotlinx.cinterop.*
import platform.posix.RTLD_LAZY
import platform.posix.dlclose
import platform.posix.dlopen
import platform.posix.dlsym

internal val DEBUG_DYNAMIC_LIB = platform.posix.getenv("DEBUG_DYNAMIC_LIB")?.toKString() == "true"

public actual open class DynamicLibraryBase actual constructor(val names: List<String>) : DynamicSymbolResolver {
    var name: String = names.firstOrNull() ?: "unknown"
    val explored = names
        .flatMap {
            when {
                it.endsWith(".dylib", ignoreCase = true) -> listOf(it)
                it.endsWith(".dll", ignoreCase = true) -> listOf(it)
                else -> {
                    val itnoso = it.replace(Regex("\\.so(\\.\\d+)?$"), "")
                    listOf(it, "$itnoso.so", "$itnoso.so.1", "$itnoso.so.2", "$itnoso.so.3", "$itnoso.so.4", "$itnoso.so.5", "$itnoso.so.6", "$itnoso.so.7", "$itnoso.so.8", "$itnoso.so.9")
                }
            }
        }
    val handle = explored
        .firstNotNullOfOrNull { name ->
            this@DynamicLibraryBase.name = name
            val handle = dlopen(name, RTLD_LAZY)
            //println("name: $name, handle: $handle")
            handle
        }
        //?: error("Can't load $names")
    init {
        if (DEBUG_DYNAMIC_LIB) println("Loaded '$names'...$handle")
        if (handle == null) println("Couldn't load '$names' library : explored=$explored")
    }
    public actual val isAvailable get() = handle != null
    override fun getSymbol(name: String): KPointer? {
        if (DEBUG_DYNAMIC_LIB) println("Requesting ${this.name}.$name...")
        val out = if (handle == null) null else dlsym(handle, name)
        if (DEBUG_DYNAMIC_LIB) println("Got ${this.name}.$name...$out")
        return out
    }
    public actual fun close() {
        dlclose(handle)
    }
    override fun toString(): String = "${this::class.simpleName}($name, $handle)"
}
