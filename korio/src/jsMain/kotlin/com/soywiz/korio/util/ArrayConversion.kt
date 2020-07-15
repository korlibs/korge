package com.soywiz.korio.util

import org.khronos.webgl.*

fun ArrayBuffer.toByteArray(): ByteArray = Int8Array(this).toByteArray()
fun Int8Array.toByteArray(): ByteArray {
    val tout = this.asDynamic()
    return if (tout is ByteArray) {
        tout.unsafeCast<ByteArray>()
    } else {
        val out = ByteArray(this.length)
        for (n in out.indices) out[n] = this[n]
        out
    }
}

fun ByteArray.toInt8Array(): Int8Array {
    val tout = this.asDynamic()
    return if (tout is Int8Array) {
        tout.unsafeCast<Int8Array>()
    } else {
        val out = Int8Array(this.size)
        for (n in 0 until out.length) out[n] = this[n]
        out
    }
}
