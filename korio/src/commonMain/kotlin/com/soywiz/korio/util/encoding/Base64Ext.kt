package com.soywiz.korio.util.encoding

import com.soywiz.korio.lang.*
import com.soywiz.krypto.encoding.*

fun Base64.encode(src: String, charset: Charset): String = encode(src.toByteArray(charset))
