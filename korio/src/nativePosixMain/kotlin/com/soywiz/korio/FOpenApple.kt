package com.soywiz.korio

import kotlinx.cinterop.*

actual fun posixFopen(filename: String, mode: String): CPointer<platform.posix.FILE>? {
    return memScoped {
        platform.posix.fopen(filename, mode)
    }
}
