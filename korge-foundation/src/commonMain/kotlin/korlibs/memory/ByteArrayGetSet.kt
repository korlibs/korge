package korlibs.memory

import korlibs.math.*
import korlibs.number.*

/////////////////////////////////////////
/////////////////////////////////////////
/////////////////////////////////////////

private fun ByteArray.u8(offset: Int): Int = this[offset].toInt() and 0xFF

private inline fun ByteArray.get16LE(offset: Int): Int = (u8(offset + 0) shl 0) or (u8(offset + 1) shl 8)
private inline fun ByteArray.get24LE(offset: Int): Int = (u8(offset + 0) shl 0) or (u8(offset + 1) shl 8) or (u8(offset + 2) shl 16)
private inline fun ByteArray.get32LE(offset: Int): Int = (u8(offset + 0) shl 0) or (u8(offset + 1) shl 8) or (u8(offset + 2) shl 16) or (u8(offset + 3) shl 24)
private inline fun ByteArray.get64LE(offset: Int): Long = (get32LE(offset + 0).unsigned shl 0) or (get32LE(offset + 4).unsigned shl 32)

private inline fun ByteArray.get16BE(offset: Int): Int = (u8(offset + 1) shl 0) or (u8(offset + 0) shl 8)
private inline fun ByteArray.get24BE(offset: Int): Int = (u8(offset + 2) shl 0) or (u8(offset + 1) shl 8) or (u8(offset + 0) shl 16)
private inline fun ByteArray.get32BE(offset: Int): Int = (u8(offset + 3) shl 0) or (u8(offset + 2) shl 8) or (u8(offset + 1) shl 16) or (u8(offset + 0) shl 24)
private inline fun ByteArray.get64BE(offset: Int): Long = (get32BE(offset + 4).unsigned shl 0) or (get32BE(offset + 0).unsigned shl 32)

// Unsigned
public fun ByteArray.getU8(offset: Int): Int = this[offset].toInt() and 0xFF
public fun ByteArray.getU16LE(offset: Int): Int = get16LE(offset)
public fun ByteArray.getU24LE(offset: Int): Int = get24LE(offset)
public fun ByteArray.getU32LE(offset: Int): Long = get32LE(offset).unsigned
public fun ByteArray.getU16BE(offset: Int): Int = get16BE(offset)
public fun ByteArray.getU24BE(offset: Int): Int = get24BE(offset)
public fun ByteArray.getU32BE(offset: Int): Long = get32BE(offset).unsigned

// Signed
public fun ByteArray.getS8(offset: Int): Int = this[offset].toInt()
public fun ByteArray.getS16LE(offset: Int): Int = get16LE(offset).signExtend(16)
public fun ByteArray.getS24LE(offset: Int): Int = get24LE(offset).signExtend(24)
public fun ByteArray.getS32LE(offset: Int): Int = get32LE(offset)
public fun ByteArray.getS64LE(offset: Int): Long = get64LE(offset)
public fun ByteArray.getF16LE(offset: Int): Half = Half.fromBits(get16LE(offset))
public fun ByteArray.getF32LE(offset: Int): Float = Float.fromBits(get32LE(offset))
public fun ByteArray.getF64LE(offset: Int): Double = Double.fromBits(get64LE(offset))
public fun ByteArray.getS16BE(offset: Int): Int = get16BE(offset).signExtend(16)
public fun ByteArray.getS24BE(offset: Int): Int = get24BE(offset).signExtend(24)
public fun ByteArray.getS32BE(offset: Int): Int = get32BE(offset)
public fun ByteArray.getS64BE(offset: Int): Long = get64BE(offset)
public fun ByteArray.getF16BE(offset: Int): Half = Half.fromBits(get16BE(offset))
public fun ByteArray.getF32BE(offset: Int): Float = Float.fromBits(get32BE(offset))
public fun ByteArray.getF64BE(offset: Int): Double = Double.fromBits(get64BE(offset))

// Custom Endian
public fun ByteArray.getU16(offset: Int, little: Boolean): Int = if (little) getU16LE(offset) else getU16BE(offset)
public fun ByteArray.getU24(offset: Int, little: Boolean): Int = if (little) getU24LE(offset) else getU24BE(offset)
public fun ByteArray.getU32(offset: Int, little: Boolean): Long = if (little) getU32LE(offset) else getU32BE(offset)
public fun ByteArray.getS16(offset: Int, little: Boolean): Int = if (little) getS16LE(offset) else getS16BE(offset)
public fun ByteArray.getS24(offset: Int, little: Boolean): Int = if (little) getS24LE(offset) else getS24BE(offset)
public fun ByteArray.getS32(offset: Int, little: Boolean): Int = if (little) getS32LE(offset) else getS32BE(offset)
public fun ByteArray.getS64(offset: Int, little: Boolean): Long = if (little) getS64LE(offset) else getS64BE(offset)
public fun ByteArray.getF16(offset: Int, little: Boolean): Half = if (little) getF16LE(offset) else getF16BE(offset)
public fun ByteArray.getF32(offset: Int, little: Boolean): Float = if (little) getF32LE(offset) else getF32BE(offset)
public fun ByteArray.getF64(offset: Int, little: Boolean): Double = if (little) getF64LE(offset) else getF64BE(offset)

private inline fun <T> ByteArray.getTypedArray(offset: Int, count: Int, elementSize: Int, array: T, crossinline get: ByteArray.(array: T, n: Int, pos: Int) -> Unit): T = array.also {
    for (n in 0 until count) get(this, array, n, offset + n * elementSize)
}

public fun ByteArray.getS8Array(offset: Int, count: Int): ByteArray = this.copyOfRange(offset, offset + count)
public fun ByteArray.getS16LEArray(offset: Int, count: Int): ShortArray = this.getTypedArray(offset, count, 2, ShortArray(count)) { array, n, pos -> array[n] = getS16LE(pos).toShort() }
public fun ByteArray.getU16LEArray(offset: Int, count: Int): CharArray = this.getTypedArray(offset, count, 2, kotlin.CharArray(count)) { array, n, pos -> array[n] = getS16LE(pos).toChar() }
public fun ByteArray.getS32LEArray(offset: Int, count: Int): IntArray = this.getTypedArray(offset, count, 4, IntArray(count)) { array, n, pos -> array[n] = getS32LE(pos) }
public fun ByteArray.getS64LEArray(offset: Int, count: Int): LongArray = this.getTypedArray(offset, count, 8, LongArray(count)) { array, n, pos -> array[n] = getS64LE(pos) }
public fun ByteArray.getF32LEArray(offset: Int, count: Int): FloatArray = this.getTypedArray(offset, count, 4, FloatArray(count)) { array, n, pos -> array[n] = getF32LE(pos) }
public fun ByteArray.getF64LEArray(offset: Int, count: Int): DoubleArray = this.getTypedArray(offset, count, 8, DoubleArray(count)) { array, n, pos -> array[n] = getF64LE(pos) }
public fun ByteArray.getS16BEArray(offset: Int, count: Int): ShortArray = this.getTypedArray(offset, count, 2, ShortArray(count)) { array, n, pos -> array[n] = getS16BE(pos).toShort() }
public fun ByteArray.getU16BEArray(offset: Int, count: Int): CharArray = this.getTypedArray(offset, count, 2, kotlin.CharArray(count)) { array, n, pos -> array[n] = getS16BE(pos).toChar() }
public fun ByteArray.getS32BEArray(offset: Int, count: Int): IntArray = this.getTypedArray(offset, count, 4, IntArray(count)) { array, n, pos -> array[n] = getS32BE(pos) }
public fun ByteArray.getS64BEArray(offset: Int, count: Int): LongArray = this.getTypedArray(offset, count, 8, LongArray(count)) { array, n, pos -> array[n] = getS64BE(pos) }
public fun ByteArray.getF32BEArray(offset: Int, count: Int): FloatArray = this.getTypedArray(offset, count, 4, FloatArray(count)) { array, n, pos -> array[n] = getF32BE(pos) }
public fun ByteArray.getF64BEArray(offset: Int, count: Int): DoubleArray = this.getTypedArray(offset, count, 8, DoubleArray(count)) { array, n, pos -> array[n] = getF64BE(pos) }

public fun ByteArray.getS16Array(offset: Int, count: Int, little: Boolean): ShortArray = if (little) getS16LEArray(offset, count) else getS16BEArray(offset, count)
public fun ByteArray.getU16Array(offset: Int, count: Int, little: Boolean): CharArray = if (little) getU16LEArray(offset, count) else getU16BEArray(offset, count)
public fun ByteArray.getS32Array(offset: Int, count: Int, little: Boolean): IntArray = if (little) getS32LEArray(offset, count) else getS32BEArray(offset, count)
public fun ByteArray.getS64Array(offset: Int, count: Int, little: Boolean): LongArray = if (little) getS64LEArray(offset, count) else getS64BEArray(offset, count)
public fun ByteArray.getF32Array(offset: Int, count: Int, little: Boolean): FloatArray = if (little) getF32LEArray(offset, count) else getF32BEArray(offset, count)
public fun ByteArray.getF64Array(offset: Int, count: Int, little: Boolean): DoubleArray = if (little) getF64LEArray(offset, count) else getF64BEArray(offset, count)

/////////////////////////////////////////
/////////////////////////////////////////
/////////////////////////////////////////

public fun ByteArray.set8(offset: Int, value: Int) { this[offset] = value.toByte() }
public fun ByteArray.set8(offset: Int, value: Long) { this[offset] = value.toByte() }
public fun ByteArray.set16(offset: Int, value: Int, little: Boolean) { if (little) set16LE(offset, value) else set16BE(offset, value) }
public fun ByteArray.set24(offset: Int, value: Int, little: Boolean) { if (little) set24LE(offset, value) else set24BE(offset, value) }
public fun ByteArray.set32(offset: Int, value: Int, little: Boolean) { if (little) set32LE(offset, value) else set32BE(offset, value) }
public fun ByteArray.set64(offset: Int, value: Long, little: Boolean) { if (little) set64LE(offset, value) else set64BE(offset, value) }
public fun ByteArray.setF16(offset: Int, value: Half, little: Boolean) { if (little) setF16LE(offset, value) else setF16BE(offset, value) }
public fun ByteArray.setF32(offset: Int, value: Float, little: Boolean) { if (little) setF32LE(offset, value) else setF32BE(offset, value) }
public fun ByteArray.setF64(offset: Int, value: Double, little: Boolean) { if (little) setF64LE(offset, value) else setF64BE(offset, value) }

public fun ByteArray.set16LE(offset: Int, value: Int) { this[offset + 0] = value.extractByte(0); this[offset + 1] = value.extractByte(8) }
public fun ByteArray.set24LE(offset: Int, value: Int) { this[offset + 0] = value.extractByte(0); this[offset + 1] = value.extractByte(8); this[offset + 2] = value.extractByte(16) }
public fun ByteArray.set32LE(offset: Int, value: Int) { this[offset + 0] = value.extractByte(0); this[offset + 1] = value.extractByte(8); this[offset + 2] = value.extractByte(16); this[offset + 3] = value.extractByte(24) }
public fun ByteArray.set32LE(offset: Int, value: Long) { set32LE(offset, value.toInt()) }
public fun ByteArray.set64LE(offset: Int, value: Long) { set32LE(offset + 0, (value ushr 0).toInt()); set32LE(offset + 4, (value ushr 32).toInt()) }
public fun ByteArray.setF16LE(offset: Int, value: Half) { set16LE(offset + 0, value.toRawBits().toInt()) }
public fun ByteArray.setF32LE(offset: Int, value: Float) { set32LE(offset + 0, value.toRawBits()) }
public fun ByteArray.setF64LE(offset: Int, value: Double) { set64LE(offset + 0, value.toRawBits()) }

public fun ByteArray.set16BE(offset: Int, value: Int) { this[offset + 1] = value.extractByte(0); this[offset + 0] = value.extractByte(8) }
public fun ByteArray.set24BE(offset: Int, value: Int) { this[offset + 2] = value.extractByte(0); this[offset + 1] = value.extractByte(8); this[offset + 0] = value.extractByte(16) }
public fun ByteArray.set32BE(offset: Int, value: Int) { this[offset + 3] = value.extractByte(0); this[offset + 2] = value.extractByte(8); this[offset + 1] = value.extractByte(16); this[offset + 0] = value.extractByte(24) }
public fun ByteArray.set32BE(offset: Int, value: Long) { set32BE(offset, value.toInt()) }
public fun ByteArray.set64BE(offset: Int, value: Long) { set32BE(offset + 0, (value ushr 32).toInt()); set32BE(offset + 4, (value ushr 0).toInt()) }
public fun ByteArray.setF16BE(offset: Int, value: Half) { set16BE(offset + 0, value.toRawBits().toInt()) }
public fun ByteArray.setF32BE(offset: Int, value: Float) { set32BE(offset + 0, value.toRawBits()) }
public fun ByteArray.setF64BE(offset: Int, value: Double) { set64BE(offset + 0, value.toRawBits()) }

public fun ByteArray.setBytes(offset: Int, bytes: ByteArray): Unit = arraycopy(bytes, 0, this, offset, bytes.size)

private inline fun wa(offset: Int, elementSize: Int, size: Int, set: (p: Int, n: Int) -> Unit) { for (n in 0 until size) set(offset + n * elementSize, n) }

public fun ByteArray.setArrayLE(offset: Int, array: CharArray): Unit = wa(offset, 2, array.size) { p, n -> set16LE(p, array[n].toInt()) }
public fun ByteArray.setArrayLE(offset: Int, array: ShortArray): Unit = wa(offset, 2, array.size) { p, n -> set16LE(p, array[n].toInt()) }
public fun ByteArray.setArrayLE(offset: Int, array: IntArray): Unit = wa(offset, 4, array.size) { p, n -> set32LE(p, array[n]) }
public fun ByteArray.setArrayLE(offset: Int, array: LongArray): Unit = wa(offset, 8, array.size) { p, n -> set64LE(p, array[n]) }
public fun ByteArray.setArrayLE(offset: Int, array: FloatArray): Unit = wa(offset, 4, array.size) { p, n -> setF32LE(p, array[n]) }
public fun ByteArray.setArrayLE(offset: Int, array: DoubleArray): Unit = wa(offset, 8, array.size) { p, n -> setF64LE(p, array[n]) }

public fun ByteArray.setArrayBE(offset: Int, array: CharArray): Unit = wa(offset, 2, array.size) { p, n -> set16BE(p, array[n].toInt()) }
public fun ByteArray.setArrayBE(offset: Int, array: ShortArray): Unit = wa(offset, 2, array.size) { p, n -> set16BE(p, array[n].toInt()) }
public fun ByteArray.setArrayBE(offset: Int, array: IntArray): Unit = wa(offset, 4, array.size) { p, n -> set32BE(p, array[n]) }
public fun ByteArray.setArrayBE(offset: Int, array: LongArray): Unit = wa(offset, 8, array.size) { p, n -> set64BE(p, array[n]) }
public fun ByteArray.setArrayBE(offset: Int, array: FloatArray): Unit = wa(offset, 4, array.size) { p, n -> setF32BE(p, array[n]) }
public fun ByteArray.setArrayBE(offset: Int, array: DoubleArray): Unit = wa(offset, 8, array.size) { p, n -> setF64BE(p, array[n]) }
