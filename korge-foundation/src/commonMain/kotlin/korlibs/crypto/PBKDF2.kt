package korlibs.crypto

import korlibs.crypto.internal.arraycopy

class PBKDF2 {
    companion object {
        fun pbkdf2WithHmacSHA1(password: ByteArray, salt: ByteArray, iterationCount: Int, keySizeInBits: Int): Hash =
            pbkdf2(password, salt, iterationCount, keySizeInBits, SHA1())

        fun pbkdf2WithHmacSHA256(password: ByteArray, salt: ByteArray, iterationCount: Int, keySizeInBits: Int): Hash =
            pbkdf2(password, salt, iterationCount, keySizeInBits, SHA256())

        fun pbkdf2WithHmacSHA512(password: ByteArray, salt: ByteArray, iterationCount: Int, keySizeInBits: Int): Hash =
            pbkdf2(password, salt, iterationCount, keySizeInBits, SHA512())

        private fun Int.toByteArray(out: ByteArray = ByteArray(4)): ByteArray {
            out[0] = (this shr 24 and 0xff).toByte()
            out[1] = (this shr 16 and 0xff).toByte()
            out[2] = (this shr 8 and 0xff).toByte()
            out[3] = (this and 0xff).toByte()
            return out
        }

        fun pbkdf2(password: ByteArray, salt: ByteArray, iterationCount: Int, keySizeInBits: Int, hasher: Hasher): Hash {
            val hLen = hasher.digestSize
            val blockSize = keySizeInBits / hLen
            val outSize = keySizeInBits / 8
            var offset = 0
            val result = ByteArray(outSize)
            val t = ByteArray(hLen)
            val i32be = ByteArray(4)
            val uv = ByteArray(salt.size + i32be.size)
            gen@ for (i in 1 .. blockSize) {
                t.fill(0)
                i.toByteArray(i32be)
                arraycopy(salt, 0, uv, 0, salt.size)
                arraycopy(i32be, 0, uv, salt.size, i32be.size)
                var u = uv
                for (c in 1 .. iterationCount) {
                    u = HMAC.hmac(password, u, hasher).bytes
                    hasher.reset()
                    for (m in u.indices) {
                        t[m] = (t[m].toInt() xor u[m].toInt()).toByte()
                    }
                }
                for (b in t) {
                    result[offset++] = b
                    if (offset >= outSize) {
                        break@gen
                    }
                }
            }
            return Hash(result)
        }
    }
}
