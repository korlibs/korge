package com.soywiz.krypto

import org.junit.Test
import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.test.assertEquals

class PBKDF2JvmTest {
    fun generateJVM(
        kind: HasherFactory,
        password: ByteArray = byteArrayOf(65, 66, 67, 68, 69, 70, 71, 72),
        keyLength: Int = 256,
        iterationCount: Int = 4096,
        salt: ByteArray = ByteArray(12) { (it + 1).toByte() }
    ): Hash {
        return Hash(SecretKeyFactory.getInstance("PBKDF2WithHmac${kind.name}").generateSecret(
            PBEKeySpec(password.map { it.toInt().toChar() }.toCharArray(), salt, iterationCount, keyLength)
        ).encoded)
    }

    fun generateKrypto(
        kind: HasherFactory,
        password: ByteArray = byteArrayOf(65, 66, 67, 68, 69, 70, 71, 72),
        keyLength: Int = 256,
        iterationCount: Int = 4096,
        salt: ByteArray = ByteArray(12) { (it + 1).toByte() }
    ): Hash {
        return PBKDF2.pbkdf2(password, salt, iterationCount, keyLength, kind())
    }

    fun checkAlgo(kind: HasherFactory) {
        assertEquals(generateJVM(kind), generateKrypto(kind))
    }

    @Test
    fun pbkdf2WithHmacSHA1() {
        checkAlgo(SHA1)
    }

    @Test
    fun pbkdf2WithHmacSHA256() {
        checkAlgo(SHA256)
    }

    @Test
    fun pbkdf2WithHmacSHA512() {
        checkAlgo(SHA512)
    }

    @Test
    fun pbkdf2WithHmacJavaSHA1() {
        checkAlgo(JavaSHA.Factory(1))
    }

    @Test
    fun pbkdf2WithHmacJavaSHA512() {
        checkAlgo(JavaSHA.Factory(512))
    }

    @Test
    fun pbkdf2WithHmacJavaSHA256() {
        checkAlgo(JavaSHA.Factory(256))
    }

    class JavaSHA(
        val size: Int,
        val md: MessageDigest = MessageDigest.getInstance("SHA-$size")
    ) : NonCoreHasher(if (size == 512) 128 else 64, md.digestLength, "SHA$size") {
        class Factory(size: Int) : HasherFactory("SHA$size", { JavaSHA(size) })

        override fun reset(): Hasher {
            md.reset()
            return this
        }

        override fun update(data: ByteArray, offset: Int, count: Int): Hasher {
            md.update(data, offset, count)
            return this
        }

        override fun digestOut(out: ByteArray) {
            md.digest().copyInto(out, 0, 0, out.size)
        }
    }
}
