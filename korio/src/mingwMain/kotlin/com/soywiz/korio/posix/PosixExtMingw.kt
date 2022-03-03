package com.soywiz.korio.posix

import kotlinx.cinterop.*
import platform.posix.*

actual fun posixFopen(filename: String, mode: String): CPointer<FILE>? {
    return memScoped {
        //setlocale(LC_ALL, ".UTF-8") // On Windows 10 : https://docs.microsoft.com/en-us/cpp/c-runtime-library/reference/setlocale-wsetlocale?redirectedfrom=MSDN&view=msvc-160#utf-8-support
        platform.posix._wfopen(filename.wcstr, mode.wcstr)
    }
}
