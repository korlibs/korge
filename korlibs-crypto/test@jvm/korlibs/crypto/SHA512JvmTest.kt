package korlibs.crypto

import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals


class SHA512JvmTest {
    @Test
    fun test() {
        for (n in 0 until 1033) {
            compareHash(ByteArray(n) { n.toByte() })
        }
    }

    private fun compareHash(data: ByteArray) {
        assertEquals(data.sha512jvm(), data.sha512())
    }

    private fun ByteArray.sha512jvm(): Hash {
        val digest: MessageDigest = MessageDigest.getInstance("SHA-512")
        digest.reset()
        digest.update(this)
        return Hash(digest.digest())
    }
}
