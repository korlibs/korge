package korlibs.memory.dyn

import com.sun.jna.*

public actual open class DynamicLibraryBase actual constructor(names: List<String>) : DynamicSymbolResolver {
    val library: NativeLibrary? = run {
        var ex: Throwable? = null
        for (name in names) {
            try {
                val instance = NativeLibrary.getInstance(name)
                if (instance != null) return@run instance
            } catch (e: Throwable) {
                if (ex == null) ex = e
            }
        }
        if (ex != null) {
            ex?.printStackTrace()
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
