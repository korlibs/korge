package korlibs.crypto

abstract class SHA(chunkSize: Int, digestSize: Int, name: String = "SHA${digestSize * 8}") : Hasher(chunkSize, digestSize, name) {
    override fun corePadding(totalWritten: Long): ByteArray {
        val tail = totalWritten % 64
        val padding = if (64 - tail >= 9) 64 - tail else 128 - tail
        val pad = ByteArray(padding.toInt()).also { it[0] = 0x80.toByte() }
        val bits = (totalWritten * 8)
        for (i in 0 until 8) pad[pad.size - 1 - i] = ((bits ushr (8 * i)) and 0xFF).toByte()
        return pad
    }

    protected fun ByteArray.readU8(o: Int): Int = this[o].toInt() and 0xFF
    protected fun ByteArray.readS32_be(o: Int): Int =
        (readU8(o + 3) shl 0) or (readU8(o + 2) shl 8) or (readU8(o + 1) shl 16) or (readU8(o + 0) shl 24)
}
