package korlibs.crypto

import korlibs.crypto.internal.arraycopy

// https://git.suckless.org/sbase/file/libutil/sha512.c.html
// https://git.suckless.org/sbase/file/sha512.h.html
/* public domain sha512 implementation based on fips180-3 */
@OptIn(ExperimentalUnsignedTypes::class)
class SHA512 : SHA(chunkSize = 128, digestSize = 64) {
    companion object : HasherFactory("SHA512", { SHA512() }) {
        private const val SHA512_DIGEST_LENGTH = 64

        private fun ror(n: ULong, k: Int): ULong = (n shr k) or (n shl (64 - k))
        private fun Ch(x: ULong, y: ULong, z: ULong) = (z xor (x and (y xor z)))
        private fun Maj(x: ULong, y: ULong, z: ULong) = ((x and y) or (z and (x or y)))
        private fun S0(x: ULong) = (ror(x, 28) xor ror(x, 34) xor ror(x, 39))
        private fun S1(x: ULong) = (ror(x, 14) xor ror(x, 18) xor ror(x, 41))
        private fun R0(x: ULong) = (ror(x, 1) xor ror(x, 8) xor (x shr 7))
        private fun R1(x: ULong) = (ror(x, 19) xor ror(x, 61) xor (x shr 6))

        private val K = ulongArrayOf(
            0x428a2f98d728ae22uL, 0x7137449123ef65cduL, 0xb5c0fbcfec4d3b2fuL, 0xe9b5dba58189dbbcuL,
            0x3956c25bf348b538uL, 0x59f111f1b605d019uL, 0x923f82a4af194f9buL, 0xab1c5ed5da6d8118uL,
            0xd807aa98a3030242uL, 0x12835b0145706fbeuL, 0x243185be4ee4b28cuL, 0x550c7dc3d5ffb4e2uL,
            0x72be5d74f27b896fuL, 0x80deb1fe3b1696b1uL, 0x9bdc06a725c71235uL, 0xc19bf174cf692694uL,
            0xe49b69c19ef14ad2uL, 0xefbe4786384f25e3uL, 0x0fc19dc68b8cd5b5uL, 0x240ca1cc77ac9c65uL,
            0x2de92c6f592b0275uL, 0x4a7484aa6ea6e483uL, 0x5cb0a9dcbd41fbd4uL, 0x76f988da831153b5uL,
            0x983e5152ee66dfabuL, 0xa831c66d2db43210uL, 0xb00327c898fb213fuL, 0xbf597fc7beef0ee4uL,
            0xc6e00bf33da88fc2uL, 0xd5a79147930aa725uL, 0x06ca6351e003826fuL, 0x142929670a0e6e70uL,
            0x27b70a8546d22ffcuL, 0x2e1b21385c26c926uL, 0x4d2c6dfc5ac42aeduL, 0x53380d139d95b3dfuL,
            0x650a73548baf63deuL, 0x766a0abb3c77b2a8uL, 0x81c2c92e47edaee6uL, 0x92722c851482353buL,
            0xa2bfe8a14cf10364uL, 0xa81a664bbc423001uL, 0xc24b8b70d0f89791uL, 0xc76c51a30654be30uL,
            0xd192e819d6ef5218uL, 0xd69906245565a910uL, 0xf40e35855771202auL, 0x106aa07032bbd1b8uL,
            0x19a4c116b8d2d0c8uL, 0x1e376c085141ab53uL, 0x2748774cdf8eeb99uL, 0x34b0bcb5e19b48a8uL,
            0x391c0cb3c5c95a63uL, 0x4ed8aa4ae3418acbuL, 0x5b9cca4f7763e373uL, 0x682e6ff3d6b2b8a3uL,
            0x748f82ee5defb2fcuL, 0x78a5636f43172f60uL, 0x84c87814a1f0ab72uL, 0x8cc702081a6439ecuL,
            0x90befffa23631e28uL, 0xa4506cebde82bde9uL, 0xbef9a3f7b2c67915uL, 0xc67178f2e372532buL,
            0xca273eceea26619cuL, 0xd186b8c721c0c207uL, 0xeada7dd6cde0eb1euL, 0xf57d4f7fee6ed178uL,
            0x06f067aa72176fbauL, 0x0a637dc5a2c898a6uL, 0x113f9804bef90daeuL, 0x1b710b35131c471buL,
            0x28db77f523047d84uL, 0x32caab7b40c72493uL, 0x3c9ebe0a15c9bebcuL, 0x431d67c49c100d4cuL,
            0x4cc5d4becb3e42b6uL, 0x597f299cfc657e2auL, 0x5fcb6fab3ad6faecuL, 0x6c44198c4a475817uL
        )
    }

    var len = 0uL
    val h = ULongArray(8)
    val buf = UByteArray(128)

    init {
        coreReset()
    }

    override fun coreReset() {
        len = 0uL
        h[0] = 0x6a09e667f3bcc908uL
        h[1] = 0xbb67ae8584caa73buL
        h[2] = 0x3c6ef372fe94f82buL
        h[3] = 0xa54ff53a5f1d36f1uL
        h[4] = 0x510e527fade682d1uL
        h[5] = 0x9b05688c2b3e6c1fuL
        h[6] = 0x1f83d9abfb41bd6buL
        h[7] = 0x5be0cd19137e2179uL
        buf.fill(0u)
    }

    // @TODO: The super update doesn't work with SHA512, do we have to fix something?
    override fun update(data: ByteArray, offset: Int, count: Int): Hasher {
        sha512_update(data.asUByteArray(), offset, count)
        totalWritten += count
        return this
    }

    override fun digestOut(out: ByteArray) {
        sha512_sum(out.asUByteArray())
        reset()
    }

    override fun coreUpdate(chunk: ByteArray) {
        sha512_update(chunk.asUByteArray(), 0, chunk.size)
    }

    override fun coreDigest(out: ByteArray) {
        sha512_sum(out.asUByteArray())
    }

    private val W = ULongArray(80)

    private fun processblock(buf: UByteArray, p: Int = 0) {
        for (i in 0 until 16) {
            var v = 0uL
            v = v or (buf[p + 8 * i + 0].toULong() shl 56)
            v = v or (buf[p + 8 * i + 1].toULong() shl 48)
            v = v or (buf[p + 8 * i + 2].toULong() shl 40)
            v = v or (buf[p + 8 * i + 3].toULong() shl 32)
            v = v or (buf[p + 8 * i + 4].toULong() shl 24)
            v = v or (buf[p + 8 * i + 5].toULong() shl 16)
            v = v or (buf[p + 8 * i + 6].toULong() shl 8)
            v = v or (buf[p + 8 * i + 7].toULong() shl 0)
            W[i] = v
        }
        for (i in 16 until 80) {
            W[i] = R1(W[i - 2]) + W[i - 7] + R0(W[i - 15]) + W[i - 16]
        }
        var a = this.h[0]
        var b = this.h[1]
        var c = this.h[2]
        var d = this.h[3]
        var e = this.h[4]
        var f = this.h[5]
        var g = this.h[6]
        var h = this.h[7]
        for (i in 0 until 80) {
            val t1 = h + S1(e) + Ch(e, f, g) + K[i] + W[i]
            val t2 = S0(a) + Maj(a, b, c)
            h = g
            g = f
            f = e
            e = d + t1
            d = c
            c = b
            b = a
            a = t1 + t2
        }
        this.h[0] += a
        this.h[1] += b
        this.h[2] += c
        this.h[3] += d
        this.h[4] += e
        this.h[5] += f
        this.h[6] += g
        this.h[7] += h
    }

    private fun pad() {
        var r = (this.len % 128uL).toInt()

        this.buf[r++] = 0x80u
        if (r > 112) {
            this.buf.fill(0u, r, 128)
            r = 0
            processblock(buf)
        }
        this.buf.fill(0u, r, 120)
        this.len *= 8uL
        this.buf[120] = (this.len shr 56).toUByte()
        this.buf[121] = (this.len shr 48).toUByte()
        this.buf[122] = (this.len shr 40).toUByte()
        this.buf[123] = (this.len shr 32).toUByte()
        this.buf[124] = (this.len shr 24).toUByte()
        this.buf[125] = (this.len shr 16).toUByte()
        this.buf[126] = (this.len shr 8).toUByte()
        this.buf[127] = (this.len shr 0).toUByte()
        processblock(this.buf)
    }

    private fun sha512_sum(md: UByteArray) {
        sha512_sum_n(md, 8)
    }

    private fun sha512_sum_n(md: UByteArray, n: Int) {
        pad()
        for (i in 0 until n) {
            md[8 * i + 0] = (h[i] shr 56).toUByte()
            md[8 * i + 1] = (h[i] shr 48).toUByte()
            md[8 * i + 2] = (h[i] shr 40).toUByte()
            md[8 * i + 3] = (h[i] shr 32).toUByte()
            md[8 * i + 4] = (h[i] shr 24).toUByte()
            md[8 * i + 5] = (h[i] shr 16).toUByte()
            md[8 * i + 6] = (h[i] shr 8).toUByte()
            md[8 * i + 7] = (h[i] shr 0).toUByte()
        }
    }

    private fun sha512_update(m: UByteArray, p: Int, len: Int) {
        var len = len
        var p = p
        val r = (this.len % 128uL).toInt()

        this.len += len.toULong()
        if (r != 0) {
            if (len < 128 - r) {
                arraycopy(m.asByteArray(), p, this.buf.asByteArray(), r, len)
                return
            }
            arraycopy(m.asByteArray(), p, buf.asByteArray(), r, 128 - r)
            len -= 128 - r
            p += 128 - r
            processblock(this.buf)
        }
        while (len >= 128) {
            processblock(m, p)
            len -= 128
            p += 128
        }
        arraycopy(m.asByteArray(), p, buf.asByteArray(), 0, len)
    }

}

fun ByteArray.sha512() = hash(SHA512)
