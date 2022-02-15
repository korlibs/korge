package com.soywiz.krypto

import com.soywiz.krypto.encoding.Hex

fun ByteArray.toHexString() = Hex.encode(this)
fun ByteArray.toHexStringLower() = Hex.encodeLower(this)
fun ByteArray.toHexStringUpper() = Hex.encodeUpper(this)
val ByteArray.hex: String get() = toHexStringLower()
