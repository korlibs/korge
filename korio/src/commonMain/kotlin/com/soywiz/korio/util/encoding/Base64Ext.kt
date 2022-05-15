package com.soywiz.korio.util.encoding

import com.soywiz.korio.lang.Charset
import com.soywiz.korio.lang.toByteArray
import com.soywiz.krypto.encoding.Base64

fun Base64.encode(src: String, charset: Charset): String = encode(src.toByteArray(charset))
