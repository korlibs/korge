package com.soywiz.korgw.platform

import com.sun.jna.FunctionMapper
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

annotation class NativeName(val name: String) {
    companion object {
        val OPTIONS = mapOf(
            Library.OPTION_FUNCTION_MAPPER to FunctionMapper { _, method ->
                method.getAnnotation(NativeName::class.java)?.name ?: method.name
            }
        )
    }
}

typealias NSRectPtr = Pointer

inline fun <reified T : Library> NativeLoad(name: String) = Native.load(name, T::class.java, NativeName.OPTIONS) as T
