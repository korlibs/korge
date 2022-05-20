package com.soywiz.korge.gradle.util

import java.io.*

fun OutputStream.write8(value: Int) {
    write(value)
}

fun OutputStream.write16LE(value: Int) {
    write((value ushr 0) and 0xFF)
    write((value ushr 8) and 0xFF)
}

fun OutputStream.write32LE(value: Int) {
    write((value ushr 0) and 0xFF)
    write((value ushr 8) and 0xFF)
    write((value ushr 16) and 0xFF)
    write((value ushr 24) and 0xFF)
}
fun OutputStream.writeBytes(bytes: ByteArray) {
    write(bytes)
}
