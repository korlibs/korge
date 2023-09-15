package korlibs.memory.dyn

import com.sun.jna.*

public open class DynamicLibraryBase constructor(names: List<String>) : DynamicSymbolResolver {
    private val library: NativeLibrary? = run {
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

    val isAvailable: Boolean get() = false
    override fun getSymbol(name: String): KPointer? {
        return KPointer(library?.getGlobalVariableAddress(name)?.address ?: 0L)
    }
    fun close() {
        library?.close()
    }
}
