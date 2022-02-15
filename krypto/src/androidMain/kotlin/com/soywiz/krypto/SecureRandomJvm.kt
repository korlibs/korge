package com.soywiz.krypto

import java.security.SecureRandom

private val jrandom by lazy {
    if (!randomUnittesting) {
        PRNGFixes.apply()
    }
    SecureRandom()
}

actual fun fillRandomBytes(array: ByteArray) {
    jrandom.nextBytes(array)
}

