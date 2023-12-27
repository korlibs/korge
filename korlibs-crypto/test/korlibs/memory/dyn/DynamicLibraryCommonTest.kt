package korlibs.memory.dyn

/*
import korlibs.memory.*
import kotlinx.cinterop.*
import kotlin.test.*

class DynamicLibraryCommonTest {
    //object C : DynamicLibrary(Platform.C_LIBRARY_NAME) {
    object C : DynamicLibrary("libSystem.dylib", "libc", "MSVCRT") {
        val strlen by func<(value: KPointer?) -> Int>()
        val malloc by func<(size: Int) -> KPointer?>()
        val free by func<(ptr: KPointer?) -> Unit>()
    }
    object M : DynamicLibrary("libSystem.dylib", "libm", "MSVCRT") {
        val cos by func<(value: Double) -> Double>()
    }

    //inline fun <reified T> typeOfTest() {
    //    val type = typeOf<T>()
    //    println(type.arguments)
    //}

    val shouldRun: Boolean get() = !Platform.isJs && !Platform.isAndroid

    @Test
    fun testLibC() {
        if (!shouldRun) return

        kmemScoped {
            val mem = allocBytes(32)
            mem.setByte(0, 1)
            mem.setByte(1, 2)
            mem.setByte(2, 3)
            mem.setByte(3, 0)
            assertEquals(3, C.strlen(mem))
            val ptr = C.malloc(10) ?: error("malloc returned null")
            try {
                ptr.setByte(0, 1)
                ptr.setByte(1, 2)
                ptr.setByte(2, 0)
                assertEquals(2, C.strlen(ptr))
            } finally {
                C.free(ptr)
            }
        }
    }

    @Test
    fun testLibM() {
        if (!shouldRun) return

        assertEquals(1.0, M.cos(0.0), 0.001)
    }
}
*/
