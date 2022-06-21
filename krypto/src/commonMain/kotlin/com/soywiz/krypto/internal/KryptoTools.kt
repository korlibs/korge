package com.soywiz.krypto.internal

import kotlin.experimental.xor

internal inline fun Int.ext8(offset: Int) = (this ushr offset) and 0xFF

internal fun Int.rotateRight(amount: Int): Int = (this ushr amount) or (this shl (32 - amount))
internal fun Int.rotateLeft(bits: Int): Int = ((this shl bits) or (this ushr (32 - bits)))

internal fun arraycopy(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, count: Int) = src.copyInto(dst, dstPos, srcPos, srcPos + count)
internal fun arraycopy(src: IntArray, srcPos: Int, dst: IntArray, dstPos: Int, count: Int) = src.copyInto(dst, dstPos, srcPos, srcPos + count)

internal fun ByteArray.readU8(o: Int): Int = this[o].toInt() and 0xFF
internal fun ByteArray.readS32_be(o: Int): Int =
    (readU8(o + 3) shl 0) or (readU8(o + 2) shl 8) or (readU8(o + 1) shl 16) or (readU8(o + 0) shl 24)

internal fun ByteArray.getu(offset: Int): Int = (this[offset].toInt() and 0xFF)

internal fun ByteArray.getInt(offset: Int): Int =
    (getu(offset + 0) shl 24) or (getu(offset + 1) shl 16) or (getu(offset + 2) shl 8) or (getu(offset + 3) shl 0)

internal fun ByteArray.setInt(offset: Int, value: Int) {
    this[offset + 0] = ((value shr 24) and 0xFF).toByte()
    this[offset + 1] = ((value shr 16) and 0xFF).toByte()
    this[offset + 2] = ((value shr 8) and 0xFF).toByte()
    this[offset + 3] = ((value shr 0) and 0xFF).toByte()
}

internal fun ByteArray.toIntArray(): IntArray =
    IntArray(size / 4).also { for (n in it.indices) it[n] = getInt(n * 4) }

internal fun IntArray.toByteArray(): ByteArray =
    ByteArray(size * 4).also { for (n in indices) it.setInt(n * 4, this[n]) }

internal fun getIV(srcIV: ByteArray?, blockSize: Int): ByteArray {
    if (srcIV == null) TODO("IV not provided")
    if (srcIV.size < blockSize) throw IllegalArgumentException("Wrong IV length: must be $blockSize bytes long")
    return srcIV.copyOf(blockSize)
    //return ByteArray(blockSize).also { dstIV -> arraycopy(srcIV, 0, dstIV, 0, kotlin.math.min(srcIV.size, dstIV.size)) }
}

internal fun arrayxor(data: ByteArray, offset: Int, xor: ByteArray) {
    for (n in xor.indices) data[offset + n] = data[offset + n] xor xor[n]
}

internal fun arrayxor(data: IntArray, offset: Int, xor: IntArray) {
    for (n in xor.indices) data[offset + n] = data[offset + n] xor xor[n]
}

internal fun arrayxor(data: ByteArray, offset: Int, size: Int, xor: ByteArray, xoroffset: Int) {
    for (n in 0 until size) data[offset + n] = data[offset + n] xor xor[xoroffset + n]
}




