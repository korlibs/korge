package com.soywiz.krypto

import com.soywiz.krypto.internal.rotateLeft

class MD4 : Hasher(chunkSize = 64, digestSize = 16) {

    companion object : HasherFactory({ MD4() }) {
        private val S = intArrayOf(3, 7, 11, 19, 3, 5, 9, 13, 3, 9, 11, 15)
        private val R3 = listOf(0, 2, 1, 3)
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
        for (j in 0 until 48) {
            val d16 = j / 16
            val f = when (d16) {
                0 -> (r[1] and r[2]) or (r[1].inv() and r[3])
                1 -> (r[1] and r[2]) or (r[1] and r[3]) or (r[2] and r[3])
                2 -> r[1] xor r[2] xor r[3]
                else -> 0
            }

            val bi = when (d16) {
                0 -> j
                1 -> j % 16 / 4 + j * 4 % 16
                2 -> R3[j % 16 / 4] + 4 * R3[j % 4]
                else -> 0
            }
            val t = when (d16) {
                0 -> 0
                1 -> 0x5a827999
                2 -> 0x6ed9eba1
                else -> 0
            }
            val temp = (r[0] + f + b[bi] + t).rotateLeft(S[(d16 shl 2) or (j and 3)])
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

fun ByteArray.md4() = hash(MD4)
