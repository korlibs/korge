package com.soywiz.korge.native

import com.soywiz.korio.*
import kotlinx.cinterop.*
import platform.posix.*

object KorgeSimpleNativeSyncIO {
    fun mkdirs(file: String) {
        com.soywiz.korio.doMkdir(file, "0777".toInt(8))
    }

    fun writeBytes(file: String, bytes: ByteArray) {
        val fd = posixFopen(file, "wb") ?: error("Can't open file '$file' for writing")
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
        val fd = posixFopen(file, "rb") ?: error("Can't open file '$file' for reading")
        try {
            fseek(fd, 0L.convert(), SEEK_END)
            val fileSize = ftell(fd)
            fseek(fd, 0L.convert(), SEEK_SET)

            val out = ByteArray(fileSize.toInt())
            if (out.isNotEmpty()) {
                memScoped {
                    out.usePinned { pin ->
                        @Suppress("UNUSED_VARIABLE")
                        val readCount = fread(pin.addressOf(0), 1.convert(), out.size.convert(), fd)
                        //println("readCount: $readCount, out.size=${out.size}, fileSize=$fileSize")
                    }
                }
            }
            return out
        } finally {
            fclose(fd)
        }
    }
}
