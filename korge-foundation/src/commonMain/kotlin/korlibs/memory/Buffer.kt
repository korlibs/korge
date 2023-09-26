@file:OptIn(ExperimentalUnsignedTypes::class)

package korlibs.memory

import korlibs.math.*
import kotlin.jvm.*

//typealias Buffer = NDataView

//expect class NDataView {

// @TODO: slice -> subarray
expect class Buffer {
    constructor(size: Int, direct: Boolean = false)
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
        h = 31 * h + buffer.getInt8(offset + n)
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
fun Buffer.sliceWithSize(start: Int, size: Int): Buffer = _slice(start, start + size)
fun Buffer._slice(start: Int = 0, end: Int = sizeInBytes): Buffer {
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

@Deprecated("", ReplaceWith("getS8(byteOffset)"))
fun Buffer.getUnalignedInt8(byteOffset: Int): Byte = getS8(byteOffset)
@Deprecated("", ReplaceWith("getS16LE(byteOffset)"))
fun Buffer.getUnalignedInt16(byteOffset: Int): Short = getS16LE(byteOffset)
@Deprecated("", ReplaceWith("getS32LE(byteOffset)"))
fun Buffer.getUnalignedInt32(byteOffset: Int): Int = getS32LE(byteOffset)
@Deprecated("", ReplaceWith("getS64LE(byteOffset)"))
fun Buffer.getUnalignedInt64(byteOffset: Int): Long = getS64LE(byteOffset)
@Deprecated("", ReplaceWith("getF32LE(byteOffset)"))
fun Buffer.getUnalignedFloat32(byteOffset: Int): Float = getF32LE(byteOffset)
@Deprecated("", ReplaceWith("getF64LE(byteOffset)"))
fun Buffer.getUnalignedFloat64(byteOffset: Int): Double = getF64LE(byteOffset)
@Deprecated("", ReplaceWith("set8(byteOffset, value)"))
fun Buffer.setUnalignedInt8(byteOffset: Int, value: Byte): Unit = set8(byteOffset, value)
@Deprecated("", ReplaceWith("set16LE(byteOffset, value)"))
fun Buffer.setUnalignedInt16(byteOffset: Int, value: Short): Unit = set16LE(byteOffset, value)
@Deprecated("", ReplaceWith("set32LE(byteOffset, value)"))
fun Buffer.setUnalignedInt32(byteOffset: Int, value: Int): Unit = set32LE(byteOffset, value)
@Deprecated("", ReplaceWith("set64LE(byteOffset, value)"))
fun Buffer.setUnalignedInt64(byteOffset: Int, value: Long): Unit = set64LE(byteOffset, value)
@Deprecated("", ReplaceWith("setF32LE(byteOffset, value)"))
fun Buffer.setUnalignedFloat32(byteOffset: Int, value: Float): Unit = setF32LE(byteOffset, value)
@Deprecated("", ReplaceWith("setF64LE(byteOffset, value)"))
fun Buffer.setUnalignedFloat64(byteOffset: Int, value: Double): Unit = setF64LE(byteOffset, value)

// Unaligned versions

@Deprecated("", ReplaceWith("getU8(byteOffset)"))
fun Buffer.getUnalignedUInt8(byteOffset: Int): Int = getU8(byteOffset)
@Deprecated("", ReplaceWith("getU16LE(byteOffset)"))
fun Buffer.getUnalignedUInt16(byteOffset: Int): Int = getU16LE(byteOffset)

@Deprecated("", ReplaceWith("set8(byteOffset, value.toByte())"))
fun Buffer.setUnalignedUInt8(byteOffset: Int, value: Int) = set8(byteOffset, value.toByte())
@Deprecated("", ReplaceWith("set8Clamped(byteOffset, value)"))
fun Buffer.setUnalignedUInt8Clamped(byteOffset: Int, value: Int) = set8Clamped(byteOffset, value)
@Deprecated("", ReplaceWith("set16LE(byteOffset, value.toShort())"))
fun Buffer.setUnalignedUInt16(byteOffset: Int, value: Int) = set16LE(byteOffset, value.toShort())
@Deprecated("", ReplaceWith("set8(byteOffset, value.toByte())"))
fun Buffer.setUnalignedInt8(byteOffset: Int, value: Int) = set8(byteOffset, value.toByte())

// Array versions

@Deprecated("")
fun Buffer.getUnalignedArrayInt8(byteOffset: Int, out: ByteArray, offset: Int = 0, size: Int = out.size - offset): ByteArray { for (n in 0 until size) out[offset + n] =
    getS8(byteOffset + n * 1); return out }
@Deprecated("")
fun Buffer.getUnalignedArrayInt16(byteOffset: Int, out: ShortArray, offset: Int = 0, size: Int = out.size - offset): ShortArray { for (n in 0 until size) out[offset + n] =
    getS16LE(byteOffset + n * 2); return out }
@Deprecated("")
fun Buffer.getUnalignedArrayInt32(byteOffset: Int, out: IntArray, offset: Int = 0, size: Int = out.size - offset): IntArray { for (n in 0 until size) out[offset + n] =
    getS32LE(byteOffset + n * 4); return out }
@Deprecated("")
fun Buffer.getUnalignedArrayInt64(byteOffset: Int, out: LongArray, offset: Int = 0, size: Int = out.size - offset): LongArray { for (n in 0 until size) out[offset + n] =
    getS64LE(byteOffset + n * 8); return out }
@Deprecated("")
fun Buffer.getUnalignedArrayFloat32(byteOffset: Int, out: FloatArray, offset: Int = 0, size: Int = out.size - offset): FloatArray { for (n in 0 until size) out[offset + n] =
    getF32LE(byteOffset + n * 4); return out }
@Deprecated("")
fun Buffer.getUnalignedArrayFloat64(byteOffset: Int, out: DoubleArray, offset: Int = 0, size: Int = out.size - offset): DoubleArray { for (n in 0 until size) out[offset + n] =
    getF64LE(byteOffset + n * 8); return out }

@Deprecated("")
fun Buffer.setUnalignedArrayInt8(byteOffset: Int, inp: ByteArray, offset: Int = 0, size: Int = inp.size - offset) { for (n in 0 until size) set8(byteOffset + n * 1, inp[offset + n])
}
@Deprecated("")
fun Buffer.setUnalignedArrayInt16(byteOffset: Int, inp: ShortArray, offset: Int = 0, size: Int = inp.size - offset) { for (n in 0 until size) set16LE(byteOffset + n * 2, inp[offset + n])
}
@Deprecated("")
fun Buffer.setUnalignedArrayInt32(byteOffset: Int, inp: IntArray, offset: Int = 0, size: Int = inp.size - offset) { for (n in 0 until size) set32LE(byteOffset + n * 4, inp[offset + n])
}
@Deprecated("")
fun Buffer.setUnalignedArrayInt64(byteOffset: Int, inp: LongArray, offset: Int = 0, size: Int = inp.size - offset) { for (n in 0 until size) set64LE(byteOffset + n * 8, inp[offset + n])
}
@Deprecated("")
fun Buffer.setUnalignedArrayFloat32(byteOffset: Int, inp: FloatArray, offset: Int = 0, size: Int = inp.size - offset) { for (n in 0 until size) setF32LE(byteOffset + n * 4, inp[offset + n])
}
@Deprecated("")
fun Buffer.setUnalignedArrayFloat64(byteOffset: Int, inp: DoubleArray, offset: Int = 0, size: Int = inp.size - offset) { for (n in 0 until size) setF64LE(byteOffset + n * 8, inp[offset + n])
}

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

@Deprecated("")
fun Buffer.getArrayUInt8(index: Int, out: UByteArrayInt, offset: Int = 0, size: Int = out.size - offset): UByteArrayInt = UByteArrayInt(getUnalignedArrayInt8(index * 1, out.data, offset, size))
@Deprecated("")
fun Buffer.getArrayUInt16(index: Int, out: UShortArrayInt, offset: Int = 0, size: Int = out.size - offset): UShortArrayInt = UShortArrayInt(getUnalignedArrayInt16(index * 2, out.data, offset, size))
@Deprecated("")
fun Buffer.getArrayInt8(index: Int, out: ByteArray, offset: Int = 0, size: Int = out.size - offset): ByteArray = getUnalignedArrayInt8(index * 1, out, offset, size)
@Deprecated("")
fun Buffer.getArrayInt16(index: Int, out: ShortArray, offset: Int = 0, size: Int = out.size - offset): ShortArray = getUnalignedArrayInt16(index * 2, out, offset, size)
@Deprecated("")
fun Buffer.getArrayInt32(index: Int, out: IntArray, offset: Int = 0, size: Int = out.size - offset): IntArray = getUnalignedArrayInt32(index * 4, out, offset, size)
@Deprecated("")
fun Buffer.getArrayInt64(index: Int, out: LongArray, offset: Int = 0, size: Int = out.size - offset): LongArray = getUnalignedArrayInt64(index * 8, out, offset, size)
@Deprecated("")
fun Buffer.getArrayFloat32(index: Int, out: FloatArray, offset: Int = 0, size: Int = out.size - offset): FloatArray = getUnalignedArrayFloat32(index * 4, out, offset, size)
@Deprecated("")
fun Buffer.getArrayFloat64(index: Int, out: DoubleArray, offset: Int = 0, size: Int = out.size - offset): DoubleArray = getUnalignedArrayFloat64(index * 8, out, offset, size)

@Deprecated("")
fun Buffer.setArrayUInt8(index: Int, inp: UByteArrayInt, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayInt8(index * 1, inp.data, offset, size)
@Deprecated("")
fun Buffer.setArrayUInt16(index: Int, inp: UShortArrayInt, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayInt16(index * 2, inp.data, offset, size)
@Deprecated("")
fun Buffer.setArrayInt8(index: Int, inp: ByteArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayInt8(index * 1, inp, offset, size)
@Deprecated("")
fun Buffer.setArrayInt16(index: Int, inp: ShortArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayInt16(index * 2, inp, offset, size)
@Deprecated("")
fun Buffer.setArrayInt32(index: Int, inp: IntArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayInt32(index * 4, inp, offset, size)
@Deprecated("")
fun Buffer.setArrayInt64(index: Int, inp: LongArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayInt64(index * 8, inp, offset, size)
@Deprecated("")
fun Buffer.setArrayFloat32(index: Int, inp: FloatArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayFloat32(index * 4, inp, offset, size)
@Deprecated("")
fun Buffer.setArrayFloat64(index: Int, inp: DoubleArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayFloat64(index * 8, inp, offset, size)

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
    constructor(data: ByteArray, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES).also { it.setArrayInt8(0, data, offset, size) })

    override val elementSizeInBytes: Int get() = ELEMENT_SIZE_IN_BYTES
    operator fun get(index: Int): Byte = buffer.getInt8(index)
    operator fun set(index: Int, value: Byte) = buffer.setInt8(index, value)
    fun getArray(index: Int, out: ByteArray, offset: Int = 0, size: Int = out.size - offset): ByteArray = buffer.getArrayInt8(index, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): ByteArray = getArray(index, ByteArray(size))
    fun setArray(index: Int, inp: ByteArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayInt8(index, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Int8Buffer = Int8Buffer(buffer._slice(start * ELEMENT_SIZE_IN_BYTES, end * ELEMENT_SIZE_IN_BYTES))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Int8Buffer = slice(start, start + size)
}

@JvmInline
value class Int16Buffer(override val buffer: Buffer) : TypedBuffer {
    companion object {
        const val ELEMENT_SIZE_IN_BYTES = 2
    }
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES, direct))
    constructor(data: ShortArray, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES).also { it.setArrayInt16(0, data, offset, size) })

    override val elementSizeInBytes: Int get() = ELEMENT_SIZE_IN_BYTES
    operator fun get(index: Int): Short = buffer.getS16LE(index * ELEMENT_SIZE_IN_BYTES)
    operator fun set(index: Int, value: Short) = buffer.set16LE(index * ELEMENT_SIZE_IN_BYTES, value)
    fun getArray(index: Int, out: ShortArray, offset: Int = 0, size: Int = out.size - offset): ShortArray = buffer.getArrayInt16(index, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): ShortArray = getArray(index, ShortArray(size))
    fun setArray(index: Int, inp: ShortArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayInt16(index, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Int16Buffer = Int16Buffer(buffer._slice(start * ELEMENT_SIZE_IN_BYTES, end * ELEMENT_SIZE_IN_BYTES))
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
    constructor(data: UByteArrayInt, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size).also { it.setArrayUInt8(0, data, offset, size) })

    override val elementSizeInBytes: Int get() = ELEMENT_SIZE_IN_BYTES
    override operator fun get(index: Int): Int = buffer.getU8(index)
    override operator fun set(index: Int, value: Int) = buffer.set8(index, value.toByte())
    fun getArray(index: Int, out: UByteArrayInt, offset: Int = 0, size: Int = out.size - offset): UByteArrayInt = buffer.getArrayUInt8(index, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): UByteArrayInt = getArray(index, UByteArrayInt(size))
    fun setArray(index: Int, inp: UByteArrayInt, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayUInt8(index, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Uint8Buffer = Uint8Buffer(buffer._slice(start * ELEMENT_SIZE_IN_BYTES, end * ELEMENT_SIZE_IN_BYTES))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Uint8Buffer = slice(start, start + size)
}

@JvmInline
value class Uint8ClampedBuffer(override val buffer: Buffer) : TypedBuffer, BaseIntBuffer {
    companion object {
        const val ELEMENT_SIZE_IN_BYTES = 1
        operator fun invoke(data: ByteArray) = Uint8ClampedBuffer(UByteArrayInt(data))
    }
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES, direct))
    constructor(data: UByteArrayInt, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size).also { it.setArrayUInt8(0, data, offset, size) })

    override val elementSizeInBytes: Int get() = ELEMENT_SIZE_IN_BYTES
    override operator fun get(index: Int): Int = buffer.getU8(index)
    override operator fun set(index: Int, value: Int) = buffer.set8Clamped(index, value)

    fun slice(start: Int = 0, end: Int = this.size): Uint8ClampedBuffer = Uint8ClampedBuffer(buffer._slice(start * ELEMENT_SIZE_IN_BYTES, end * ELEMENT_SIZE_IN_BYTES))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Uint8ClampedBuffer = slice(start, start + size)
}

@JvmInline
value class Uint16Buffer(override val buffer: Buffer) : TypedBuffer, BaseIntBuffer {
    companion object {
        const val ELEMENT_SIZE_IN_BYTES = 2
        operator fun invoke(data: ByteArray) = Uint8ClampedBuffer(UByteArrayInt(data))
    }
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES, direct))
    constructor(data: UShortArrayInt, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES).also { it.setArrayUInt16(0, data, offset, size) })

    override val elementSizeInBytes: Int get() = ELEMENT_SIZE_IN_BYTES
    override operator fun get(index: Int): Int = buffer.getU16LE(index * ELEMENT_SIZE_IN_BYTES)
    override operator fun set(index: Int, value: Int) = buffer.set16LE(index * ELEMENT_SIZE_IN_BYTES, value.toShort())
    fun getArray(index: Int, out: UShortArrayInt, offset: Int = 0, size: Int = out.size - offset): UShortArrayInt = buffer.getArrayUInt16(index, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): UShortArrayInt = getArray(index, UShortArrayInt(size))
    fun setArray(index: Int, inp: UShortArrayInt, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayUInt16(index, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Uint16Buffer = Uint16Buffer(buffer._slice(start * ELEMENT_SIZE_IN_BYTES, end * ELEMENT_SIZE_IN_BYTES))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Uint16Buffer = slice(start, start + size)
}

@JvmInline
value class Int32Buffer(override val buffer: Buffer) : TypedBuffer, BaseIntBuffer {
    companion object {
        const val ELEMENT_SIZE_IN_BYTES = 4
    }
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES, direct))
    constructor(data: IntArray, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES).also { it.setArrayInt32(0, data, offset, size) })

    override val elementSizeInBytes: Int get() = ELEMENT_SIZE_IN_BYTES
    override operator fun get(index: Int): Int = buffer.getS32LE(index * ELEMENT_SIZE_IN_BYTES)
    override operator fun set(index: Int, value: Int) = buffer.set32LE(index * ELEMENT_SIZE_IN_BYTES, value)
    fun getArray(index: Int, out: IntArray, offset: Int = 0, size: Int = out.size - offset): IntArray = buffer.getArrayInt32(index, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): IntArray = getArray(index, IntArray(size))
    fun setArray(index: Int, inp: IntArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayInt32(index, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Int32Buffer = Int32Buffer(buffer._slice(start * ELEMENT_SIZE_IN_BYTES, end * ELEMENT_SIZE_IN_BYTES))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Int32Buffer = slice(start, start + size)
}

@JvmInline
value class Uint32Buffer(override val buffer: Buffer) : TypedBuffer {
    companion object {
        const val ELEMENT_SIZE_IN_BYTES = 4
    }
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES, direct))
    constructor(data: UIntArray, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES).also { it.setArrayInt32(0, data.toIntArray(), offset, size) })

    override val elementSizeInBytes: Int get() = ELEMENT_SIZE_IN_BYTES
    operator fun get(index: Int): UInt = buffer.getS32LE(index * ELEMENT_SIZE_IN_BYTES).toUInt()
    operator fun set(index: Int, value: UInt) = buffer.set32LE(index * ELEMENT_SIZE_IN_BYTES, value.toInt())
    operator fun set(index: Int, value: Int) = buffer.set32LE(index * ELEMENT_SIZE_IN_BYTES, value)
    fun getArray(index: Int, out: UIntArray, offset: Int = 0, size: Int = out.size - offset): UIntArray = buffer.getArrayInt32(index, out.asIntArray(), offset, size).asUIntArray()
    fun getArray(index: Int = 0, size: Int = this.size - index): UIntArray = getArray(index, UIntArray(size))
    fun setArray(index: Int, inp: UIntArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayInt32(index, inp.asIntArray(), offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Uint32Buffer = Uint32Buffer(buffer._slice(start * ELEMENT_SIZE_IN_BYTES, end * ELEMENT_SIZE_IN_BYTES))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Uint32Buffer = slice(start, start + size)
}

@JvmInline
value class Int64Buffer(override val buffer: Buffer) : TypedBuffer {
    companion object {
        const val ELEMENT_SIZE_IN_BYTES = 8
    }
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES, direct))
    constructor(data: LongArray, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES).also { it.setArrayInt64(0, data, offset, size) })

    override val elementSizeInBytes: Int get() = ELEMENT_SIZE_IN_BYTES
    operator fun get(index: Int): Long = buffer.getS64LE(index * ELEMENT_SIZE_IN_BYTES)
    operator fun set(index: Int, value: Long) = buffer.set64LE(index * ELEMENT_SIZE_IN_BYTES, value)
    fun getArray(index: Int, out: LongArray, offset: Int = 0, size: Int = out.size - offset): LongArray = buffer.getArrayInt64(index, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): LongArray = getArray(index, LongArray(size))
    fun setArray(index: Int, inp: LongArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayInt64(index, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Int64Buffer = Int64Buffer(buffer._slice(start * ELEMENT_SIZE_IN_BYTES, end * ELEMENT_SIZE_IN_BYTES))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Int64Buffer = slice(start, start + size)
}

@JvmInline
value class Float32Buffer(override val buffer: Buffer) : TypedBuffer, BaseFloatBuffer {
    companion object {
        const val ELEMENT_SIZE_IN_BYTES = 4
    }
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES, direct))
    constructor(data: FloatArray, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES).also { it.setArrayFloat32(0, data, offset, size) })

    override val elementSizeInBytes: Int get() = ELEMENT_SIZE_IN_BYTES
    override operator fun get(index: Int): Float = buffer.getF32LE(index * ELEMENT_SIZE_IN_BYTES)
    override operator fun set(index: Int, value: Float) = buffer.setF32LE(index * ELEMENT_SIZE_IN_BYTES, value)
    fun getArray(index: Int, out: FloatArray, offset: Int = 0, size: Int = out.size - offset): FloatArray = buffer.getArrayFloat32(index, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): FloatArray = getArray(index, FloatArray(size))
    fun setArray(index: Int, inp: FloatArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayFloat32(index, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Float32Buffer = Float32Buffer(buffer._slice(start * ELEMENT_SIZE_IN_BYTES, end * ELEMENT_SIZE_IN_BYTES))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Float32Buffer = slice(start, start + size)
}

@JvmInline
value class Float64Buffer(override val buffer: Buffer) : TypedBuffer {
    companion object {
        const val ELEMENT_SIZE_IN_BYTES = 8
    }
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES, direct))
    constructor(data: DoubleArray, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size * ELEMENT_SIZE_IN_BYTES).also { it.setArrayFloat64(0, data, offset, size) })

    override val elementSizeInBytes: Int get() = ELEMENT_SIZE_IN_BYTES
    operator fun get(index: Int): Double = buffer.getF64LE(index * ELEMENT_SIZE_IN_BYTES)
    operator fun set(index: Int, value: Double) = buffer.setF64LE(index * ELEMENT_SIZE_IN_BYTES, value)
    fun getArray(index: Int, out: DoubleArray, offset: Int = 0, size: Int = out.size - offset): DoubleArray = buffer.getArrayFloat64(index, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): DoubleArray = getArray(index, DoubleArray(size))
    fun setArray(index: Int, inp: DoubleArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayFloat64(index, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Float64Buffer = Float64Buffer(buffer._slice(start * ELEMENT_SIZE_IN_BYTES, end * ELEMENT_SIZE_IN_BYTES))
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

fun TypedBuffer.asUInt8(): Uint8Buffer = this.buffer.u8
fun TypedBuffer.asUInt16(): Uint16Buffer = this.buffer.u16
fun TypedBuffer.asInt8(): Int8Buffer = this.buffer.i8
fun TypedBuffer.asInt16(): Int16Buffer = this.buffer.i16
fun TypedBuffer.asInt32(): Int32Buffer = this.buffer.i32
fun TypedBuffer.asInt64(): Int64Buffer = this.buffer.i64
fun TypedBuffer.asFloat32(): Float32Buffer = this.buffer.f32
fun TypedBuffer.asFloat64(): Float64Buffer = this.buffer.f64

inline fun <T> BufferTemp(size: Int, callback: (Buffer) -> T): T = Buffer.allocDirect(size).run(callback)

fun ByteArray.toNBufferUInt8(): Uint8Buffer = Buffer(this).u8
