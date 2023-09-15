package korlibs.crypto

@Suppress("UNUSED_CHANGED_VALUE")
/**
 * Based on CryptoJS v3.1.2
 * code.google.com/p/crypto-js
 * (c) 2009-2013 by Jeff Mott. All rights reserved.
 * code.google.com/p/crypto-js/wiki/License
 */
class AES(val keyWords: IntArray) : Cipher {
    override val blockSize: Int get() = BLOCK_SIZE

    private val keySize = keyWords.size
    private val numRounds = keySize + 6
    private val ksRows = (numRounds + 1) * 4
    private val keySchedule = IntArray(ksRows).apply {
        for (ksRow in indices) {
            this[ksRow] = when {
                ksRow < keySize -> keyWords[ksRow]
                else -> {
                    var t = this[ksRow - 1]
                    if (0 == (ksRow % keySize)) {
                        t = (t shl 8) or (t ushr 24)
                        t = (SBOX[t.ext8(24)] shl 24) or (SBOX[t.ext8(16)] shl 16) or (SBOX[t.ext8(8)] shl 8) or SBOX[t and 0xff]
                        t = t xor (RCON[(ksRow / keySize) or 0] shl 24)
                    } else if (keySize > 6 && ksRow % keySize == 4) {
                        t = (SBOX[t.ext8(24)] shl 24) or (SBOX[t.ext8(16)] shl 16) or (SBOX[t.ext8(8)] shl 8) or SBOX[t and 0xff]
                    }
                    this[ksRow - keySize] xor t
                }
            }
        }
    }
    private val invKeySchedule = IntArray(ksRows).apply {
        for (invKsRow in indices) {
            val ksRow = ksRows - invKsRow
            val t = if ((invKsRow % 4) != 0) keySchedule[ksRow] else keySchedule[ksRow - 4]
            this[invKsRow] = if (invKsRow < 4 || ksRow <= 4) t else INV_SUB_MIX_0[SBOX[t.ext8(24)]] xor INV_SUB_MIX_1[SBOX[t.ext8(16)]] xor INV_SUB_MIX_2[SBOX[t.ext8(8)]] xor INV_SUB_MIX_3[SBOX[t and 0xff]]
        }
    }

    constructor(key: ByteArray) : this(key.toIntArray())

    override fun encrypt(data: ByteArray, offset: Int, len: Int) {
        for (n in 0 until len step BLOCK_SIZE) encryptBlock(data, offset + n)
    }

    override fun decrypt(data: ByteArray, offset: Int, len: Int) {
        for (n in 0 until len step BLOCK_SIZE) decryptBlock(data, offset + n)
    }

    fun encryptBlock(M: ByteArray, offset: Int) {
        this.doCryptBlock(M, offset, this.keySchedule, SUB_MIX_0, SUB_MIX_1, SUB_MIX_2, SUB_MIX_3, SBOX)
    }

    fun decryptBlock(M: ByteArray, offset: Int) {
        this.doCryptBlock(
            M, offset,
            this.invKeySchedule, INV_SUB_MIX_0, INV_SUB_MIX_1, INV_SUB_MIX_2, INV_SUB_MIX_3, INV_SBOX,
            swap13 = true
        )
    }

    private fun doCryptBlock(
        M: IntArray, offset: Int, keySchedule: IntArray,
        SUB_MIX_0: IntArray, SUB_MIX_1: IntArray, SUB_MIX_2: IntArray, SUB_MIX_3: IntArray, SBOX: IntArray,
        swap13: Boolean = false
    ) {
        doCryptBlockInternal(M, offset, keySchedule, SUB_MIX_0, SUB_MIX_1, SUB_MIX_2, SUB_MIX_3, SBOX, swap13,
            get = { array, o, i -> array[o + i] },
            set = { array, o, i, value -> array[o + i] = value },
        )
    }

    private fun doCryptBlock(
        M: ByteArray, offset: Int, keySchedule: IntArray,
        SUB_MIX_0: IntArray, SUB_MIX_1: IntArray, SUB_MIX_2: IntArray, SUB_MIX_3: IntArray, SBOX: IntArray,
        swap13: Boolean = false
    ) {
        doCryptBlockInternal(M, offset, keySchedule, SUB_MIX_0, SUB_MIX_1, SUB_MIX_2, SUB_MIX_3, SBOX, swap13,
            get = { array, o, i -> array.getInt(o + i * 4) },
            set = { array, o, i, value -> array.setInt(o + i * 4, value) },
        )
    }

    private inline fun <T> doCryptBlockInternal(
        M: T, offset: Int, keySchedule: IntArray,
        SUB_MIX_0: IntArray, SUB_MIX_1: IntArray, SUB_MIX_2: IntArray, SUB_MIX_3: IntArray, SBOX: IntArray,
        swap13: Boolean = false,
        get: (M: T, offset: Int, index: Int) -> Int,
        set: (M: T, offset: Int, index: Int, value: Int) -> Unit,
    ) {
        val O1 = if (!swap13) 1 else 3
        val O3 = if (!swap13) 3 else 1
        var s0 = get(M, offset, 0) xor keySchedule[0]
        var s1 = get(M, offset, O1) xor keySchedule[1]
        var s2 = get(M, offset, 2) xor keySchedule[2]
        var s3 = get(M, offset, O3) xor keySchedule[3]
        var ksRow = 4

        for (round in 1 until numRounds) {
            val t0 = SUB_MIX_0[s0.ext8(24)] xor SUB_MIX_1[s1.ext8(16)] xor SUB_MIX_2[s2.ext8(8)] xor SUB_MIX_3[s3.ext8(0)] xor keySchedule[ksRow++]
            val t1 = SUB_MIX_0[s1.ext8(24)] xor SUB_MIX_1[s2.ext8(16)] xor SUB_MIX_2[s3.ext8(8)] xor SUB_MIX_3[s0.ext8(0)] xor keySchedule[ksRow++]
            val t2 = SUB_MIX_0[s2.ext8(24)] xor SUB_MIX_1[s3.ext8(16)] xor SUB_MIX_2[s0.ext8(8)] xor SUB_MIX_3[s1.ext8(0)] xor keySchedule[ksRow++]
            val t3 = SUB_MIX_0[s3.ext8(24)] xor SUB_MIX_1[s0.ext8(16)] xor SUB_MIX_2[s1.ext8(8)] xor SUB_MIX_3[s2.ext8(0)] xor keySchedule[ksRow++]
            s0 = t0; s1 = t1; s2 = t2; s3 = t3
        }

        val t0 = ((SBOX[s0.ext8(24)] shl 24) or (SBOX[s1.ext8(16)] shl 16) or (SBOX[s2.ext8(8)] shl 8) or SBOX[s3.ext8(0)]) xor keySchedule[ksRow++]
        val t1 = ((SBOX[s1.ext8(24)] shl 24) or (SBOX[s2.ext8(16)] shl 16) or (SBOX[s3.ext8(8)] shl 8) or SBOX[s0.ext8(0)]) xor keySchedule[ksRow++]
        val t2 = ((SBOX[s2.ext8(24)] shl 24) or (SBOX[s3.ext8(16)] shl 16) or (SBOX[s0.ext8(8)] shl 8) or SBOX[s1.ext8(0)]) xor keySchedule[ksRow++]
        val t3 = ((SBOX[s3.ext8(24)] shl 24) or (SBOX[s0.ext8(16)] shl 16) or (SBOX[s1.ext8(8)] shl 8) or SBOX[s2.ext8(0)]) xor keySchedule[ksRow++]

        set(M, offset, 0, t0)
        set(M, offset, O1, t1)
        set(M, offset, 2, t2)
        set(M, offset, O3, t3)
    }

    companion object {
        private val SBOX = IntArray(256)
        private val INV_SBOX = IntArray(256)
        private val SUB_MIX_0 = IntArray(256)
        private val SUB_MIX_1 = IntArray(256)
        private val SUB_MIX_2 = IntArray(256)
        private val SUB_MIX_3 = IntArray(256)
        private val INV_SUB_MIX_0 = IntArray(256)
        private val INV_SUB_MIX_1 = IntArray(256)
        private val INV_SUB_MIX_2 = IntArray(256)
        private val INV_SUB_MIX_3 = IntArray(256)
        private val RCON = intArrayOf(0x00, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36)

        private const val BLOCK_SIZE = 16

        init {
            val d = IntArray(256) { if (it >= 128) (it shl 1) xor 0x11b else (it shl 1) }
            var x = 0
            var xi = 0
            for (i in 0 until 256) {
                var sx = xi xor (xi shl 1) xor (xi shl 2) xor (xi shl 3) xor (xi shl 4)
                sx = (sx ushr 8) xor (sx and 0xff) xor 0x63
                SBOX[x] = sx
                INV_SBOX[sx] = x
                val x2 = d[x]
                val x4 = d[x2]
                val x8 = d[x4]
                ((d[sx] * 0x101) xor (sx * 0x1010100)).also { t ->
                    SUB_MIX_0[x] = (t shl 24) or (t ushr 8)
                    SUB_MIX_1[x] = (t shl 16) or (t ushr 16)
                    SUB_MIX_2[x] = (t shl 8) or (t ushr 24)
                    SUB_MIX_3[x] = (t shl 0)
                }
                ((x8 * 0x1010101) xor (x4 * 0x10001) xor (x2 * 0x101) xor (x * 0x1010100)).also { t ->
                    INV_SUB_MIX_0[sx] = (t shl 24) or (t ushr 8)
                    INV_SUB_MIX_1[sx] = (t shl 16) or (t ushr 16)
                    INV_SUB_MIX_2[sx] = (t shl 8) or (t ushr 24)
                    INV_SUB_MIX_3[sx] = (t shl 0)
                }

                if (x == 0) {
                    x = 1; xi = 1
                } else {
                    x = x2 xor d[d[d[x8 xor x2]]]
                    xi = xi xor d[d[xi]]
                }
            }
        }

        fun encryptAesEcb(data: ByteArray, key: ByteArray, padding: Padding): ByteArray =
            AES(key)[CipherMode.ECB, padding].encrypt(data)

        fun decryptAesEcb(data: ByteArray, key: ByteArray, padding: Padding): ByteArray =
            AES(key)[CipherMode.ECB, padding].decrypt(data)

        fun encryptAesCbc(data: ByteArray, key: ByteArray, iv: ByteArray, padding: Padding): ByteArray =
            AES(key)[CipherMode.CBC, padding, iv].encrypt(data)

        fun decryptAesCbc(data: ByteArray, key: ByteArray, iv: ByteArray, padding: Padding): ByteArray =
            AES(key)[CipherMode.CBC, padding, iv].decrypt(data)

        fun encryptAesPcbc(data: ByteArray, key: ByteArray, iv: ByteArray, padding: Padding): ByteArray =
            AES(key)[CipherMode.PCBC, padding, iv].encrypt(data)

        fun decryptAesPcbc(data: ByteArray, key: ByteArray, iv: ByteArray, padding: Padding): ByteArray =
            AES(key)[CipherMode.PCBC, padding, iv].decrypt(data)

        fun encryptAesCfb(data: ByteArray, key: ByteArray, iv: ByteArray, padding: Padding): ByteArray =
            AES(key)[CipherMode.CFB, padding, iv].encrypt(data)

        fun decryptAesCfb(data: ByteArray, key: ByteArray, iv: ByteArray, padding: Padding): ByteArray =
            AES(key)[CipherMode.CFB, padding, iv].decrypt(data)

        fun encryptAesOfb(data: ByteArray, key: ByteArray, iv: ByteArray, padding: Padding): ByteArray =
            AES(key)[CipherMode.OFB, padding, iv].encrypt(data)

        fun decryptAesOfb(data: ByteArray, key: ByteArray, iv: ByteArray, padding: Padding): ByteArray =
            AES(key)[CipherMode.OFB, padding, iv].decrypt(data)

        fun encryptAesCtr(data: ByteArray, key: ByteArray, iv: ByteArray, padding: Padding): ByteArray =
            AES(key)[CipherMode.CTR, padding, iv].encrypt(data)

        fun decryptAesCtr(data: ByteArray, key: ByteArray, iv: ByteArray, padding: Padding): ByteArray =
            AES(key)[CipherMode.CTR, padding, iv].decrypt(data)
    }
}

private fun ByteArray.getu(offset: Int): Int = (this[offset].toInt() and 0xFF)
private inline fun Int.ext8(offset: Int): Int = (this ushr offset) and 0xFF
private fun ByteArray.toIntArray(): IntArray = IntArray(size / 4).also { for (n in it.indices) it[n] = getInt(n * 4) }
private fun ByteArray.getInt(offset: Int): Int = (getu(offset + 0) shl 24) or (getu(offset + 1) shl 16) or (getu(offset + 2) shl 8) or (getu(offset + 3) shl 0)
private fun ByteArray.setInt(offset: Int, value: Int) {
    this[offset + 0] = ((value shr 24) and 0xFF).toByte()
    this[offset + 1] = ((value shr 16) and 0xFF).toByte()
    this[offset + 2] = ((value shr 8) and 0xFF).toByte()
    this[offset + 3] = ((value shr 0) and 0xFF).toByte()
}

