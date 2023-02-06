package com.soywiz.kmem.dyn

import com.soywiz.kmem.*
import kotlinx.cinterop.*
import kotlin.test.*

class DynamicLibraryCommonTest {
    //object C : DynamicLibrary(Platform.C_LIBRARY_NAME) {
    object C : DynamicLibrary("libSystem.dylib", "libc", "MSVCRT") {
        val cos by func<(value: Double) -> Double>()
        val strlen by func<(value: KPointer?) -> Int>()
        val malloc by func<(size: Int) -> KPointer?>()
        val free by func<(ptr: KPointer?) -> Unit>()
    }

    //inline fun <reified T> typeOfTest() {
    //    val type = typeOf<T>()
    //    println(type.arguments)
    //}

    @Test
    fun test() {
        if (Platform.isJs) return
        if (Platform.isAndroid) return

        kmemScoped {
            val mem = allocBytes(32)
            mem.setByte(0, 1)
            mem.setByte(1, 2)
            mem.setByte(2, 3)
            assertEquals(3, C.strlen.invoke(mem))
            assertEquals(1.0, C.cos(0.0), 0.001)
            val ptr = C.malloc(10) ?: error("malloc returned null")
            try {
                ptr.setByte(0, 1)
                ptr.setByte(1, 2)
                ptr.setByte(2, 0)
                assertEquals(2, C.strlen.invoke(ptr))
            } finally {
                C.free(ptr)
            }
        }
    }
}
