package com.soywiz.krypto

import com.soywiz.krypto.internal.arraycopy

class HMAC {

    companion object {
        fun hmacSHA1(key: ByteArray, data: ByteArray): Hash = hmac(key, data, SHA1())

        fun hmacSHA256(key: ByteArray, data: ByteArray): Hash = hmac(key, data, SHA256())

        fun hmacMD5(key: ByteArray, data: ByteArray): Hash = hmac(key, data, MD5())

        internal fun hmac(key: ByteArray, data: ByteArray, hasher: Hasher): Hash {
            var key = key
            val blockSize = hasher.chunkSize
            if (key.size > blockSize) {
                hasher.reset()
                hasher.update(key)
                key = hasher.digest().bytes
            }
            if (key.size < blockSize) {
                val newKey = ByteArray(blockSize)
                arraycopy(key, 0, newKey, 0, key.size)
                key = newKey
            }

            val oKeyPad = ByteArray(blockSize) { (0x5c xor key[it].toInt()).toByte() }
            val iKeyPad = ByteArray(blockSize) { (0x36 xor key[it].toInt()).toByte() }

            hasher.reset()
            hasher.update(iKeyPad)
            hasher.update(data)
            val h1 = hasher.digest().bytes

            hasher.reset()
            hasher.update(oKeyPad)
            hasher.update(h1)
            return hasher.digest()
        }
    }
}
