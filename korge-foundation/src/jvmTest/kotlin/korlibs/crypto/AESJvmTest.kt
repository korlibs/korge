package korlibs.crypto

import korlibs.encoding.*
import javax.crypto.spec.*
import kotlin.test.*

class AESJvmTest {
    val MODES = listOf(
        CipherMode.ECB,
        CipherMode.CBC,
        CipherMode.PCBC,
        CipherMode.CFB,
        CipherMode.OFB,
        CipherMode.CTR,
    )

    @Test
    fun testSimple() {
        val key = SecureRandom.nextBytes(16)
        val iv = SecureRandom.nextBytes(16)
        val plain = ByteArray(32) { it.toByte() }
        val encrypted = encryptJava("AES", "CTR", "NoPadding", plain, key, iv)
        val decrypted = decryptJava("AES", "CTR", "NoPadding", encrypted, key, iv)
        assertEquals(plain.hex, decrypted.hex)
    }

    @Test
    fun testCompareOurImpl() {
        val key = ByteArray(16) { (it * 3).toByte() }
        val iv = ByteArray(16) { (it * 7).toByte() }
        val ivCopy = iv.copyOf()
        val plain = ByteArray(32) { it.toByte() }
        val plainCopy = plain.copyOf()

        for (mode in MODES) {
            val crypt = AES(key)[mode, CipherPadding.NoPadding, iv]
            val algo = "AES"
            val padding = "NoPadding"
            val encryptedJava = encryptJava(algo, mode.name, padding, plain, key, iv)
            val encryptedOur = crypt.encrypt(plain)
            val encryptedCopy = encryptedOur.copyOf()

            val decryptedJava = decryptJava(algo, mode.name, padding, encryptedJava, key, iv)
            val decryptedOur = crypt.decrypt(encryptedOur)

            // No mutating inputs
            assertEquals(encryptedCopy.hex, encryptedOur.hex, "encrypted shouldn't be modified")
            assertEquals(plainCopy.hex, plain.hex, "plain shouldn't be modified")
            assertEquals(ivCopy.hex, iv.hex, "iv shouldn't be modified")

            assertEquals(encryptedJava.hex, encryptedOur.hex, "Failed encrypted java-our ${mode.name}")
            assertEquals(decryptedJava.hex, decryptedOur.hex, "Failed decrypted java-our ${mode.name}")
            assertEquals(plain.hex, decryptedOur.hex, "Failed plain-decrypted ${mode.name}")
        }
    }

    private fun encryptJava(algo: String, mode: String, padding: String, data: ByteArray, key: ByteArray = SecureRandom.nextBytes(16), iv: ByteArray = SecureRandom.nextBytes(16)): ByteArray {
        return encryptDecryptJava(javax.crypto.Cipher.ENCRYPT_MODE, algo, mode, padding, data, key, iv)
    }

    private fun decryptJava(algo: String, mode: String, padding: String, data: ByteArray, key: ByteArray = SecureRandom.nextBytes(16), iv: ByteArray = SecureRandom.nextBytes(16)): ByteArray {
        return encryptDecryptJava(javax.crypto.Cipher.DECRYPT_MODE, algo, mode, padding, data, key, iv)
    }

    private fun encryptDecryptJava(emode: Int, algo: String, mode: String, padding: String, data: ByteArray, key: ByteArray = SecureRandom.nextBytes(16), iv: ByteArray = SecureRandom.nextBytes(16)): ByteArray {
        return javax.crypto.Cipher.getInstance("$algo/$mode/$padding").let { cipher ->
            val keySpec = SecretKeySpec(key, algo)
            if (mode == "ECB") {
                cipher.init(emode, keySpec)
            } else {
                cipher.init(emode, keySpec, IvParameterSpec(iv))
            }
            cipher.doFinal(data)
        }
    }
}
