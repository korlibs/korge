package com.soywiz.krypto

import com.soywiz.krypto.internal.arraycopy
import com.soywiz.krypto.internal.readS32_be
import com.soywiz.krypto.internal.rotateRight

class SHA256 : SHA(chunkSize = 64, digestSize = 32) {
    companion object : HasherFactory({ SHA256() }) {
        private val H = intArrayOf(
            0x6a09e667, -0x4498517b, 0x3c6ef372, -0x5ab00ac6,
            0x510e527f, -0x64fa9774, 0x1f83d9ab, 0x5be0cd19
        )

        private val K = intArrayOf(
            0x428a2f98, 0x71374491, -0x4a3f0431, -0x164a245b,
            0x3956c25b, 0x59f111f1, -0x6dc07d5c, -0x54e3a12b,
            -0x27f85568, 0x12835b01, 0x243185be, 0x550c7dc3,
            0x72be5d74, -0x7f214e02, -0x6423f959, -0x3e640e8c,
            -0x1b64963f, -0x1041b87a, 0x0fc19dc6, 0x240ca1cc,
            0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            -0x67c1aeae, -0x57ce3993, -0x4ffcd838, -0x40a68039,
            -0x391ff40d, -0x2a586eb9, 0x06ca6351, 0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
            0x650a7354, 0x766a0abb, -0x7e3d36d2, -0x6d8dd37b,
            -0x5d40175f, -0x57e599b5, -0x3db47490, -0x3893ae5d,
            -0x2e6d17e7, -0x2966f9dc, -0xbf1ca7b, 0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
            0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
            0x748f82ee, 0x78a5636f, -0x7b3787ec, -0x7338fdf8,
            -0x6f410006, -0x5baf9315, -0x41065c09, -0x398e870e
        )
    }


    private val h = IntArray(8)
    private val r = IntArray(8)
    private val w = IntArray(64)

    init {
        coreReset()
    }

    override fun coreReset(): Unit = run { arraycopy(H, 0, h, 0, 8) }

    override fun coreUpdate(chunk: ByteArray) {
        arraycopy(h, 0, r, 0, 8)

        for (j in 0 until 16) w[j] = chunk.readS32_be(j * 4)
        for (j in 16 until 64) {
            val s0 = w[j - 15].rotateRight(7) xor w[j - 15].rotateRight(18) xor w[j - 15].ushr(3)
            val s1 = w[j - 2].rotateRight(17) xor w[j - 2].rotateRight(19) xor w[j - 2].ushr(10)
            w[j] = w[j - 16] + s0 + w[j - 7] + s1
        }

        for (j in 0 until 64) {
            val s1 = r[4].rotateRight(6) xor r[4].rotateRight(11) xor r[4].rotateRight(25)
            val ch = r[4] and r[5] xor (r[4].inv() and r[6])
            val t1 = r[7] + s1 + ch + K[j] + w[j]
            val s0 = r[0].rotateRight(2) xor r[0].rotateRight(13) xor r[0].rotateRight(22)
            val maj = r[0] and r[1] xor (r[0] and r[2]) xor (r[1] and r[2])
            val t2 = s0 + maj
            r[7] = r[6]
            r[6] = r[5]
            r[5] = r[4]
            r[4] = r[3] + t1
            r[3] = r[2]
            r[2] = r[1]
            r[1] = r[0]
            r[0] = t1 + t2

        }
        for (j in 0 until 8) h[j] += r[j]
    }

    override fun coreDigest(out: ByteArray) {
        for (n in out.indices) out[n] = (h[n / 4] ushr (24 - 8 * (n % 4))).toByte()
    }
}

fun ByteArray.sha256() = hash(SHA256)
