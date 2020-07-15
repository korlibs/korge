package com.soywiz.krypto

import java.security.SecureRandom

private val jrandom = SecureRandom()

actual fun fillRandomBytes(array: ByteArray) {
    jrandom.nextBytes(array)
}
