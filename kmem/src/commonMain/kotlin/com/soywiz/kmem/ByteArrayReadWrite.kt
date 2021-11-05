package com.soywiz.kmem

/////////////////////////////////////////
/////////////////////////////////////////
/////////////////////////////////////////

private fun ByteArray.u8(o: Int): Int = this[o].toInt() and 0xFF

private inline fun ByteArray.read16LE(o: Int): Int = (u8(o + 0) shl 0) or (u8(o + 1) shl 8)
private inline fun ByteArray.read24LE(o: Int): Int = (u8(o + 0) shl 0) or (u8(o + 1) shl 8) or (u8(o + 2) shl 16)
private inline fun ByteArray.read32LE(o: Int): Int = (u8(o + 0) shl 0) or (u8(o + 1) shl 8) or (u8(o + 2) shl 16) or (u8(o + 3) shl 24)
private inline fun ByteArray.read64LE(o: Int): Long = (read32LE(o + 0).unsigned shl 0) or (read32LE(o + 4).unsigned shl 32)

private inline fun ByteArray.read16BE(o: Int): Int = (u8(o + 1) shl 0) or (u8(o + 0) shl 8)
private inline fun ByteArray.read24BE(o: Int): Int = (u8(o + 2) shl 0) or (u8(o + 1) shl 8) or (u8(o + 0) shl 16)
private inline fun ByteArray.read32BE(o: Int): Int = (u8(o + 3) shl 0) or (u8(o + 2) shl 8) or (u8(o + 1) shl 16) or (u8(o + 0) shl 24)
private inline fun ByteArray.read64BE(o: Int): Long = (read32BE(o + 4).unsigned shl 0) or (read32BE(o + 0).unsigned shl 32)

// Unsigned
public fun ByteArray.readU8(o: Int): Int = this[o].toInt() and 0xFF
public fun ByteArray.readU16LE(o: Int): Int = read16LE(o)
public fun ByteArray.readU24LE(o: Int): Int = read24LE(o)
public fun ByteArray.readU32LE(o: Int): Long = read32LE(o).unsigned
public fun ByteArray.readU16BE(o: Int): Int = read16BE(o)
public fun ByteArray.readU24BE(o: Int): Int = read24BE(o)
public fun ByteArray.readU32BE(o: Int): Long = read32BE(o).unsigned

// Signed
public fun ByteArray.readS8(o: Int): Int = this[o].toInt()
public fun ByteArray.readS16LE(o: Int): Int = read16LE(o).signExtend(16)
public fun ByteArray.readS24LE(o: Int): Int = read24LE(o).signExtend(24)
public fun ByteArray.readS32LE(o: Int): Int = read32LE(o)
public fun ByteArray.readS64LE(o: Int): Long = read64LE(o)
public fun ByteArray.readF16LE(o: Int): Float16 = Float16.fromBits(read16LE(o))
public fun ByteArray.readF32LE(o: Int): Float = Float.fromBits(read32LE(o))
public fun ByteArray.readF64LE(o: Int): Double = Double.fromBits(read64LE(o))
public fun ByteArray.readS16BE(o: Int): Int = read16BE(o).signExtend(16)
public fun ByteArray.readS24BE(o: Int): Int = read24BE(o).signExtend(24)
public fun ByteArray.readS32BE(o: Int): Int = read32BE(o)
public fun ByteArray.readS64BE(o: Int): Long = read64BE(o)
public fun ByteArray.readF16BE(o: Int): Float16 = Float16.fromBits(read16BE(o))
public fun ByteArray.readF32BE(o: Int): Float = Float.fromBits(read32BE(o))
public fun ByteArray.readF64BE(o: Int): Double = Double.fromBits(read64BE(o))

// Custom Endian
public fun ByteArray.readU16(o: Int, little: Boolean): Int = if (little) readU16LE(o) else readU16BE(o)
public fun ByteArray.readU24(o: Int, little: Boolean): Int = if (little) readU24LE(o) else readU24BE(o)
public fun ByteArray.readU32(o: Int, little: Boolean): Long = if (little) readU32LE(o) else readU32BE(o)
public fun ByteArray.readS16(o: Int, little: Boolean): Int = if (little) readS16LE(o) else readS16BE(o)
public fun ByteArray.readS24(o: Int, little: Boolean): Int = if (little) readS24LE(o) else readS24BE(o)
public fun ByteArray.readS32(o: Int, little: Boolean): Int = if (little) readS32LE(o) else readS32BE(o)
public fun ByteArray.readS64(o: Int, little: Boolean): Long = if (little) readS64LE(o) else readS64BE(o)
public fun ByteArray.readF16(o: Int, little: Boolean): Float16 = if (little) readF16LE(o) else readF16BE(o)
public fun ByteArray.readF32(o: Int, little: Boolean): Float = if (little) readF32LE(o) else readF32BE(o)
public fun ByteArray.readF64(o: Int, little: Boolean): Double = if (little) readF64LE(o) else readF64BE(o)

private inline fun <T> ByteArray.readTypedArray(o: Int, count: Int, elementSize: Int, array: T, crossinline read: ByteArray.(array: T, n: Int, pos: Int) -> Unit): T = array.also {
    for (n in 0 until count) read(this, array, n, o + n * elementSize)
}

public fun ByteArray.readByteArray(o: Int, count: Int): ByteArray = this.copyOfRange(o, o + count)
public fun ByteArray.readShortArrayLE(o: Int, count: Int): ShortArray = this.readTypedArray(o, count, 2, ShortArray(count)) { array, n, pos -> array[n] = readS16LE(pos).toShort() }
public fun ByteArray.readCharArrayLE(o: Int, count: Int): CharArray = this.readTypedArray(o, count, 2, kotlin.CharArray(count)) { array, n, pos -> array[n] = readS16LE(pos).toChar() }
public fun ByteArray.readIntArrayLE(o: Int, count: Int): IntArray = this.readTypedArray(o, count, 4, IntArray(count)) { array, n, pos -> array[n] = readS32LE(pos) }
public fun ByteArray.readLongArrayLE(o: Int, count: Int): LongArray = this.readTypedArray(o, count, 8, LongArray(count)) { array, n, pos -> array[n] = readS64LE(pos) }
public fun ByteArray.readFloatArrayLE(o: Int, count: Int): FloatArray = this.readTypedArray(o, count, 4, FloatArray(count)) { array, n, pos -> array[n] = readF32LE(pos) }
public fun ByteArray.readDoubleArrayLE(o: Int, count: Int): DoubleArray = this.readTypedArray(o, count, 8, DoubleArray(count)) { array, n, pos -> array[n] = readF64LE(pos) }
public fun ByteArray.readShortArrayBE(o: Int, count: Int): ShortArray = this.readTypedArray(o, count, 2, ShortArray(count)) { array, n, pos -> array[n] = readS16BE(pos).toShort() }
public fun ByteArray.readCharArrayBE(o: Int, count: Int): CharArray = this.readTypedArray(o, count, 2, kotlin.CharArray(count)) { array, n, pos -> array[n] = readS16BE(pos).toChar() }
public fun ByteArray.readIntArrayBE(o: Int, count: Int): IntArray = this.readTypedArray(o, count, 4, IntArray(count)) { array, n, pos -> array[n] = readS32BE(pos) }
public fun ByteArray.readLongArrayBE(o: Int, count: Int): LongArray = this.readTypedArray(o, count, 8, LongArray(count)) { array, n, pos -> array[n] = readS64BE(pos) }
public fun ByteArray.readFloatArrayBE(o: Int, count: Int): FloatArray = this.readTypedArray(o, count, 4, FloatArray(count)) { array, n, pos -> array[n] = readF32BE(pos) }
public fun ByteArray.readDoubleArrayBE(o: Int, count: Int): DoubleArray = this.readTypedArray(o, count, 8, DoubleArray(count)) { array, n, pos -> array[n] = readF64BE(pos) }

public fun ByteArray.readShortArray(o: Int, count: Int, little: Boolean): ShortArray = if (little) readShortArrayLE(o, count) else readShortArrayBE(o, count)
public fun ByteArray.readCharArray(o: Int, count: Int, little: Boolean): CharArray = if (little) readCharArrayLE(o, count) else readCharArrayBE(o, count)
public fun ByteArray.readIntArray(o: Int, count: Int, little: Boolean): IntArray = if (little) readIntArrayLE(o, count) else readIntArrayBE(o, count)
public fun ByteArray.readLongArray(o: Int, count: Int, little: Boolean): LongArray = if (little) readLongArrayLE(o, count) else readLongArrayBE(o, count)
public fun ByteArray.readFloatArray(o: Int, count: Int, little: Boolean): FloatArray = if (little) readFloatArrayLE(o, count) else readFloatArrayBE(o, count)
public fun ByteArray.readDoubleArray(o: Int, count: Int, little: Boolean): DoubleArray = if (little) readDoubleArrayLE(o, count) else readDoubleArrayBE(o, count)

/////////////////////////////////////////
/////////////////////////////////////////
/////////////////////////////////////////

public fun ByteArray.write8(o: Int, v: Int) { this[o] = v.toByte() }
public fun ByteArray.write8(o: Int, v: Long) { this[o] = v.toByte() }
public fun ByteArray.write16(o: Int, v: Int, little: Boolean) { if (little) write16LE(o, v) else write16BE(o, v) }
public fun ByteArray.write24(o: Int, v: Int, little: Boolean) { if (little) write24LE(o, v) else write24BE(o, v) }
public fun ByteArray.write32(o: Int, v: Int, little: Boolean) { if (little) write32LE(o, v) else write32BE(o, v) }
public fun ByteArray.write64(o: Int, v: Long, little: Boolean) { if (little) write64LE(o, v) else write64BE(o, v) }
public fun ByteArray.writeF16(o: Int, v: Float16, little: Boolean) { if (little) writeF16LE(o, v) else writeF16BE(o, v) }
public fun ByteArray.writeF32(o: Int, v: Float, little: Boolean) { if (little) writeF32LE(o, v) else writeF32BE(o, v) }
public fun ByteArray.writeF64(o: Int, v: Double, little: Boolean) { if (little) writeF64LE(o, v) else writeF64BE(o, v) }

public fun ByteArray.write16LE(o: Int, v: Int) { this[o + 0] = v.extractByte(0); this[o + 1] = v.extractByte(8) }
public fun ByteArray.write24LE(o: Int, v: Int) { this[o + 0] = v.extractByte(0); this[o + 1] = v.extractByte(8); this[o + 2] = v.extractByte(16) }
public fun ByteArray.write32LE(o: Int, v: Int) { this[o + 0] = v.extractByte(0); this[o + 1] = v.extractByte(8); this[o + 2] = v.extractByte(16); this[o + 3] = v.extractByte(24) }
public fun ByteArray.write32LE(o: Int, v: Long) { write32LE(o, v.toInt()) }
public fun ByteArray.write64LE(o: Int, v: Long) { write32LE(o + 0, (v ushr 0).toInt()); write32LE(o + 4, (v ushr 32).toInt()) }
public fun ByteArray.writeF16LE(o: Int, v: Float16) { write16LE(o + 0, v.toRawBits().toInt()) }
public fun ByteArray.writeF32LE(o: Int, v: Float) { write32LE(o + 0, v.toRawBits()) }
public fun ByteArray.writeF64LE(o: Int, v: Double) { write64LE(o + 0, v.toRawBits()) }

public fun ByteArray.write16BE(o: Int, v: Int) { this[o + 1] = v.extractByte(0); this[o + 0] = v.extractByte(8) }
public fun ByteArray.write24BE(o: Int, v: Int) { this[o + 2] = v.extractByte(0); this[o + 1] = v.extractByte(8); this[o + 0] = v.extractByte(16) }
public fun ByteArray.write32BE(o: Int, v: Int) { this[o + 3] = v.extractByte(0); this[o + 2] = v.extractByte(8); this[o + 1] = v.extractByte(16); this[o + 0] = v.extractByte(24) }
public fun ByteArray.write32BE(o: Int, v: Long) { write32BE(o, v.toInt()) }
public fun ByteArray.write64BE(o: Int, v: Long) { write32BE(o + 0, (v ushr 32).toInt()); write32BE(o + 4, (v ushr 0).toInt()) }
public fun ByteArray.writeF16BE(o: Int, v: Float16) { write16BE(o + 0, v.toRawBits().toInt()) }
public fun ByteArray.writeF32BE(o: Int, v: Float) { write32BE(o + 0, v.toRawBits()) }
public fun ByteArray.writeF64BE(o: Int, v: Double) { write64BE(o + 0, v.toRawBits()) }

public fun ByteArray.writeBytes(o: Int, bytes: ByteArray): Unit = arraycopy(bytes, 0, this, o, bytes.size)

private inline fun wa(o: Int, elementSize: Int, size: Int, write: (p: Int, n: Int) -> Unit) { for (n in 0 until size) write(o + n * elementSize, n) }

public fun ByteArray.writeArrayLE(o: Int, array: CharArray): Unit = wa(o, 2, array.size) { p, n -> write16LE(p, array[n].toInt()) }
public fun ByteArray.writeArrayLE(o: Int, array: ShortArray): Unit = wa(o, 2, array.size) { p, n -> write16LE(p, array[n].toInt()) }
public fun ByteArray.writeArrayLE(o: Int, array: IntArray): Unit = wa(o, 4, array.size) { p, n -> write32LE(p, array[n]) }
public fun ByteArray.writeArrayLE(o: Int, array: LongArray): Unit = wa(o, 8, array.size) { p, n -> write64LE(p, array[n]) }
public fun ByteArray.writeArrayLE(o: Int, array: FloatArray): Unit = wa(o, 4, array.size) { p, n -> writeF32LE(p, array[n]) }
public fun ByteArray.writeArrayLE(o: Int, array: DoubleArray): Unit = wa(o, 8, array.size) { p, n -> writeF64LE(p, array[n]) }

public fun ByteArray.writeArrayBE(o: Int, array: CharArray): Unit = wa(o, 2, array.size) { p, n -> write16BE(p, array[n].toInt()) }
public fun ByteArray.writeArrayBE(o: Int, array: ShortArray): Unit = wa(o, 2, array.size) { p, n -> write16BE(p, array[n].toInt()) }
public fun ByteArray.writeArrayBE(o: Int, array: IntArray): Unit = wa(o, 4, array.size) { p, n -> write32BE(p, array[n]) }
public fun ByteArray.writeArrayBE(o: Int, array: LongArray): Unit = wa(o, 8, array.size) { p, n -> write64BE(p, array[n]) }
public fun ByteArray.writeArrayBE(o: Int, array: FloatArray): Unit = wa(o, 4, array.size) { p, n -> writeF32BE(p, array[n]) }
public fun ByteArray.writeArrayBE(o: Int, array: DoubleArray): Unit = wa(o, 8, array.size) { p, n -> writeF64BE(p, array[n]) }
