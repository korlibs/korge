package korlibs.crypto

import korlibs.encoding.Hex
import korlibs.encoding.hexLower
import kotlin.test.Test
import kotlin.test.assertEquals

class CipherTest {
    @Test
    fun testAes() {
        val plainText = Hex.decode("00112233445566778899aabbccddeeff")
        val cipherKey = Hex.decode("000102030405060708090a0b0c0d0e0f")
        val cipherText = Hex.decode("69c4e0d86a7b0430d8cdb78070b4c55a")
        val encryptor = AES(cipherKey)[CipherMode.ECB, Padding.NoPadding]
        assertEquals(cipherText.hexLower, encryptor.encrypt(plainText).hexLower)
    }
}
