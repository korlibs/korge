package com.soywiz.korge.native

import kotlinx.cinterop.*
import platform.posix.*

object KorgeSimpleNativeSyncIO {
    fun writeBytes(file: String, bytes: ByteArray) {
        val fd = fopen(file, "wb") ?: error("Can't open file '$file' for writing")
        try {
            if (bytes.isNotEmpty()) {
                memScoped {
                    bytes.usePinned { pin ->
                        fwrite(pin.addressOf(0), 1.convert(), bytes.size.convert(), fd)
                    }
                }
            }
        } finally {
            fclose(fd)
        }
    }

    fun readBytes(file: String): ByteArray {
        val fd = fopen(file, "rb") ?: error("Can't open file '$file' for reading")
        try {
            fseek(fd, 0L, SEEK_END)
            val fileSize = ftell(fd)
            fseek(fd, 0L, SEEK_CUR)

            val out = ByteArray(fileSize.toInt())
            if (out.isNotEmpty()) {
                memScoped {
                    out.usePinned { pin ->
                        fread(pin.addressOf(0), 1.convert(), out.size.convert(), fd)
                    }
                }
            }
            return out
        } finally {
            fclose(fd)
        }

    }
}
