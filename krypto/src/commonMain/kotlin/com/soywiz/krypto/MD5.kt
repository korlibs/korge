package com.soywiz.krypto

import com.soywiz.krypto.internal.rotateLeft
import kotlin.math.abs
import kotlin.math.sin

class MD5 : Hasher(chunkSize = 64, digestSize = 16) {
    companion object : HasherFactory({ MD5() }) {
        private val S = intArrayOf(7, 12, 17, 22, 5, 9, 14, 20, 4, 11, 16, 23, 6, 10, 15, 21)
        private val T = IntArray(64) { ((1L shl 32) * abs(sin(1.0 + it))).toLong().toInt() }
    }

    private val r = IntArray(4)
    private val o = IntArray(4)
    private val b = IntArray(16)

    init {
        coreReset()
    }

    override fun coreReset() {
        r[0] = 0x67452301
        r[1] = 0xEFCDAB89.toInt()
        r[2] = 0x98BADCFE.toInt()
        r[3] = 0x10325476
    }

    override fun coreUpdate(chunk: ByteArray) {
        for (j in 0 until 64) b[j ushr 2] = (chunk[j].toInt() shl 24) or (b[j ushr 2] ushr 8)
        for (j in 0 until 4) o[j] = r[j]
        for (j in 0 until 64) {
            val d16 = j / 16
            val f = when (d16) {
                0 -> (r[1] and r[2]) or (r[1].inv() and r[3])
                1 -> (r[1] and r[3]) or (r[2] and r[3].inv())
                2 -> r[1] xor r[2] xor r[3]
                3 -> r[2] xor (r[1] or r[3].inv())
                else -> 0
            }
            val bi = when (d16) {
                0 -> j
                1 -> (j * 5 + 1) and 0x0F
                2 -> (j * 3 + 5) and 0x0F
                3 -> (j * 7) and 0x0F
                else -> 0
            }
            val temp = r[1] + (r[0] + f + b[bi] + T[j]).rotateLeft(S[(d16 shl 2) or (j and 3)])
            r[0] = r[3]
            r[3] = r[2]
            r[2] = r[1]
            r[1] = temp
        }
        for (j in 0 until 4) r[j] += o[j]
    }

    override fun corePadding(totalWritten: Long): ByteArray {
        val numberOfBlocks = ((totalWritten + 8) / chunkSize) + 1
        val totalWrittenBits = totalWritten * 8
        return ByteArray(((numberOfBlocks * chunkSize) - totalWritten).toInt()).apply {
            this[0] = 0x80.toByte()
            for (i in 0 until 8) this[this.size - 8 + i] = (totalWrittenBits ushr (8 * i)).toByte()
        }
    }

    override fun coreDigest(out: ByteArray) {
        for (it in 0 until 16) out[it] = (r[it / 4] ushr ((it % 4) * 8)).toByte()
    }
}

fun ByteArray.md5() = hash(MD5)
