package korlibs.memory.dyn

import com.sun.jna.*

annotation class NativeName(val name: String) {
    companion object {
        val OPTIONS = mapOf(
            Library.OPTION_FUNCTION_MAPPER to FunctionMapper { _, method ->
                method.getAnnotation(NativeName::class.java)?.name ?: method.name
            }
        )
    }
}

inline fun <reified T : Library> NativeLoad(name: String): T = Native.load(name, T::class.java, NativeName.OPTIONS) as T

