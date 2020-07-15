package com.soywiz.krypto

import com.soywiz.krypto.internal.arraycopy

class PBKDF2 {
    companion object {
        fun pbkdf2WithHmacSHA1(password: ByteArray, salt: ByteArray, iterationCount: Int, keySizeInBits: Int) =
            pbkdf2(password, salt, iterationCount, keySizeInBits, SHA1())

        fun pbkdf2WithHmacSHA256(password: ByteArray, salt: ByteArray, iterationCount: Int, keySizeInBits: Int) =
            pbkdf2(password, salt, iterationCount, keySizeInBits, SHA256())

        private fun Int.toByteArray(): ByteArray {
            return byteArrayOf(
                (this shr 24 and 0xff).toByte(),
                (this shr 16 and 0xff).toByte(),
                (this shr 8 and 0xff).toByte(),
                (this and 0xff).toByte()
            )
        }

        private fun pbkdf2(password: ByteArray, salt: ByteArray, iterationCount: Int, keySizeInBits: Int, hasher: Hasher): ByteArray {
            val hLen = hasher.digestSize
            val blockSize = keySizeInBits / hLen
            val outSize = keySizeInBits / 8
            val result = ByteArray(outSize)
            var offset = 0
            val t = ByteArray(hLen)
            gen@ for (i in 1 .. blockSize) {
                t.fill(0)
                val i32be = i.toByteArray()
                var u = ByteArray(salt.size + i32be.size)
                arraycopy(salt, 0, u, 0, salt.size)
                arraycopy(i32be, 0, u, salt.size, i32be.size)
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
            return result
        }
    }
}
