package com.soywiz.kmem.dyn

import com.soywiz.kmem.*
import kotlinx.cinterop.*
import kotlin.reflect.*
import kotlin.test.*

class DynamicLibraryCommonTest {
    //object C : DynamicLibrary(Platform.C_LIBRARY_NAME) {
    object C : DynamicLibrary("libSystem.dylib", "libc", "MSVCRT") {
        //val cos by func<(value: Double) -> Double>()
        val strlen by sfunc<(value: KPointer?) -> Int>()
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
            println(C.strlen.invoke(mem))
        }
    }
}
