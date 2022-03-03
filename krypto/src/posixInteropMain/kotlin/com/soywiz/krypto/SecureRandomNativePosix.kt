package com.soywiz.krypto

import kotlinx.cinterop.*
import platform.posix.*

actual fun fillRandomBytes(array: ByteArray) {
    memScoped {
        val temp1 = allocArray<ByteVar>(1024)
        val ptr = temp1.getPointer(this)
        val file = fopen("/dev/urandom", "rb")
        if (file != null) {
            fread(ptr, 1.convert(), array.size.convert(), file)
            for (n in 0 until array.size) array[n] = ptr[n]
            fclose(file)
        }
    }
}
