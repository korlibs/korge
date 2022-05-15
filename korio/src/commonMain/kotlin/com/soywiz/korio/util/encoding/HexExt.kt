package com.soywiz.korio.util.encoding

import com.soywiz.kmem.ByteArrayBuilder
import com.soywiz.krypto.encoding.Hex

fun Hex.decode(src: String, dst: ByteArrayBuilder) = decode(src) { n, byte -> dst.append(byte) }
