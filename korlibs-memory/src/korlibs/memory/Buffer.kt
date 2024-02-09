@file:OptIn(ExperimentalUnsignedTypes::class)

package korlibs.memory

import korlibs.math.unsigned
import korlibs.math.clampUByte
import korlibs.math.clampUShort
import kotlin.jvm.*

typealias DataView = Buffer

expect class Buffer {
    constructor(size: Int, direct: Boolean = false)
    @Deprecated("Can't wrap without copying on WasmJS")
    constructor(array: ByteArray, offset: Int = 0, size: Int = array.size - offset)

    val byteOffset: Int
    val sizeInBytes: Int
    internal fun sliceInternal(start: Int, end: Int): Buffer

    fun transferBytes(bufferOffset: Int, array: ByteArray, arrayOffset: Int, len: Int, toArray: Boolean): Unit

    fun getS8(byteOffset: Int): Byte
    fun getS16LE(byteOffset: Int): Short
    fun getS32LE(byteOffset: Int): Int
    fun getS64LE(byteOffset: Int): Long
    fun getF32LE(byteOffset: Int): Float
    fun getF64LE(byteOffset: Int): Double
    fun getS16BE(byteOffset: Int): Short
    fun getS32BE(byteOffset: Int): Int
    fun getS64BE(byteOffset: Int): Long
    fun getF32BE(byteOffset: Int): Float
    fun getF64BE(byteOffset: Int): Double

    fun set8(byteOffset: Int, value: Byte)
    fun set16LE(byteOffset: Int, value: Short)
    fun set32LE(byteOffset: Int, value: Int)
    fun set64LE(byteOffset: Int, value: Long)
    fun setF32LE(byteOffset: Int, value: Float)
    fun setF64LE(byteOffset: Int, value: Double)
    fun set16BE(byteOffset: Int, value: Short)
    fun set32BE(byteOffset: Int, value: Int)
    fun set64BE(byteOffset: Int, value: Long)
    fun setF32BE(byteOffset: Int, value: Float)
    fun setF64BE(byteOffset: Int, value: Double)

    companion object {
        fun copy(src: Buffer, srcPosBytes: Int, dst: Buffer, dstPosBytes: Int, sizeInBytes: Int)
        fun equals(src: Buffer, srcPosBytes: Int, dst: Buffer, dstPosBytes: Int, sizeInBytes: Int): Boolean
    }
}

internal fun Buffer.Companion.hashCodeCommon(
    buffer: Buffer
): Int {
    var h = 1
    val len = buffer.sizeInBytes
    for (n in 0 until (len / 4)) {
        h = 31 * h + buffer.getS32LE(n * 4)
    }
    val offset = (len / 4) * 4
    for (n in 0 until len % 4) {
        h = 31 * h + buffer.getS8(offset + n)
    }
    return h
}

internal fun Buffer.Companion.equalsCommon(
    that: Buffer,
    other: Any?
): Boolean {
    if (other !is Buffer || that.size != other.size) return false
    return Buffer.equals(that, 0, other, 0, that.sizeInBytes)
}

internal fun Buffer.Companion.equalsCommon(
    src: Buffer,
    srcPosBytes: Int,
    dst: Buffer,
    dstPosBytes: Int,
    sizeInBytes: Int,
    use64: Boolean = true,
): Boolean {
    check(srcPosBytes + sizeInBytes <= src.sizeInBytes)
    check(dstPosBytes + sizeInBytes <= dst.sizeInBytes)
    //for (n in 0 until sizeInBytes) {
    //    if (src.getUnalignedInt8(srcPosBytes + n) != dst.getUnalignedInt8(dstPosBytes + n)) return false
    //}
    //return true
    var offset = 0
    var remaining = sizeInBytes

    if (use64) {
        val WORD = 8
        val words = remaining / WORD
        remaining %= WORD
        for (n in 0 until words) {
            val v0 = src.getS64LE(srcPosBytes + offset + n * WORD)
            val v1 = dst.getS64LE(dstPosBytes + offset + n * WORD)
            if (v0 != v1) {
                return false
            }
        }
        offset += words * WORD
    }
    if (true) {
        val WORD = 4
        val words = remaining / WORD
        remaining %= WORD
        for (n in 0 until words) {
            val v0 = src.getS32LE(srcPosBytes + offset + n * WORD)
            val v1 = dst.getS32LE(dstPosBytes + offset + n * WORD)
            if (v0 != v1) {
                return false
            }
        }
        offset += words * WORD
    }

    if (true) {
        for (n in 0 until remaining) {
            val v0 = src.getS8(srcPosBytes + offset + n)
            val v1 = dst.getS8(dstPosBytes + offset + n)
            if (v0 != v1) {
                return false
            }
        }
    }
    return true
}

internal fun NBuffer_toString(buffer: Buffer): String = "Buffer(size=${buffer.size})"
internal fun checkNBufferSize(size: Int): Int {
    if (size < 0) throw IllegalArgumentException("invalid size $size")
    return size
}
internal fun checkNBufferWrap(array: ByteArray, offset: Int, size: Int): ByteArray {
    val end = offset + size
    if (size < 0 || offset !in 0..array.size || end !in 0..array.size) {
        throw IllegalArgumentException("invalid arguments offset=$offset, size=$size for array.size=${array.size}")
    }
    return array
}
val Buffer.size: Int get() = sizeInBytes
fun Buffer.Companion.allocDirect(size: Int): Buffer = Buffer(size, direct = true)
fun Buffer.Companion.allocNoDirect(size: Int): Buffer = Buffer(size, direct = false)

fun Buffer.copyOf(newSize: Int): Buffer {
    val out = Buffer(newSize)
    arraycopy(this, 0, out, 0, kotlin.math.min(this.sizeInBytes, newSize))
    return out
}
fun Buffer.clone(direct: Boolean = false): Buffer {
    val out = Buffer(this.size, direct)
    arraycopy(this, 0, out, 0, size)
    return out
}

private fun Int.hexChar(): Char = when (this) {
    in 0..9 -> '0' + this
    in 10..26 -> 'a' + (this - 10)
    else -> '?'
}
fun Buffer.hex(): String = buildString(sizeInBytes * 2) {
    for (n in 0 until this@hex.sizeInBytes) {
        val value = this@hex.getUInt8(n)
        append(value.extract4(4).hexChar())
        append(value.extract4(0).hexChar())
    }
}
fun Buffer.sliceWithSize(start: Int, size: Int): Buffer = sliceBuffer(start, start + size)
fun Buffer.sliceBuffer(start: Int = 0, end: Int = sizeInBytes): Buffer {
    if (start > end || start !in 0 .. sizeInBytes || end !in 0 .. sizeInBytes) {
        throw IllegalArgumentException("invalid slice start:$start, end:$end not in 0..$sizeInBytes")
    }
    return sliceInternal(start, end)
}

// Basic functions

fun Buffer.getU8(byteOffset: Int): Int = getS8(byteOffset).unsigned
fun Buffer.getU16LE(byteOffset: Int): Int = getS16LE(byteOffset).unsigned
fun Buffer.getU32LE(byteOffset: Int): Long = getS32LE(byteOffset).unsigned
fun Buffer.getU16BE(byteOffset: Int): Int = getS16BE(byteOffset).unsigned
fun Buffer.getU32BE(byteOffset: Int): Long = getS32BE(byteOffset).unsigned

inline fun Buffer.getU8(byteOffset: Int, littleEndian: Boolean = true): Int = getU8(byteOffset)
inline fun Buffer.getU16(byteOffset: Int, littleEndian: Boolean = true): Int = if (littleEndian) getU16LE(byteOffset) else getU16BE(byteOffset)
inline fun Buffer.getU32(byteOffset: Int, littleEndian: Boolean = true): Long = if (littleEndian) getU32LE(byteOffset) else getU32BE(byteOffset)

inline fun Buffer.getS16(byteOffset: Int, littleEndian: Boolean = true): Short = if (littleEndian) getS16LE(byteOffset) else getS16BE(byteOffset)
inline fun Buffer.getS32(byteOffset: Int, littleEndian: Boolean = true): Int = if (littleEndian) getS32LE(byteOffset) else getS32BE(byteOffset)
inline fun Buffer.getS64(byteOffset: Int, littleEndian: Boolean = true): Long = if (littleEndian) getS64LE(byteOffset) else getS64BE(byteOffset)
inline fun Buffer.getF32(byteOffset: Int, littleEndian: Boolean = true): Float = if (littleEndian) getF32LE(byteOffset) else getF32BE(byteOffset)
inline fun Buffer.getF64(byteOffset: Int, littleEndian: Boolean = true): Double = if (littleEndian) getF64LE(byteOffset) else getF64BE(byteOffset)

inline fun Buffer.set16(byteOffset: Int, value: Short, littleEndian: Boolean = true): Unit = if (littleEndian) set16LE(byteOffset, value) else set16BE(byteOffset, value)
inline fun Buffer.set32(byteOffset: Int, value: Int, littleEndian: Boolean = true): Unit = if (littleEndian) set32LE(byteOffset, value) else set32BE(byteOffset, value)
inline fun Buffer.set64(byteOffset: Int, value: Long, littleEndian: Boolean = true): Unit = if (littleEndian) set64LE(byteOffset, value) else set64BE(byteOffset, value)
inline fun Buffer.setF32(byteOffset: Int, value: Float, littleEndian: Boolean = true): Unit = if (littleEndian) setF32LE(byteOffset, value) else setF32BE(byteOffset, value)
inline fun Buffer.setF64(byteOffset: Int, value: Double, littleEndian: Boolean = true): Unit = if (littleEndian) setF64LE(byteOffset, value) else setF64BE(byteOffset, value)

fun Buffer.set8Clamped(byteOffset: Int, value: Int): Unit = set8(byteOffset, value.clampUByte().toByte())
fun Buffer.set16LEClamped(byteOffset: Int, value: Int): Unit = set16LE(byteOffset, value.clampUShort().toShort())
fun Buffer.set16BEClamped(byteOffset: Int, value: Int): Unit = set16BE(byteOffset, value.clampUShort().toShort())
inline fun Buffer.set16Clamped(byteOffset: Int, value: Int, littleEndian: Boolean = true): Unit = if (littleEndian) set16LEClamped(byteOffset, value) else set16BEClamped(byteOffset, value)

inline private fun <T> _getArray(byteOffset: Int, out: T, start: Int, size: Int, elementBytes: Int, set: (Int, Int) -> Unit): T {
    for (n in 0 until size) set(start + n, byteOffset + n * elementBytes)
    return out
}

fun Buffer.getS8Array(byteOffset: Int, out: ByteArray, start: Int = 0, size: Int = out.size - start): ByteArray = out.also { arraycopy(this, byteOffset, out, start, size) }
fun Buffer.getS16ArrayLE(byteOffset: Int, out: ShortArray, start: Int = 0, size: Int = out.size - start): ShortArray = _getArray(byteOffset, out, start, size, 2) { index, offset -> out[index] = getS16LE(offset) }
fun Buffer.getS32ArrayLE(byteOffset: Int, out: IntArray, start: Int = 0, size: Int = out.size - start): IntArray = _getArray(byteOffset, out, start, size, 4) { index, offset -> out[index] = getS32LE(offset) }
fun Buffer.getS64ArrayLE(byteOffset: Int, out: LongArray, start: Int = 0, size: Int = out.size - start): LongArray = _getArray(byteOffset, out, start, size, 8) { index, offset -> out[index] = getS64LE(offset) }
fun Buffer.getF32ArrayLE(byteOffset: Int, out: FloatArray, start: Int = 0, size: Int = out.size - start): FloatArray = _getArray(byteOffset, out, start, size, 4) { index, offset -> out[index] = getF32LE(offset) }
fun Buffer.getF64ArrayLE(byteOffset: Int, out: DoubleArray, start: Int = 0, size: Int = out.size - start): DoubleArray = _getArray(byteOffset, out, start, size, 8) { index, offset -> out[index] = getF64LE(offset) }
fun Buffer.getS16ArrayBE(byteOffset: Int, out: ShortArray, start: Int = 0, size: Int = out.size - start): ShortArray = _getArray(byteOffset, out, start, size, 2) { index, offset -> out[index] = getS16BE(offset) }
fun Buffer.getS32ArrayBE(byteOffset: Int, out: IntArray, start: Int = 0, size: Int = out.size - start): IntArray = _getArray(byteOffset, out, start, size, 4) { index, offset -> out[index] = getS32BE(offset) }
fun Buffer.getS64ArrayBE(byteOffset: Int, out: LongArray, start: Int = 0, size: Int = out.size - start): LongArray = _getArray(byteOffset, out, start, size, 8) { index, offset -> out[index] = getS64BE(offset) }
fun Buffer.getF32ArrayBE(byteOffset: Int, out: FloatArray, start: Int = 0, size: Int = out.size - start): FloatArray = _getArray(byteOffset, out, start, size, 4) { index, offset -> out[index] = getF32BE(offset) }
fun Buffer.getF64ArrayBE(byteOffset: Int, out: DoubleArray, start: Int = 0, size: Int = out.size - start): DoubleArray = _getArray(byteOffset, out, start, size, 8) { index, offset -> out[index] = getF64BE(offset) }
fun Buffer.getS16Array(byteOffset: Int, out: ShortArray, start: Int = 0, size: Int = out.size - start, littleEndian: Boolean = true): ShortArray = if (littleEndian) getS16ArrayLE(byteOffset, out, start, size) else getS16ArrayBE(byteOffset, out, start, size)
fun Buffer.getS32Array(byteOffset: Int, out: IntArray, start: Int = 0, size: Int = out.size - start, littleEndian: Boolean = true): IntArray = if (littleEndian) getS32ArrayLE(byteOffset, out, start, size) else getS32ArrayBE(byteOffset, out, start, size)
fun Buffer.getS64Array(byteOffset: Int, out: LongArray, start: Int = 0, size: Int = out.size - start, littleEndian: Boolean = true): LongArray = if (littleEndian) getS64ArrayLE(byteOffset, out, start, size) else getS64ArrayBE(byteOffset, out, start, size)
fun Buffer.getF32Array(byteOffset: Int, out: FloatArray, start: Int = 0, size: Int = out.size - start, littleEndian: Boolean = true): FloatArray = if (littleEndian) getF32ArrayLE(byteOffset, out, start, size) else getF32ArrayBE(byteOffset, out, start, size)
fun Buffer.getF64Array(byteOffset: Int, out: DoubleArray, start: Int = 0, size: Int = out.size - start, littleEndian: Boolean = true): DoubleArray = if (littleEndian) getF64ArrayLE(byteOffset, out, start, size) else getF64ArrayBE(byteOffset, out, start, size)

fun Buffer.getS8Array(byteOffset: Int, size: Int): ByteArray = getS8Array(byteOffset, ByteArray(size))
fun Buffer.getS16ArrayLE(byteOffset: Int, size: Int): ShortArray = getS16ArrayLE(byteOffset, ShortArray(size))
fun Buffer.getS32ArrayLE(byteOffset: Int, size: Int): IntArray = getS32ArrayLE(byteOffset, IntArray(size))
fun Buffer.getS64ArrayLE(byteOffset: Int, size: Int): LongArray = getS64ArrayLE(byteOffset, LongArray(size))
fun Buffer.getF32ArrayLE(byteOffset: Int, size: Int): FloatArray = getF32ArrayLE(byteOffset, FloatArray(size))
fun Buffer.getF64ArrayLE(byteOffset: Int, size: Int): DoubleArray = getF64ArrayLE(byteOffset, DoubleArray(size))
fun Buffer.getS16ArrayBE(byteOffset: Int, size: Int): ShortArray = getS16ArrayBE(byteOffset, ShortArray(size))
fun Buffer.getS32ArrayBE(byteOffset: Int, size: Int): IntArray = getS32ArrayBE(byteOffset, IntArray(size))
fun Buffer.getS64ArrayBE(byteOffset: Int, size: Int): LongArray = getS64ArrayBE(byteOffset, LongArray(size))
fun Buffer.getF32ArrayBE(byteOffset: Int, size: Int): FloatArray = getF32ArrayBE(byteOffset, FloatArray(size))
fun Buffer.getF64ArrayBE(byteOffset: Int, size: Int): DoubleArray = getF64ArrayBE(byteOffset, DoubleArray(size))
fun Buffer.getS16Array(byteOffset: Int, size: Int, littleEndian: Boolean = true): ShortArray = if (littleEndian) getS16ArrayLE(byteOffset, size) else getS16ArrayBE(byteOffset, size)
fun Buffer.getS32Array(byteOffset: Int, size: Int, littleEndian: Boolean = true): IntArray = if (littleEndian) getS32ArrayLE(byteOffset, size) else getS32ArrayBE(byteOffset, size)
fun Buffer.getS64Array(byteOffset: Int, size: Int, littleEndian: Boolean = true): LongArray = if (littleEndian) getS64ArrayLE(byteOffset, size) else getS64ArrayBE(byteOffset, size)
fun Buffer.getF32Array(byteOffset: Int, size: Int, littleEndian: Boolean = true): FloatArray = if (littleEndian) getF32ArrayLE(byteOffset, size) else getF32ArrayBE(byteOffset, size)
fun Buffer.getF64Array(byteOffset: Int, size: Int, littleEndian: Boolean = true): DoubleArray = if (littleEndian) getF64ArrayLE(byteOffset, size) else getF64ArrayBE(byteOffset, size)

private inline fun _setArray(byteOffset: Int, start: Int, size: Int, elementBytes: Int, set: (Int, Int) -> Unit) {
    for (n in 0 until size) {
        set(byteOffset + n * elementBytes, start + n)
    }
}

fun Buffer.setArray(byteOffset: Int, data: ByteArray, start: Int = 0, size: Int = data.size - start): Unit { arraycopy(data, start, this, byteOffset, size) }
fun Buffer.setArrayLE(byteOffset: Int, data: ShortArray, start: Int = 0, size: Int = data.size - start): Unit = _setArray(byteOffset, start, size, 2) { offset, index -> set16LE(offset, data[index]) }
fun Buffer.setArrayLE(byteOffset: Int, data: IntArray, start: Int = 0, size: Int = data.size - start): Unit = _setArray(byteOffset, start, size, 4) { offset, index -> set32LE(offset, data[index]) }
fun Buffer.setArrayLE(byteOffset: Int, data: LongArray, start: Int = 0, size: Int = data.size - start): Unit = _setArray(byteOffset, start, size, 8) { offset, index -> set64LE(offset, data[index]) }
fun Buffer.setArrayLE(byteOffset: Int, data: FloatArray, start: Int = 0, size: Int = data.size - start): Unit = _setArray(byteOffset, start, size, 4) { offset, index -> setF32LE(offset, data[index]) }
fun Buffer.setArrayLE(byteOffset: Int, data: DoubleArray, start: Int = 0, size: Int = data.size - start): Unit = _setArray(byteOffset, start, size, 8) { offset, index -> setF64LE(offset, data[index]) }
fun Buffer.setArrayBE(byteOffset: Int, data: ShortArray, start: Int = 0, size: Int = data.size - start): Unit = _setArray(byteOffset, start, size, 2) { offset, index -> set16BE(offset, data[index]) }
fun Buffer.setArrayBE(byteOffset: Int, data: IntArray, start: Int = 0, size: Int = data.size - start): Unit = _setArray(byteOffset, start, size, 4) { offset, index -> set32BE(offset, data[index]) }
fun Buffer.setArrayBE(byteOffset: Int, data: LongArray, start: Int = 0, size: Int = data.size - start): Unit = _setArray(byteOffset, start, size, 8) { offset, index -> set64BE(offset, data[index]) }
fun Buffer.setArrayBE(byteOffset: Int, data: FloatArray, start: Int = 0, size: Int = data.size - start): Unit = _setArray(byteOffset, start, size, 4) { offset, index -> setF32BE(offset, data[index]) }
fun Buffer.setArrayBE(byteOffset: Int, data: DoubleArray, start: Int = 0, size: Int = data.size - start): Unit = _setArray(byteOffset, start, size, 8) { offset, index -> setF64BE(offset, data[index]) }
fun Buffer.setArray(byteOffset: Int, data: ShortArray, start: Int = 0, size: Int = data.size - start, littleEndian: Boolean = true): Unit = if (littleEndian) setArrayLE(byteOffset, data, start, size) else setArrayBE(byteOffset, data, start, size)
fun Buffer.setArray(byteOffset: Int, data: IntArray, start: Int = 0, size: Int = data.size - start, littleEndian: Boolean = true): Unit = if (littleEndian) setArrayLE(byteOffset, data, start, size) else setArrayBE(byteOffset, data, start, size)
fun Buffer.setArray(byteOffset: Int, data: LongArray, start: Int = 0, size: Int = data.size - start, littleEndian: Boolean = true): Unit = if (littleEndian) setArrayLE(byteOffset, data, start, size) else setArrayBE(byteOffset, data, start, size)
fun Buffer.setArray(byteOffset: Int, data: FloatArray, start: Int = 0, size: Int = data.size - start, littleEndian: Boolean = true): Unit = if (littleEndian) setArrayLE(byteOffset, data, start, size) else setArrayBE(byteOffset, data, start, size)
fun Buffer.setArray(byteOffset: Int, data: DoubleArray, start: Int = 0, size: Int = data.size - start, littleEndian: Boolean = true): Unit = if (littleEndian) setArrayLE(byteOffset, data, start, size) else setArrayBE(byteOffset, data, start, size)

@Deprecated("", ReplaceWith("getS8(byteOffset)"))
fun Buffer.getUnalignedInt8(byteOffset: Int): Byte = getS8(byteOffset)
@Deprecated("", ReplaceWith("getS16(byteOffset)"))
fun Buffer.getUnalignedInt16(byteOffset: Int): Short = getS16(byteOffset)
@Deprecated("", ReplaceWith("getS32(byteOffset)"))
fun Buffer.getUnalignedInt32(byteOffset: Int): Int = getS32(byteOffset)
@Deprecated("", ReplaceWith("getS64(byteOffset)"))
fun Buffer.getUnalignedInt64(byteOffset: Int): Long = getS64(byteOffset)
@Deprecated("", ReplaceWith("getF32(byteOffset)"))
fun Buffer.getUnalignedFloat32(byteOffset: Int): Float = getF32(byteOffset)
@Deprecated("", ReplaceWith("getF64(byteOffset)"))
fun Buffer.getUnalignedFloat64(byteOffset: Int): Double = getF64(byteOffset)
@Deprecated("", ReplaceWith("set8(byteOffset, value)"))
fun Buffer.setUnalignedInt8(byteOffset: Int, value: Byte): Unit = set8(byteOffset, value)
@Deprecated("", ReplaceWith("set16(byteOffset, value)"))
fun Buffer.setUnalignedInt16(byteOffset: Int, value: Short): Unit = set16(byteOffset, value)
@Deprecated("", ReplaceWith("set32(byteOffset, value)"))
fun Buffer.setUnalignedInt32(byteOffset: Int, value: Int): Unit = set32(byteOffset, value)
@Deprecated("", ReplaceWith("set64(byteOffset, value)"))
fun Buffer.setUnalignedInt64(byteOffset: Int, value: Long): Unit = set64(byteOffset, value)
@Deprecated("", ReplaceWith("setF32(byteOffset, value)"))
fun Buffer.setUnalignedFloat32(byteOffset: Int, value: Float): Unit = setF32(byteOffset, value)
@Deprecated("", ReplaceWith("setF64(byteOffset, value)"))
fun Buffer.setUnalignedFloat64(byteOffset: Int, value: Double): Unit = setF64(byteOffset, value)

// Unaligned versions

@Deprecated("", ReplaceWith("getU8(byteOffset)"))
fun Buffer.getUnalignedUInt8(byteOffset: Int): Int = getU8(byteOffset)
@Deprecated("", ReplaceWith("getU16(byteOffset)"))
fun Buffer.getUnalignedUInt16(byteOffset: Int): Int = getU16(byteOffset)

@Deprecated("", ReplaceWith("set8(byteOffset, value.toByte())"))
fun Buffer.setUnalignedUInt8(byteOffset: Int, value: Int) = set8(byteOffset, value.toByte())
@Deprecated("", ReplaceWith("set8Clamped(byteOffset, value)"))
fun Buffer.setUnalignedUInt8Clamped(byteOffset: Int, value: Int) = set8Clamped(byteOffset, value)
@Deprecated("", ReplaceWith("set16(byteOffset, value.toShort())"))
fun Buffer.setUnalignedUInt16(byteOffset: Int, value: Int) = set16(byteOffset, value.toShort())
@Deprecated("", ReplaceWith("set8(byteOffset, value.toByte())"))
fun Buffer.setUnalignedInt8(byteOffset: Int, value: Int) = set8(byteOffset, value.toByte())

// Array versions

@Deprecated("", ReplaceWith("getS8Array(byteOffset, out, offset, size)"))
fun Buffer.getUnalignedArrayInt8(byteOffset: Int, out: ByteArray, offset: Int = 0, size: Int = out.size - offset): ByteArray = getS8Array(byteOffset, out, offset, size)
@Deprecated("", ReplaceWith("getS16Array(byteOffset, out, offset, size)"))
fun Buffer.getUnalignedArrayInt16(byteOffset: Int, out: ShortArray, offset: Int = 0, size: Int = out.size - offset): ShortArray = getS16Array(byteOffset, out, offset, size)
@Deprecated("", ReplaceWith("getS32Array(byteOffset, out, offset, size)"))
fun Buffer.getUnalignedArrayInt32(byteOffset: Int, out: IntArray, offset: Int = 0, size: Int = out.size - offset): IntArray = getS32Array(byteOffset, out, offset, size)
@Deprecated("", ReplaceWith("getS64Array(byteOffset, out, offset, size)"))
fun Buffer.getUnalignedArrayInt64(byteOffset: Int, out: LongArray, offset: Int = 0, size: Int = out.size - offset): LongArray = getS64Array(byteOffset, out, offset, size)
@Deprecated("", ReplaceWith("getF32Array(byteOffset, out, offset, size)"))
fun Buffer.getUnalignedArrayFloat32(byteOffset: Int, out: FloatArray, offset: Int = 0, size: Int = out.size - offset): FloatArray = getF32Array(byteOffset, out, offset, size)
@Deprecated("", ReplaceWith("getF64Array(byteOffset, out, offset, size)"))
fun Buffer.getUnalignedArrayFloat64(byteOffset: Int, out: DoubleArray, offset: Int = 0, size: Int = out.size - offset): DoubleArray = getF64Array(byteOffset, out, offset, size)

@Deprecated("", ReplaceWith("setArray(byteOffset, inp, offset, size)"))
fun Buffer.setUnalignedArrayInt8(byteOffset: Int, inp: ByteArray, offset: Int = 0, size: Int = inp.size - offset) = setArray(byteOffset, inp, offset, size)
@Deprecated("", ReplaceWith("setArray(byteOffset, inp, offset, size)"))
fun Buffer.setUnalignedArrayInt16(byteOffset: Int, inp: ShortArray, offset: Int = 0, size: Int = inp.size - offset) = setArray(byteOffset, inp, offset, size)
@Deprecated("", ReplaceWith("setArray(byteOffset, inp, offset, size)"))
fun Buffer.setUnalignedArrayInt32(byteOffset: Int, inp: IntArray, offset: Int = 0, size: Int = inp.size - offset) = setArray(byteOffset, inp, offset, size)
@Deprecated("", ReplaceWith("setArray(byteOffset, inp, offset, size)"))
fun Buffer.setUnalignedArrayInt64(byteOffset: Int, inp: LongArray, offset: Int = 0, size: Int = inp.size - offset) = setArray(byteOffset, inp, offset, size)
@Deprecated("", ReplaceWith("setArray(byteOffset, inp, offset, size)"))
fun Buffer.setUnalignedArrayFloat32(byteOffset: Int, inp: FloatArray, offset: Int = 0, size: Int = inp.size - offset) = setArray(byteOffset, inp, offset, size)
@Deprecated("", ReplaceWith("setArray(byteOffset, inp, offset, size)"))
fun Buffer.setUnalignedArrayFloat64(byteOffset: Int, inp: DoubleArray, offset: Int = 0, size: Int = inp.size - offset) = setArray(byteOffset, inp, offset, size)

@Deprecated("", ReplaceWith("getU8(index)"))
fun Buffer.getUInt8(index: Int): Int = getU8(index)
@Deprecated("", ReplaceWith("getU16LE(index * Short.SIZE_BYTES)"))
fun Buffer.getUInt16(index: Int): Int = getU16LE(index * Short.SIZE_BYTES)
@Deprecated("", ReplaceWith("getS8(index)"))
fun Buffer.getInt8(index: Int): Byte = getS8(index)
@Deprecated("", ReplaceWith("getS16LE(index * Short.SIZE_BYTES)"))
fun Buffer.getInt16(index: Int): Short = getS16LE(index * Short.SIZE_BYTES)
@Deprecated("", ReplaceWith("getS32LE(index * Int.SIZE_BYTES)"))
fun Buffer.getInt32(index: Int): Int = getS32LE(index * Int.SIZE_BYTES)
@Deprecated("", ReplaceWith("getS64LE(index * Long.SIZE_BYTES)"))
fun Buffer.getInt64(index: Int): Long = getS64LE(index * Long.SIZE_BYTES)
@Deprecated("", ReplaceWith("getF32LE(index * Float.SIZE_BYTES)"))
fun Buffer.getFloat32(index: Int): Float = getF32LE(index * Float.SIZE_BYTES)
@Deprecated("", ReplaceWith("getF64LE(index * Double.SIZE_BYTES)"))
fun Buffer.getFloat64(index: Int): Double = getF64LE(index * Double.SIZE_BYTES)

@Deprecated("", ReplaceWith("set8(index, value.toByte())"))
fun Buffer.setUInt8(index: Int, value: Int) = set8(index, value.toByte())
@Deprecated("", ReplaceWith("set8Clamped(index, value)"))
fun Buffer.setUInt8Clamped(index: Int, value: Int) = set8Clamped(index, value)
@Deprecated("", ReplaceWith("set16LE(index * Short.SIZE_BYTES, value.toShort())"))
fun Buffer.setUInt16(index: Int, value: Int) = set16LE(index * Short.SIZE_BYTES, value.toShort())
@Deprecated("", ReplaceWith("set8(index, value)"))
fun Buffer.setInt8(index: Int, value: Byte) = set8(index, value)
@Deprecated("", ReplaceWith("set16LE(index, value.toShort())"))
fun Buffer.setInt8(index: Int, value: Int) = set16LE(index, value.toShort())
@Deprecated("", ReplaceWith("set16LE(index * Short.SIZE_BYTES, value)"))
fun Buffer.setInt16(index: Int, value: Short) = set16LE(index * Short.SIZE_BYTES, value)
@Deprecated("", ReplaceWith("set32LE(index * Int.SIZE_BYTES, value)"))
fun Buffer.setInt32(index: Int, value: Int) = set32LE(index * Int.SIZE_BYTES, value)
@Deprecated("", ReplaceWith("set64LE(index * Long.SIZE_BYTES, value)"))
fun Buffer.setInt64(index: Int, value: Long) = set64LE(index * Long.SIZE_BYTES, value)
@Deprecated("", ReplaceWith("setF32LE(index * Float.SIZE_BYTES, value)"))
fun Buffer.setFloat32(index: Int, value: Float) = setF32LE(index * Float.SIZE_BYTES, value)
@Deprecated("", ReplaceWith("setF64LE(index * Double.SIZE_BYTES, value)"))
fun Buffer.setFloat64(index: Int, value: Double) = setF64LE(index * Double.SIZE_BYTES, value)

// ALIGNED ARRAYS

@Deprecated("", ReplaceWith("UByteArrayInt(getS8Array(index * Byte.SIZE_BYTES, out.data, offset, size))", "korlibs.memory.UByteArrayInt"))
fun Buffer.getArrayUInt8(index: Int, out: UByteArrayInt, offset: Int = 0, size: Int = out.size - offset): UByteArrayInt = UByteArrayInt(getS8Array(index * Byte.SIZE_BYTES, out.data, offset, size))
@Deprecated("", ReplaceWith("UShortArrayInt(getS16Array(index * Short.SIZE_BYTES, out.data, offset, size))", "korlibs.memory.UShortArrayInt"))
fun Buffer.getArrayUInt16(index: Int, out: UShortArrayInt, offset: Int = 0, size: Int = out.size - offset): UShortArrayInt = UShortArrayInt(getS16Array(index * Short.SIZE_BYTES, out.data, offset, size))
@Deprecated("", ReplaceWith("getS8Array(index * Byte.SIZE_BYTES, out, offset, size)"))
fun Buffer.getArrayInt8(index: Int, out: ByteArray, offset: Int = 0, size: Int = out.size - offset): ByteArray = getS8Array(index * Byte.SIZE_BYTES, out, offset, size)
@Deprecated("", ReplaceWith("getS16Array(index * Short.SIZE_BYTES, out, offset, size)"))
fun Buffer.getArrayInt16(index: Int, out: ShortArray, offset: Int = 0, size: Int = out.size - offset): ShortArray = getS16Array(index * Short.SIZE_BYTES, out, offset, size)
@Deprecated("", ReplaceWith("getS32Array(index * Int.SIZE_BYTES, out, offset, size)"))
fun Buffer.getArrayInt32(index: Int, out: IntArray, offset: Int = 0, size: Int = out.size - offset): IntArray = getS32Array(index * Int.SIZE_BYTES, out, offset, size)
@Deprecated("", ReplaceWith("getS64Array(index * Long.SIZE_BYTES, out, offset, size)"))
fun Buffer.getArrayInt64(index: Int, out: LongArray, offset: Int = 0, size: Int = out.size - offset): LongArray = getS64Array(index * Long.SIZE_BYTES, out, offset, size)
@Deprecated("", ReplaceWith("getF32Array(index * Float.SIZE_BYTES, out, offset, size)"))
fun Buffer.getArrayFloat32(index: Int, out: FloatArray, offset: Int = 0, size: Int = out.size - offset): FloatArray = getF32Array(index * Float.SIZE_BYTES, out, offset, size)
@Deprecated("", ReplaceWith("getF64Array(index * Double.SIZE_BYTES, out, offset, size)"))
fun Buffer.getArrayFloat64(index: Int, out: DoubleArray, offset: Int = 0, size: Int = out.size - offset): DoubleArray = getF64Array(index * Double.SIZE_BYTES, out, offset, size)

@Deprecated("", ReplaceWith("setArray(index * Byte.SIZE_BYTES, inp.data, offset, size)"))
fun Buffer.setArrayUInt8(index: Int, inp: UByteArrayInt, offset: Int = 0, size: Int = inp.size - offset): Unit = setArray(index * Byte.SIZE_BYTES, inp.data, offset, size)
@Deprecated("", ReplaceWith("setArray(index * Short.SIZE_BYTES, inp.data, offset, size)"))
fun Buffer.setArrayUInt16(index: Int, inp: UShortArrayInt, offset: Int = 0, size: Int = inp.size - offset): Unit = setArray(index * Short.SIZE_BYTES, inp.data, offset, size)
@Deprecated("", ReplaceWith("setArray(index * Byte.SIZE_BYTES, inp, offset, size)"))
fun Buffer.setArrayInt8(index: Int, inp: ByteArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setArray(index * Byte.SIZE_BYTES, inp, offset, size)
@Deprecated("", ReplaceWith("setArray(index * Short.SIZE_BYTES, inp, offset, size)"))
fun Buffer.setArrayInt16(index: Int, inp: ShortArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setArray(index * Short.SIZE_BYTES, inp, offset, size)
@Deprecated("", ReplaceWith("setArray(index * Int.SIZE_BYTES, inp, offset, size)"))
fun Buffer.setArrayInt32(index: Int, inp: IntArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setArray(index * Int.SIZE_BYTES, inp, offset, size)
@Deprecated("", ReplaceWith("setArray(index * Long.SIZE_BYTES, inp, offset, size)"))
fun Buffer.setArrayInt64(index: Int, inp: LongArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setArray(index * Long.SIZE_BYTES, inp, offset, size)
@Deprecated("", ReplaceWith("setArray(index * Float.SIZE_BYTES, inp, offset, size)"))
fun Buffer.setArrayFloat32(index: Int, inp: FloatArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setArray(index * Float.SIZE_BYTES, inp, offset, size)
@Deprecated("", ReplaceWith("setArray(index * Double.SIZE_BYTES, inp, offset, size)"))
fun Buffer.setArrayFloat64(index: Int, inp: DoubleArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setArray(index * Double.SIZE_BYTES, inp, offset, size)

interface BaseBuffer {
    val size: Int
}

interface TypedBuffer : BaseBuffer {
    val buffer: Buffer
    override val size: Int get() = buffer.sizeInBytes / elementSizeInBytes
    val elementSizeInBytes: Int
}

interface BaseIntBuffer : BaseBuffer {
    operator fun get(index: Int): Int
    operator fun set(index: Int, value: Int)
}

interface BaseFloatBuffer : BaseBuffer {
    operator fun get(index: Int): Float
    operator fun set(index: Int, value: Float)
}

fun BaseIntBuffer.toIntArray(): IntArray = IntArray(size) { this[it] }
fun BaseFloatBuffer.toFloatArray(): FloatArray = FloatArray(size) { this[it] }

@JvmInline
value class IntArrayBuffer(val array: IntArray) : BaseIntBuffer {
    override val size: Int get() = array.size
    override operator fun get(index: Int): Int = array[index]
    override operator fun set(index: Int, value: Int) { array[index] = value }
}

@JvmInline
value class FloatArrayBuffer(val array: FloatArray) : BaseFloatBuffer {
    override val size: Int get() = array.size
    override operator fun get(index: Int): Float = array[index]
    override operator fun set(index: Int, value: Float) { array[index] = value }
}

@JvmInline
value class Int8Buffer(override val buffer: Buffer) : TypedBuffer {
    companion object {
        const val ELEMENT_SIZE_IN_BYTES = 1
    }
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES, direct))
    constructor(data: ByteArray, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES).also { it.setArray(0, data, offset, size) })

    override val elementSizeInBytes: Int get() = ELEMENT_SIZE_IN_BYTES
    operator fun get(index: Int): Byte = buffer.getS8(index)
    operator fun set(index: Int, value: Byte) = buffer.set8(index, value)
    fun getArray(index: Int, out: ByteArray, offset: Int = 0, size: Int = out.size - offset): ByteArray = buffer.getS8Array(index * ELEMENT_SIZE_IN_BYTES, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): ByteArray = getArray(index, ByteArray(size))
    fun setArray(index: Int, inp: ByteArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArray(index * ELEMENT_SIZE_IN_BYTES, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Int8Buffer = Int8Buffer(buffer.sliceBuffer(start * ELEMENT_SIZE_IN_BYTES, end * ELEMENT_SIZE_IN_BYTES))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Int8Buffer = slice(start, start + size)
}

@JvmInline
value class Int16Buffer(override val buffer: Buffer) : TypedBuffer {
    companion object {
        const val ELEMENT_SIZE_IN_BYTES = 2
    }
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES, direct))
    constructor(data: ShortArray, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES).also { it.setArray(0, data, offset, size) })

    override val elementSizeInBytes: Int get() = ELEMENT_SIZE_IN_BYTES
    operator fun get(index: Int): Short = buffer.getS16(index * ELEMENT_SIZE_IN_BYTES)
    operator fun set(index: Int, value: Short) = buffer.set16(index * ELEMENT_SIZE_IN_BYTES, value)
    fun getArray(index: Int, out: ShortArray, offset: Int = 0, size: Int = out.size - offset): ShortArray = buffer.getS16Array(index * Short.SIZE_BYTES, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): ShortArray = getArray(index, ShortArray(size))
    fun setArray(index: Int, inp: ShortArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArray(index * Short.SIZE_BYTES, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Int16Buffer = Int16Buffer(buffer.sliceBuffer(start * ELEMENT_SIZE_IN_BYTES, end * ELEMENT_SIZE_IN_BYTES))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Int16Buffer = slice(start, start + size)
}

@JvmInline
value class Uint8Buffer(override val buffer: Buffer) : TypedBuffer, BaseIntBuffer {
    companion object {
        const val ELEMENT_SIZE_IN_BYTES = 1
        operator fun invoke(data: ByteArray) = Uint8Buffer(UByteArrayInt(data))
        operator fun invoke(data: UByteArray) = Uint8Buffer(UByteArrayInt(data.toByteArray()))
    }
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES, direct))
    constructor(data: UByteArrayInt, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size).also { it.setArray(0, data.data, offset, size) })

    override val elementSizeInBytes: Int get() = ELEMENT_SIZE_IN_BYTES
    override operator fun get(index: Int): Int = buffer.getU8(index)
    override operator fun set(index: Int, value: Int) = buffer.set8(index, value.toByte())
    fun getArray(index: Int, out: UByteArrayInt, offset: Int = 0, size: Int = out.size - offset): UByteArrayInt = UByteArrayInt(buffer.getS8Array(index * ELEMENT_SIZE_IN_BYTES, out.data, offset, size))
    fun getArray(index: Int = 0, size: Int = this.size - index): UByteArrayInt = getArray(index, UByteArrayInt(size))
    fun setArray(index: Int, inp: UByteArrayInt, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArray(index * ELEMENT_SIZE_IN_BYTES, inp.data, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Uint8Buffer = Uint8Buffer(buffer.sliceBuffer(start * ELEMENT_SIZE_IN_BYTES, end * ELEMENT_SIZE_IN_BYTES))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Uint8Buffer = slice(start, start + size)
}

@JvmInline
value class Uint8ClampedBuffer(override val buffer: Buffer) : TypedBuffer, BaseIntBuffer {
    companion object {
        const val ELEMENT_SIZE_IN_BYTES = 1
        operator fun invoke(data: ByteArray) = Uint8ClampedBuffer(UByteArrayInt(data))
    }
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES, direct))
    constructor(data: UByteArrayInt, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size).also { it.setArray(0, data.data, offset, size) })

    override val elementSizeInBytes: Int get() = ELEMENT_SIZE_IN_BYTES
    override operator fun get(index: Int): Int = buffer.getU8(index)
    override operator fun set(index: Int, value: Int) = buffer.set8Clamped(index, value)

    fun slice(start: Int = 0, end: Int = this.size): Uint8ClampedBuffer = Uint8ClampedBuffer(buffer.sliceBuffer(start * ELEMENT_SIZE_IN_BYTES, end * ELEMENT_SIZE_IN_BYTES))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Uint8ClampedBuffer = slice(start, start + size)
}

@JvmInline
value class Uint16Buffer(override val buffer: Buffer) : TypedBuffer, BaseIntBuffer {
    companion object {
        const val ELEMENT_SIZE_IN_BYTES = 2
        operator fun invoke(data: ByteArray) = Uint8ClampedBuffer(UByteArrayInt(data))
    }
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES, direct))
    constructor(data: UShortArrayInt, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES).also { it.setArray(0, data.data, offset, size) })

    override val elementSizeInBytes: Int get() = ELEMENT_SIZE_IN_BYTES
    override operator fun get(index: Int): Int = buffer.getU16(index * ELEMENT_SIZE_IN_BYTES)
    override operator fun set(index: Int, value: Int) = buffer.set16(index * ELEMENT_SIZE_IN_BYTES, value.toShort())
    fun getArray(index: Int, out: UShortArrayInt, offset: Int = 0, size: Int = out.size - offset): UShortArrayInt = UShortArrayInt(buffer.getS16Array(index * ELEMENT_SIZE_IN_BYTES, out.data, offset, size))
    fun getArray(index: Int = 0, size: Int = this.size - index): UShortArrayInt = getArray(index, UShortArrayInt(size))
    fun setArray(index: Int, inp: UShortArrayInt, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArray(index * ELEMENT_SIZE_IN_BYTES, inp.data, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Uint16Buffer = Uint16Buffer(buffer.sliceBuffer(start * ELEMENT_SIZE_IN_BYTES, end * ELEMENT_SIZE_IN_BYTES))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Uint16Buffer = slice(start, start + size)
}

@JvmInline
value class Int32Buffer(override val buffer: Buffer) : TypedBuffer, BaseIntBuffer {
    companion object {
        const val ELEMENT_SIZE_IN_BYTES = 4
    }
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES, direct))
    constructor(data: IntArray, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES).also { it.setArray(0, data, offset, size) })

    override val elementSizeInBytes: Int get() = ELEMENT_SIZE_IN_BYTES
    override operator fun get(index: Int): Int = buffer.getS32(index * ELEMENT_SIZE_IN_BYTES)
    override operator fun set(index: Int, value: Int) = buffer.set32(index * ELEMENT_SIZE_IN_BYTES, value)
    fun getArray(index: Int, out: IntArray, offset: Int = 0, size: Int = out.size - offset): IntArray = buffer.getS32Array(index * ELEMENT_SIZE_IN_BYTES, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): IntArray = getArray(index, IntArray(size))
    fun setArray(index: Int, inp: IntArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArray(index * ELEMENT_SIZE_IN_BYTES, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Int32Buffer = Int32Buffer(buffer.sliceBuffer(start * ELEMENT_SIZE_IN_BYTES, end * ELEMENT_SIZE_IN_BYTES))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Int32Buffer = slice(start, start + size)
}

@JvmInline
value class Uint32Buffer(override val buffer: Buffer) : TypedBuffer {
    companion object {
        const val ELEMENT_SIZE_IN_BYTES = 4
    }
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES, direct))
    constructor(data: UIntArray, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES).also { it.setArray(0, data.toIntArray(), offset, size) })

    override val elementSizeInBytes: Int get() = ELEMENT_SIZE_IN_BYTES
    operator fun get(index: Int): UInt = buffer.getS32(index * ELEMENT_SIZE_IN_BYTES).toUInt()
    operator fun set(index: Int, value: UInt) = buffer.set32(index * ELEMENT_SIZE_IN_BYTES, value.toInt())
    operator fun set(index: Int, value: Int) = buffer.set32(index * ELEMENT_SIZE_IN_BYTES, value)
    fun getArray(index: Int, out: UIntArray, offset: Int = 0, size: Int = out.size - offset): UIntArray = buffer.getS32Array(index * ELEMENT_SIZE_IN_BYTES, out.asIntArray(), offset, size).asUIntArray()
    fun getArray(index: Int = 0, size: Int = this.size - index): UIntArray = getArray(index, UIntArray(size))
    fun setArray(index: Int, inp: UIntArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArray(index * ELEMENT_SIZE_IN_BYTES, inp.asIntArray(), offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Uint32Buffer = Uint32Buffer(buffer.sliceBuffer(start * ELEMENT_SIZE_IN_BYTES, end * ELEMENT_SIZE_IN_BYTES))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Uint32Buffer = slice(start, start + size)
}

@JvmInline
value class Int64Buffer(override val buffer: Buffer) : TypedBuffer {
    companion object {
        const val ELEMENT_SIZE_IN_BYTES = 8
    }
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES, direct))
    constructor(data: LongArray, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES).also { it.setArray(0, data, offset, size) })

    override val elementSizeInBytes: Int get() = ELEMENT_SIZE_IN_BYTES
    operator fun get(index: Int): Long = buffer.getS64(index * ELEMENT_SIZE_IN_BYTES)
    operator fun set(index: Int, value: Long) = buffer.set64(index * ELEMENT_SIZE_IN_BYTES, value)
    fun getArray(index: Int, out: LongArray, offset: Int = 0, size: Int = out.size - offset): LongArray = buffer.getS64Array(index * ELEMENT_SIZE_IN_BYTES, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): LongArray = getArray(index, LongArray(size))
    fun setArray(index: Int, inp: LongArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArray(index * ELEMENT_SIZE_IN_BYTES, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Int64Buffer = Int64Buffer(buffer.sliceBuffer(start * ELEMENT_SIZE_IN_BYTES, end * ELEMENT_SIZE_IN_BYTES))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Int64Buffer = slice(start, start + size)
}

@JvmInline
value class Float32Buffer(override val buffer: Buffer) : TypedBuffer, BaseFloatBuffer {
    companion object {
        const val ELEMENT_SIZE_IN_BYTES = 4
    }
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES, direct))
    constructor(data: FloatArray, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES).also { it.setArray(0, data, offset, size) })

    override val elementSizeInBytes: Int get() = ELEMENT_SIZE_IN_BYTES
    override operator fun get(index: Int): Float = buffer.getF32(index * ELEMENT_SIZE_IN_BYTES)
    override operator fun set(index: Int, value: Float) = buffer.setF32(index * ELEMENT_SIZE_IN_BYTES, value)
    fun getArray(index: Int, out: FloatArray, offset: Int = 0, size: Int = out.size - offset): FloatArray = buffer.getF32Array(index * ELEMENT_SIZE_IN_BYTES, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): FloatArray = getArray(index, FloatArray(size))
    fun setArray(index: Int, inp: FloatArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArray(index * ELEMENT_SIZE_IN_BYTES, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Float32Buffer = Float32Buffer(buffer.sliceBuffer(start * ELEMENT_SIZE_IN_BYTES, end * ELEMENT_SIZE_IN_BYTES))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Float32Buffer = slice(start, start + size)
}

@JvmInline
value class Float64Buffer(override val buffer: Buffer) : TypedBuffer {
    companion object {
        const val ELEMENT_SIZE_IN_BYTES = 8
    }
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES, direct))
    constructor(data: DoubleArray, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES).also { it.setArray(0, data, offset, size) })

    override val elementSizeInBytes: Int get() = ELEMENT_SIZE_IN_BYTES
    operator fun get(index: Int): Double = buffer.getF64(index * ELEMENT_SIZE_IN_BYTES)
    operator fun set(index: Int, value: Double) = buffer.setF64(index * ELEMENT_SIZE_IN_BYTES, value)
    fun getArray(index: Int, out: DoubleArray, offset: Int = 0, size: Int = out.size - offset): DoubleArray = buffer.getF64Array(index * ELEMENT_SIZE_IN_BYTES, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): DoubleArray = getArray(index, DoubleArray(size))
    fun setArray(index: Int, inp: DoubleArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArray(index * ELEMENT_SIZE_IN_BYTES, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Float64Buffer = Float64Buffer(buffer.sliceBuffer(start * ELEMENT_SIZE_IN_BYTES, end * ELEMENT_SIZE_IN_BYTES))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Float64Buffer = slice(start, start + size)
}

@Deprecated("", ReplaceWith("asUInt8()")) val Buffer.u8: Uint8Buffer get() = asUInt8()
@Deprecated("", ReplaceWith("asUInt16()")) val Buffer.u16: Uint16Buffer get() = asUInt16()
@Deprecated("", ReplaceWith("asInt8()")) val Buffer.i8: Int8Buffer get() = asInt8()
@Deprecated("", ReplaceWith("asInt16()")) val Buffer.i16: Int16Buffer get() = asInt16()
@Deprecated("", ReplaceWith("asInt32()")) val Buffer.i32: Int32Buffer get() = asInt32()
@Deprecated("", ReplaceWith("asInt64()")) val Buffer.i64: Int64Buffer get() = asInt64()
@Deprecated("", ReplaceWith("asFloat32()")) val Buffer.f32: Float32Buffer get() = asFloat32()
@Deprecated("", ReplaceWith("asFloat64()")) val Buffer.f64: Float64Buffer get() = asFloat64()

fun Buffer.asUInt8(): Uint8Buffer = Uint8Buffer(this)
fun Buffer.asUInt16(): Uint16Buffer = Uint16Buffer(this)
fun Buffer.asInt8(): Int8Buffer = Int8Buffer(this)
fun Buffer.asInt16(): Int16Buffer = Int16Buffer(this)
fun Buffer.asInt32(): Int32Buffer = Int32Buffer(this)
fun Buffer.asInt64(): Int64Buffer = Int64Buffer(this)
fun Buffer.asFloat32(): Float32Buffer = Float32Buffer(this)
fun Buffer.asFloat64(): Float64Buffer = Float64Buffer(this)

fun TypedBuffer.asUInt8(): Uint8Buffer = this.buffer.asUInt8()
fun TypedBuffer.asUInt16(): Uint16Buffer = this.buffer.asUInt16()
fun TypedBuffer.asInt8(): Int8Buffer = this.buffer.asInt8()
fun TypedBuffer.asInt16(): Int16Buffer = this.buffer.asInt16()
fun TypedBuffer.asInt32(): Int32Buffer = this.buffer.asInt32()
fun TypedBuffer.asInt64(): Int64Buffer = this.buffer.asInt64()
fun TypedBuffer.asFloat32(): Float32Buffer = this.buffer.asFloat32()
fun TypedBuffer.asFloat64(): Float64Buffer = this.buffer.asFloat64()

inline fun <T> BufferTemp(size: Int, callback: (Buffer) -> T): T = Buffer.allocDirect(size).run(callback)

fun ByteArray.toNBufferUInt8(): Uint8Buffer = Buffer(this).u8
