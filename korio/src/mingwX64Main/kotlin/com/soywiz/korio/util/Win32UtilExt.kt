@file:OptIn(ExperimentalUnsignedTypes::class)

package com.soywiz.korio.util

import kotlinx.cinterop.*
import platform.windows.*

fun GetErrorAsString(error: DWORD): String {
    return memScoped {
        if (error.toInt() == 0) return ""
        val ptr = alloc<CPointerVar<CHARVar>>()
        FormatMessageA(
            (FORMAT_MESSAGE_ALLOCATE_BUFFER or FORMAT_MESSAGE_FROM_SYSTEM or FORMAT_MESSAGE_IGNORE_INSERTS).convert(),
            null, error,
            (LANG_NEUTRAL or (SUBLANG_DEFAULT shl 10)).convert(),
            ptr.ptr.reinterpret(), 0, null
        )
        val out = ptr.value?.toKString() ?: ""
        LocalFree(ptr.ptr)
        out
    }
}

fun GetLastErrorAsString(): String = GetErrorAsString(GetLastError().convert())
