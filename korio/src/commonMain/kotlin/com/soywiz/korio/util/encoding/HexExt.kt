package com.soywiz.korio.util.encoding

import com.soywiz.kmem.*
import com.soywiz.krypto.encoding.*

fun Hex.decode(src: String, dst: ByteArrayBuilder) = decode(src) { n, byte -> dst.append(byte) }
