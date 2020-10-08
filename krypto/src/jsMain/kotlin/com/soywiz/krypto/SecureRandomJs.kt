package com.soywiz.krypto

import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array

private val isNodeJs by lazy { js("(typeof process === 'object' && typeof require === 'function')").unsafeCast<Boolean>() }
private external fun require(name: String): dynamic
private val _global: dynamic = js("((typeof global !== 'undefined') ? global : self)")

actual fun fillRandomBytes(array: ByteArray) {
    if (isNodeJs) {
        require("crypto").randomFillSync(Uint8Array(array.unsafeCast<Int8Array>().buffer))
    } else {
        _global.crypto.getRandomValues(array)
    }
}
