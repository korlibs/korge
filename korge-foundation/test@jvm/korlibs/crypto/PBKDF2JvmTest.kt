package korlibs.crypto

import org.junit.Test
import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.test.assertEquals

class PBKDF2JvmTest {
    companion object {
        //const val DEFAULT_ITERATION_COUNT = 4096
        const val DEFAULT_ITERATION_COUNT = 128
    }

    fun generateJVM(
        kind: HasherFactory, password: ByteArray, keyLength: Int, iterationCount: Int, salt: ByteArray
    ): Hash {
        return Hash(SecretKeyFactory.getInstance("PBKDF2WithHmac${kind.name}").generateSecret(
            PBEKeySpec(password.map { it.toInt().toChar() }.toCharArray(), salt, iterationCount, keyLength)
        ).encoded)
    }

    fun generateKrypto(
        kind: HasherFactory, password: ByteArray, keyLength: Int, iterationCount: Int, salt: ByteArray
    ): Hash {
        return PBKDF2.pbkdf2(password, salt, iterationCount, keyLength, kind())
    }

    fun checkAlgo(
        kind: HasherFactory,
        password: ByteArray = byteArrayOf(65, 66, 67, 68, 69, 70, 71, 72),
        keyLength: Int = 256,
        iterationCount: Int = DEFAULT_ITERATION_COUNT,
        salt: ByteArray = ByteArray(12) { (it + 1).toByte() }
    ) {
        val jvm = generateJVM(kind, password, keyLength, iterationCount, salt)
        val krypto = generateKrypto(kind, password, keyLength, iterationCount, salt)
        //println("${kind.name}: ${password.decodeToString()}, $krypto")
        assertEquals(jvm, krypto)
    }

    @Test
    fun pbkdf2WithHmacSHA1() {
        checkAlgo(SHA1)
        checkAlgo(SHA1, password = "password".encodeToByteArray())
    }

    @Test
    fun pbkdf2WithHmacSHA256() {
        checkAlgo(SHA256)
        checkAlgo(SHA256, password = "password".encodeToByteArray())
    }

    @Test
    fun pbkdf2WithHmacSHA512() {
        checkAlgo(SHA512)
        checkAlgo(SHA512, password = "password".encodeToByteArray())
    }

    @Test
    fun pbkdf2WithHmacJavaSHA1() {
        checkAlgo(JavaSHA.Factory(1))
        checkAlgo(JavaSHA.Factory(1), password = "password".encodeToByteArray())
    }

    @Test
    fun pbkdf2WithHmacJavaSHA256() {
        checkAlgo(JavaSHA.Factory(256))
        checkAlgo(JavaSHA.Factory(256), password = "password".encodeToByteArray())
    }

    @Test
    fun pbkdf2WithHmacJavaSHA512() {
        checkAlgo(JavaSHA.Factory(512))
        checkAlgo(JavaSHA.Factory(512), password = "password".encodeToByteArray())
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
