package com.soywiz.kmem.dyn

import kotlinx.cinterop.*
import platform.posix.*
import kotlin.test.*

class DynamicLibraryNativeTest {
    //object C : DynamicLibrary("c") {
    object C : DynamicLibrary("libSystem.dylib") {
        //val cos by func<(value: Double) -> Double>()
        val strlen by func<(value: KPointer?) -> Int>()
    }

    @Test
    fun test() {
        kmemScoped {
            val bytes = allocBytes(32)
            bytes.setByte(0, 1)
            bytes.setByte(1, 2)
            bytes.setByte(2, 3)
            println(C.strlen.invoke(bytes.toLong().toCPointer()!!))
        }
    }
}
