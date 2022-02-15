package com.soywiz.krypto

import com.soywiz.krypto.internal.arraycopy
import kotlin.random.Random

enum class Padding {
    NoPadding,
    PKCS7Padding,
    ANSIX923Padding,
    ISO10126Padding,
    ZeroPadding;

    companion object {
        fun padding(data: ByteArray, blockSize: Int, padding: Padding): ByteArray {
            val paddingSize = if (padding == NoPadding) 0 else blockSize - data.size % blockSize
            val result = ByteArray(data.size + paddingSize)
            arraycopy(data, 0, result, 0, data.size)
            return when (padding) {
                NoPadding -> {
                    result
                }
                PKCS7Padding -> {
                    for (i in data.size until result.size) {
                        result[i] = paddingSize.toByte()
                    }
                    result
                }
                ANSIX923Padding -> {
                    result[result.size - 1] = paddingSize.toByte()
                    result
                }
                ISO10126Padding -> {
                    val randomBytes = Random.nextBytes(paddingSize)
                    randomBytes[paddingSize - 1] = paddingSize.toByte()
                    arraycopy(randomBytes, 0, result, data.size, randomBytes.size)
                    result
                }
                ZeroPadding -> {
                    result
                }
            }
        }

        fun removePadding(data: ByteArray, padding: Padding): ByteArray {
            return when (padding) {
                NoPadding -> {
                    data
                }
                PKCS7Padding, ANSIX923Padding, ISO10126Padding -> {
                    val paddingSize = data[data.size - 1].toInt() and 0xff
                    val result = ByteArray(data.size - paddingSize)
                    arraycopy(data, 0, result, 0, result.size)
                    result
                }
                ZeroPadding -> {
                    var paddingSize = 0
                    for (i in data.size - 1 downTo 0) {
                        if (data[i].toInt() != 0) {
                            break
                        }
                        ++paddingSize
                    }
                    val result = ByteArray(data.size - paddingSize)
                    arraycopy(data, 0, result, 0, result.size)
                    result
                }
            }
        }
    }
}
