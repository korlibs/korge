package korlibs.crypto

import kotlin.test.Test
import kotlin.test.assertEquals

class PBKDF2Test {
    @Test
    fun pbkdf2WithHmacSHA1() {
        val password = byteArrayOf(65, 66, 67, 68, 69, 70, 71, 72) // [A, B, C, D, E, F, G, H]
        val salt = ByteArray(12){(it + 1).toByte()}
        val iterationCount = 128
        val keyLength = 256
        assertEquals(
            "506a8102a74cc4732cfbce262f07e09ff6a36b9ae98dc1491302a8b6dfbf9e00",
            PBKDF2.pbkdf2WithHmacSHA1(password, salt, iterationCount, keyLength).hex
        )
    }

    @Test
    fun pbkdf2WithHmacSHA256() {
        val password = byteArrayOf(65, 66, 67, 68, 69, 70, 71, 72) // [A, B, C, D, E, F, G, H]
        val salt = ByteArray(12){(it + 1).toByte()}
        val iterationCount = 128
        val keyLength = 256
        assertEquals(
            "c8b80edacfe754d8fceaa97a4756818aebded276ce832ee8fc39d2e8fd2f6d8f",
            PBKDF2.pbkdf2WithHmacSHA256(password, salt, iterationCount, keyLength).hex
        )
    }

    @Test
    fun pbkdf2WithHmacSHA512() {
        val password = "password".encodeToByteArray()
        val salt = ByteArray(12) { (it + 1).toByte() }
        val iterationCount = 128
        val keyLength = 256
        assertEquals(
            "846a7cc6612ba20651e4dd8513b2e71e3e45d983561d9a5123b17d42df5aeabe",
            PBKDF2.pbkdf2WithHmacSHA512(password, salt, iterationCount, keyLength).hex,
        )
    }
}
