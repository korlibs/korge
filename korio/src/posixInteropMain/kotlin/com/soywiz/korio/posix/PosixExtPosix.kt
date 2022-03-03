package com.soywiz.korio.posix

import kotlinx.cinterop.*
import platform.posix.*

actual fun posixFopen(filename: String, mode: String): CPointer<FILE>? {
    return fopen(filename, mode)
}
