package com.soywiz.krypto

import kotlin.test.Test
import kotlin.test.assertEquals

class PBKDF2Test {
    @Test
    fun pbkdf2WithHmacSHA1() {
        val password = byteArrayOf(65, 66, 67, 68, 69, 70, 71, 72) // [A, B, C, D, E, F, G, H]
        val salt = ByteArray(12){(it + 1).toByte()}
        val iterationCount = 4096
        val keyLength = 256
        val v = PBKDF2.pbkdf2WithHmacSHA1(password, salt, iterationCount, keyLength)
        //println(v.contentToString())
        //println(v.hex)
    }

    @Test
    fun pbkdf2WithHmacSHA256() {
        val password = byteArrayOf(65, 66, 67, 68, 69, 70, 71, 72) // [A, B, C, D, E, F, G, H]
        val salt = ByteArray(12){(it + 1).toByte()}
        val iterationCount = 4096
        val keyLength = 256
        val v = PBKDF2.pbkdf2WithHmacSHA256(password, salt, iterationCount, keyLength)
        assertEquals("151e360d1a6d085395d6a79473edfbf3dbdbdc6ffadf2b27a255d87f7bc4d2d1", v.hex)
    }
}
