package com.soywiz.kproject.util

fun ByteArray.hex(): String {
    val hex = "0123456789abcdef"
    val out = StringBuilder(this.size * 2)
    for (n in 0 until size) {
        val byte = this[n]
        out.append(hex[(byte.toInt() ushr 8) and 0xF])
        out.append(hex[(byte.toInt() ushr 0) and 0xF])
    }
    return out.toString()
}
