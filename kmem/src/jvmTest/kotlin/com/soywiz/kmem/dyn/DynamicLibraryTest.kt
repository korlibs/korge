package com.soywiz.kmem.dyn

import com.sun.jna.*
import kotlinx.cinterop.*
import java.lang.reflect.*
import kotlin.reflect.*
import kotlin.test.*

class DynamicLibraryTest {
    //object C : DynamicLibrary(Platform.C_LIBRARY_NAME) {
    object C : DynamicLibrary("libSystem.dylib") {
        //val cos by func<(value: Double) -> Double>()
        val strlen by sfunc<(value: KPointer?) -> Int>()
    }

    @Test
    fun test() {
        kmemScoped {
            val mem = allocBytes(32)
            mem.setByte(0, 1)
            mem.setByte(1, 2)
            mem.setByte(2, 3)
            println(C.strlen.invoke(mem))
        }
    }
}
