package com.soywiz.kmem

import kotlin.jvm.*

expect class NBuffer {
    companion object
}
val NBuffer.size: Int get() = sizeInBytes
internal fun NBuffer_toString(buffer: NBuffer): String = "FBuffer(size=${buffer.size})"
internal fun checkNBufferSize(size: Int) {
    if (size < 0) throw IllegalArgumentException("invalid size $size")
}
internal fun checkNBufferWrap(array: ByteArray, offset: Int, size: Int) {
    val end = offset + size
    if (size < 0 || offset !in 0..array.size || end !in 0..array.size) {
        throw IllegalArgumentException("invalid arguments offset=$offset, size=$size for array.size=${array.size}")
    }
}
expect fun NBuffer(size: Int, direct: Boolean = false): NBuffer
expect fun NBuffer(array: ByteArray, offset: Int = 0, size: Int = array.size - offset): NBuffer
expect val NBuffer.byteOffset: Int
expect val NBuffer.sizeInBytes: Int
private fun Int.hexChar(): Char = when (this) {
    in 0..9 -> '0' + this
    in 10..26 -> 'a' + (this - 10)
    else -> '?'
}
fun NBuffer.hex(): String = buildString(sizeInBytes * 2) {
    for (n in 0 until this@hex.sizeInBytes) {
        val value = this@hex.getUInt8(n)
        append(value.extract4(4).hexChar())
        append(value.extract4(0).hexChar())
    }
}
internal expect fun NBuffer.sliceInternal(start: Int, end: Int): NBuffer
fun NBuffer.sliceWithSize(start: Int, size: Int): NBuffer = slice(start, start + size)
fun NBuffer.slice(start: Int = 0, end: Int = sizeInBytes): NBuffer {
    if (start > end || start !in 0 .. sizeInBytes || end !in 0 .. sizeInBytes) {
        throw IllegalArgumentException("invalid slice start:$start, end:$end not in 0..$sizeInBytes")
    }
    return sliceInternal(start, end)
}

// Copy
expect fun NBuffer.Companion.copy(src: NBuffer, srcPosBytes: Int, dst: NBuffer, dstPosBytes: Int, sizeInBytes: Int)

// Unaligned versions

fun NBuffer.getUnalignedUInt8(byteOffset: Int): Int = getUnalignedInt8(byteOffset).toInt() and 0xFF
fun NBuffer.getUnalignedUInt16(byteOffset: Int): Int = getUnalignedInt16(byteOffset).toInt() and 0xFFFF
expect fun NBuffer.getUnalignedInt8(byteOffset: Int): Byte
expect fun NBuffer.getUnalignedInt16(byteOffset: Int): Short
expect fun NBuffer.getUnalignedInt32(byteOffset: Int): Int
expect fun NBuffer.getUnalignedInt64(byteOffset: Int): Long
expect fun NBuffer.getUnalignedFloat32(byteOffset: Int): Float
expect fun NBuffer.getUnalignedFloat64(byteOffset: Int): Double

fun NBuffer.setUnalignedUInt8(byteOffset: Int, value: Int) = setUnalignedInt8(byteOffset, value.toByte())
fun NBuffer.setUnalignedUInt8Clamped(byteOffset: Int, value: Int) = setUnalignedInt8(byteOffset, value.clampUByte().toByte())
fun NBuffer.setUnalignedUInt16(byteOffset: Int, value: Int) = setUnalignedInt16(byteOffset, value.toShort())
expect fun NBuffer.setUnalignedInt8(byteOffset: Int, value: Byte)
fun NBuffer.setUnalignedInt8(byteOffset: Int, value: Int) = setUnalignedInt8(byteOffset, value.toByte())
expect fun NBuffer.setUnalignedInt16(byteOffset: Int, value: Short)
expect fun NBuffer.setUnalignedInt32(byteOffset: Int, value: Int)
expect fun NBuffer.setUnalignedInt64(byteOffset: Int, value: Long)
expect fun NBuffer.setUnalignedFloat32(byteOffset: Int, value: Float)
expect fun NBuffer.setUnalignedFloat64(byteOffset: Int, value: Double)

// Array versions

fun NBuffer.getUnalignedArrayInt8(byteOffset: Int, out: ByteArray, offset: Int = 0, size: Int = out.size - offset): ByteArray { for (n in 0 until size) out[offset + n] = getUnalignedInt8(byteOffset + n * 1); return out }
fun NBuffer.getUnalignedArrayInt16(byteOffset: Int, out: ShortArray, offset: Int = 0, size: Int = out.size - offset): ShortArray { for (n in 0 until size) out[offset + n] = getUnalignedInt16(byteOffset + n * 2); return out }
fun NBuffer.getUnalignedArrayInt32(byteOffset: Int, out: IntArray, offset: Int = 0, size: Int = out.size - offset): IntArray { for (n in 0 until size) out[offset + n] = getUnalignedInt32(byteOffset + n * 4); return out }
fun NBuffer.getUnalignedArrayInt64(byteOffset: Int, out: LongArray, offset: Int = 0, size: Int = out.size - offset): LongArray { for (n in 0 until size) out[offset + n] = getUnalignedInt64(byteOffset + n * 8); return out }
fun NBuffer.getUnalignedArrayFloat32(byteOffset: Int, out: FloatArray, offset: Int = 0, size: Int = out.size - offset): FloatArray { for (n in 0 until size) out[offset + n] = getUnalignedFloat32(byteOffset + n * 4); return out }
fun NBuffer.getUnalignedArrayFloat64(byteOffset: Int, out: DoubleArray, offset: Int = 0, size: Int = out.size - offset): DoubleArray { for (n in 0 until size) out[offset + n] = getUnalignedFloat64(byteOffset + n * 8); return out }

fun NBuffer.setUnalignedArrayInt8(byteOffset: Int, inp: ByteArray, offset: Int = 0, size: Int = inp.size - offset): Unit { for (n in 0 until size) setUnalignedInt8(byteOffset + n * 1, inp[offset + n]) }
fun NBuffer.setUnalignedArrayInt16(byteOffset: Int, inp: ShortArray, offset: Int = 0, size: Int = inp.size - offset): Unit { for (n in 0 until size) setUnalignedInt16(byteOffset + n * 2, inp[offset + n]) }
fun NBuffer.setUnalignedArrayInt32(byteOffset: Int, inp: IntArray, offset: Int = 0, size: Int = inp.size - offset): Unit { for (n in 0 until size) setUnalignedInt32(byteOffset + n * 4, inp[offset + n]) }
fun NBuffer.setUnalignedArrayInt64(byteOffset: Int, inp: LongArray, offset: Int = 0, size: Int = inp.size - offset): Unit { for (n in 0 until size) setUnalignedInt64(byteOffset + n * 8, inp[offset + n]) }
fun NBuffer.setUnalignedArrayFloat32(byteOffset: Int, inp: FloatArray, offset: Int = 0, size: Int = inp.size - offset): Unit { for (n in 0 until size) setUnalignedFloat32(byteOffset + n * 4, inp[offset + n]) }
fun NBuffer.setUnalignedArrayFloat64(byteOffset: Int, inp: DoubleArray, offset: Int = 0, size: Int = inp.size - offset): Unit { for (n in 0 until size) setUnalignedFloat64(byteOffset + n * 8, inp[offset + n]) }

fun NBuffer.getUInt8(index: Int): Int = getUnalignedUInt8(index)
fun NBuffer.getUInt16(index: Int): Int = getUnalignedUInt16(index * 2).toInt() and 0xFFFF
fun NBuffer.getInt8(index: Int): Byte = getUnalignedInt8(index)
fun NBuffer.getInt16(index: Int): Short = getUnalignedInt16(index * 2)
fun NBuffer.getInt32(index: Int): Int = getUnalignedInt32(index * 4)
fun NBuffer.getInt64(index: Int): Long = getUnalignedInt64(index * 8)
fun NBuffer.getFloat32(index: Int): Float = getUnalignedFloat32(index * 4)
fun NBuffer.getFloat64(index: Int): Double = getUnalignedFloat64(index * 8)

fun NBuffer.setUInt8(index: Int, value: Int) = setUnalignedUInt8(index, value)
fun NBuffer.setUInt8Clamped(index: Int, value: Int) = setUnalignedUInt8Clamped(index, value)
fun NBuffer.setUInt16(index: Int, value: Int) = setUnalignedUInt16(index * 2, value)
fun NBuffer.setInt8(index: Int, value: Byte) = setUnalignedInt8(index, value)
fun NBuffer.setInt8(index: Int, value: Int) = setUnalignedInt8(index, value)
fun NBuffer.setInt16(index: Int, value: Short) = setUnalignedInt16(index * 2, value)
fun NBuffer.setInt32(index: Int, value: Int) = setUnalignedInt32(index * 4, value)
fun NBuffer.setInt64(index: Int, value: Long) = setUnalignedInt64(index * 8, value)
fun NBuffer.setFloat32(index: Int, value: Float) = setUnalignedFloat32(index * 4, value)
fun NBuffer.setFloat64(index: Int, value: Double) = setUnalignedFloat64(index * 8, value)

// ALIGNED ARRAYS

fun NBuffer.getArrayUInt8(index: Int, out: UByteArrayInt, offset: Int = 0, size: Int = out.size - offset): UByteArrayInt = UByteArrayInt(getUnalignedArrayInt8(index * 1, out.data, offset, size))
fun NBuffer.getArrayUInt16(index: Int, out: UShortArrayInt, offset: Int = 0, size: Int = out.size - offset): UShortArrayInt = UShortArrayInt(getUnalignedArrayInt16(index * 2, out.data, offset, size))
fun NBuffer.getArrayInt8(index: Int, out: ByteArray, offset: Int = 0, size: Int = out.size - offset): ByteArray = getUnalignedArrayInt8(index * 1, out, offset, size)
fun NBuffer.getArrayInt16(index: Int, out: ShortArray, offset: Int = 0, size: Int = out.size - offset): ShortArray = getUnalignedArrayInt16(index * 2, out, offset, size)
fun NBuffer.getArrayInt32(index: Int, out: IntArray, offset: Int = 0, size: Int = out.size - offset): IntArray = getUnalignedArrayInt32(index * 4, out, offset, size)
fun NBuffer.getArrayInt64(index: Int, out: LongArray, offset: Int = 0, size: Int = out.size - offset): LongArray = getUnalignedArrayInt64(index * 8, out, offset, size)
fun NBuffer.getArrayFloat32(index: Int, out: FloatArray, offset: Int = 0, size: Int = out.size - offset): FloatArray = getUnalignedArrayFloat32(index * 4, out, offset, size)
fun NBuffer.getArrayFloat64(index: Int, out: DoubleArray, offset: Int = 0, size: Int = out.size - offset): DoubleArray = getUnalignedArrayFloat64(index * 8, out, offset, size)

fun NBuffer.setArrayUInt8(index: Int, inp: UByteArrayInt, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayInt8(index * 1, inp.data, offset, size)
fun NBuffer.setArrayUInt16(index: Int, inp: UShortArrayInt, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayInt16(index * 2, inp.data, offset, size)
fun NBuffer.setArrayInt8(index: Int, inp: ByteArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayInt8(index * 1, inp, offset, size)
fun NBuffer.setArrayInt16(index: Int, inp: ShortArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayInt16(index * 2, inp, offset, size)
fun NBuffer.setArrayInt32(index: Int, inp: IntArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayInt32(index * 4, inp, offset, size)
fun NBuffer.setArrayInt64(index: Int, inp: LongArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayInt64(index * 8, inp, offset, size)
fun NBuffer.setArrayFloat32(index: Int, inp: FloatArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayFloat32(index * 4, inp, offset, size)
fun NBuffer.setArrayFloat64(index: Int, inp: DoubleArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayFloat64(index * 8, inp, offset, size)

interface NBufferTyped {
    val buffer: NBuffer
    val size: Int
}

interface BaseIntBuffer {
    val size: Int
    operator fun get(index: Int): Int
    operator fun set(index: Int, value: Int)
}

@JvmInline
value class IntArrayIntBuffer(val array: IntArray) : BaseIntBuffer {
    override val size: Int get() = array.size
    override operator fun get(index: Int): Int = array[index]
    override operator fun set(index: Int, value: Int) { array[index] = value }
}

inline class NBufferInt8(override val buffer: NBuffer) : NBufferTyped {
    constructor(size: Int, direct: Boolean = false) : this(NBuffer(size * 1, direct))
    constructor(data: ByteArray) : this(NBuffer(data.size * 1).also { it.setArrayInt8(0, data) })

    override val size: Int get() = buffer.sizeInBytes
    operator fun get(index: Int): Byte = buffer.getInt8(index)
    operator fun set(index: Int, value: Byte) = buffer.setInt8(index, value)
    fun getArray(index: Int, out: ByteArray, offset: Int = 0, size: Int = out.size - offset): ByteArray = buffer.getArrayInt8(index, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): ByteArray = getArray(index, ByteArray(size))
    fun setArray(index: Int, inp: ByteArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayInt8(index, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): NBufferInt8 = NBufferInt8(buffer.slice(start, end))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): NBufferInt8 = NBufferInt8(buffer.sliceWithSize(start, size))
}

inline class NBufferInt16(override val buffer: NBuffer) : NBufferTyped {
    constructor(size: Int, direct: Boolean = false) : this(NBuffer(size * 2, direct))
    constructor(data: ShortArray) : this(NBuffer(data.size * 2).also { it.setArrayInt16(0, data) })

    override val size: Int get() = buffer.sizeInBytes / 2
    operator fun get(index: Int): Short = buffer.getInt16(index)
    operator fun set(index: Int, value: Short) = buffer.setInt16(index, value)
    fun getArray(index: Int, out: ShortArray, offset: Int = 0, size: Int = out.size - offset): ShortArray = buffer.getArrayInt16(index, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): ShortArray = getArray(index, ShortArray(size))
    fun setArray(index: Int, inp: ShortArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayInt16(index, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): NBufferInt16 = NBufferInt16(buffer.slice(start * 2, end * 2))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): NBufferInt16 = NBufferInt16(buffer.sliceWithSize(start * 2, size * 2))
}

inline class NBufferUInt8(override val buffer: NBuffer) : NBufferTyped, BaseIntBuffer {
    constructor(size: Int, direct: Boolean = false) : this(NBuffer(size * 1, direct))
    constructor(data: UByteArrayInt) : this(NBuffer(data.size).also { it.setArrayUInt8(0, data) })
    companion object {
        operator fun invoke(data: ByteArray) = NBufferUInt8(UByteArrayInt(data))
    }

    override val size: Int get() = buffer.sizeInBytes
    override operator fun get(index: Int): Int = buffer.getUInt8(index)
    override operator fun set(index: Int, value: Int) = buffer.setUInt8(index, value)
    fun getArray(index: Int, out: UByteArrayInt, offset: Int = 0, size: Int = out.size - offset): UByteArrayInt = buffer.getArrayUInt8(index, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): UByteArrayInt = getArray(index, UByteArrayInt(size))
    fun setArray(index: Int, inp: UByteArrayInt, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayUInt8(index, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): NBufferUInt8 = NBufferUInt8(buffer.slice(start, end))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): NBufferUInt8 = NBufferUInt8(buffer.sliceWithSize(start, size))
}


inline class NBufferClampedUInt8(override val buffer: NBuffer) : NBufferTyped, BaseIntBuffer {
    constructor(size: Int, direct: Boolean = false) : this(NBuffer(size * 1, direct))
    constructor(data: UByteArrayInt) : this(NBuffer(data.size).also { it.setArrayUInt8(0, data) })
    companion object {
        operator fun invoke(data: ByteArray) = NBufferClampedUInt8(UByteArrayInt(data))
    }

    override val size: Int get() = buffer.sizeInBytes
    override operator fun get(index: Int): Int = buffer.getUInt8(index)
    override operator fun set(index: Int, value: Int) = buffer.setUInt8Clamped(index, value)

    fun slice(start: Int = 0, end: Int = this.size): NBufferClampedUInt8 = NBufferClampedUInt8(buffer.slice(start, end))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): NBufferClampedUInt8 = NBufferClampedUInt8(buffer.sliceWithSize(start, size))
}

inline class NBufferUInt16(override val buffer: NBuffer) : NBufferTyped, BaseIntBuffer {
    constructor(size: Int, direct: Boolean = false) : this(NBuffer(size * 2, direct))
    constructor(data: UShortArrayInt) : this(NBuffer(data.size * 2).also { it.setArrayUInt16(0, data) })

    override val size: Int get() = buffer.sizeInBytes / 2
    override operator fun get(index: Int): Int = buffer.getUInt16(index)
    override operator fun set(index: Int, value: Int) = buffer.setUInt16(index, value)
    fun getArray(index: Int, out: UShortArrayInt, offset: Int = 0, size: Int = out.size - offset): UShortArrayInt = buffer.getArrayUInt16(index, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): UShortArrayInt = getArray(index, UShortArrayInt(size))
    fun setArray(index: Int, inp: UShortArrayInt, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayUInt16(index, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): NBufferUInt16 = NBufferUInt16(buffer.slice(start * 2, end * 2))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): NBufferUInt16 = NBufferUInt16(buffer.sliceWithSize(start * 2, size * 2))
}

inline class NBufferInt32(override val buffer: NBuffer) : NBufferTyped {
    constructor(size: Int, direct: Boolean = false) : this(NBuffer(size * 4, direct))
    constructor(data: IntArray) : this(NBuffer(data.size * 4).also { it.setArrayInt32(0, data) })

    override val size: Int get() = buffer.sizeInBytes / 4
    operator fun get(index: Int): Int = buffer.getInt32(index)
    operator fun set(index: Int, value: Int) = buffer.setInt32(index, value)
    fun getArray(index: Int, out: IntArray, offset: Int = 0, size: Int = out.size - offset): IntArray = buffer.getArrayInt32(index, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): IntArray = getArray(index, IntArray(size))
    fun setArray(index: Int, inp: IntArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayInt32(index, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): NBufferInt32 = NBufferInt32(buffer.slice(start * 4, end * 4))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): NBufferInt32 = NBufferInt32(buffer.sliceWithSize(start * 4, size * 4))
}

inline class NBufferUInt32(override val buffer: NBuffer) : NBufferTyped {
    constructor(size: Int, direct: Boolean = false) : this(NBuffer(size * 4, direct))
    constructor(data: UIntArray) : this(NBuffer(data.size * 4).also { it.setArrayInt32(0, data.toIntArray()) })

    override val size: Int get() = buffer.sizeInBytes / 4
    operator fun get(index: Int): UInt = buffer.getInt32(index).toUInt()
    operator fun set(index: Int, value: UInt) = buffer.setInt32(index, value.toInt())
    operator fun set(index: Int, value: Int) = buffer.setInt32(index, value)
    fun getArray(index: Int, out: UIntArray, offset: Int = 0, size: Int = out.size - offset): UIntArray = buffer.getArrayInt32(index, out.asIntArray(), offset, size).asUIntArray()
    fun getArray(index: Int = 0, size: Int = this.size - index): UIntArray = getArray(index, UIntArray(size))
    fun setArray(index: Int, inp: UIntArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayInt32(index, inp.asIntArray(), offset, size)

    fun slice(start: Int = 0, end: Int = this.size): NBufferInt32 = NBufferInt32(buffer.slice(start * 4, end * 4))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): NBufferInt32 = NBufferInt32(buffer.sliceWithSize(start * 4, size * 4))
}

inline class NBufferInt64(override val buffer: NBuffer) : NBufferTyped {
    constructor(size: Int, direct: Boolean = false) : this(NBuffer(size * 8, direct))
    constructor(data: LongArray) : this(NBuffer(data.size * 8).also { it.setArrayInt64(0, data) })

    override val size: Int get() = buffer.sizeInBytes / 8
    operator fun get(index: Int): Long = buffer.getInt64(index)
    operator fun set(index: Int, value: Long) = buffer.setInt64(index, value)
    fun getArray(index: Int, out: LongArray, offset: Int = 0, size: Int = out.size - offset): LongArray = buffer.getArrayInt64(index, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): LongArray = getArray(index, LongArray(size))
    fun setArray(index: Int, inp: LongArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayInt64(index, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): NBufferInt64 = NBufferInt64(buffer.slice(start * 8, end * 8))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): NBufferInt64 = NBufferInt64(buffer.sliceWithSize(start * 8, size * 8))
}

inline class NBufferFloat32(override val buffer: NBuffer) : NBufferTyped {
    constructor(size: Int, direct: Boolean = false) : this(NBuffer(size * 4, direct))
    constructor(data: FloatArray) : this(NBuffer(data.size * 4).also { it.setArrayFloat32(0, data) })

    override val size: Int get() = buffer.sizeInBytes / 4
    operator fun get(index: Int): Float = buffer.getFloat32(index)
    operator fun set(index: Int, value: Float) = buffer.setFloat32(index, value)
    fun getArray(index: Int, out: FloatArray, offset: Int = 0, size: Int = out.size - offset): FloatArray = buffer.getArrayFloat32(index, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): FloatArray = getArray(index, FloatArray(size))
    fun setArray(index: Int, inp: FloatArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayFloat32(index, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): NBufferFloat32 = NBufferFloat32(buffer.slice(start * 4, end * 4))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): NBufferFloat32 = NBufferFloat32(buffer.sliceWithSize(start * 4, size * 4))
}

inline class NBufferFloat64(override val buffer: NBuffer) : NBufferTyped {
    constructor(size: Int, direct: Boolean = false) : this(NBuffer(size * 8, direct))
    constructor(data: DoubleArray) : this(NBuffer(data.size * 8).also { it.setArrayFloat64(0, data) })

    override val size: Int get() = buffer.sizeInBytes / 8
    operator fun get(index: Int): Double = buffer.getFloat64(index)
    operator fun set(index: Int, value: Double) = buffer.setFloat64(index, value)
    fun getArray(index: Int, out: DoubleArray, offset: Int = 0, size: Int = out.size - offset): DoubleArray = buffer.getArrayFloat64(index, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): DoubleArray = getArray(index, DoubleArray(size))
    fun setArray(index: Int, inp: DoubleArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayFloat64(index, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): NBufferFloat64 = NBufferFloat64(buffer.slice(start * 8, end * 8))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): NBufferFloat64 = NBufferFloat64(buffer.sliceWithSize(start * 8, size * 8))
}

inline class NBufferFast32(override val buffer: NBuffer) : NBufferTyped {
    constructor(size: Int, direct: Boolean = false) : this(NBuffer(size * 4, direct))
    override val size: Int get() = buffer.sizeInBytes / 4

    fun getF(index: Int): Float = buffer.getFloat32(index)
    fun setF(index: Int, value: Float) = buffer.setFloat32(index, value)
    fun getI(index: Int): Int = buffer.getInt32(index)
    fun setI(index: Int, value: Int) = buffer.setInt32(index, value)

    fun slice(start: Int = 0, end: Int = this.size): NBufferFast32 = NBufferFast32(buffer.slice(start * 4, end * 4))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): NBufferFast32 = NBufferFast32(buffer.sliceWithSize(start * 4, size * 4))
}

val NBuffer.u8: NBufferUInt8 get() = NBufferUInt8(this)
val NBuffer.u16: NBufferUInt16 get() = NBufferUInt16(this)
val NBuffer.i8: NBufferInt8 get() = NBufferInt8(this)
val NBuffer.i16: NBufferInt16 get() = NBufferInt16(this)
val NBuffer.i32: NBufferInt32 get() = NBufferInt32(this)
val NBuffer.i64: NBufferInt64 get() = NBufferInt64(this)
val NBuffer.f32: NBufferFloat32 get() = NBufferFloat32(this)
val NBuffer.f64: NBufferFloat64 get() = NBufferFloat64(this)
val NBuffer.fast32: NBufferFast32 get() = NBufferFast32(this)

fun NBufferTyped.asUInt8(): NBufferUInt8 = this.buffer.u8
fun NBufferTyped.asUInt16(): NBufferUInt16 = this.buffer.u16
fun NBufferTyped.asInt8(): NBufferInt8 = this.buffer.i8
fun NBufferTyped.asInt16(): NBufferInt16 = this.buffer.i16
fun NBufferTyped.asInt32(): NBufferInt32 = this.buffer.i32
fun NBufferTyped.asInt64(): NBufferInt64 = this.buffer.i64
fun NBufferTyped.asFloat32(): NBufferFloat32 = this.buffer.f32
fun NBufferTyped.asFloat64(): NBufferFloat64 = this.buffer.f64
fun NBufferTyped.asFast32(): NBufferFast32 = this.buffer.fast32

// @TODO: Compatibility layer with FBuffer, MemBuffer & DataBuffer

@Deprecated("", ReplaceWith("", "com.soywiz.kmem.NBufferUInt8")) typealias Uint8Buffer = NBufferUInt8
@Deprecated("") typealias Int8Buffer = NBufferInt8
@Deprecated("") typealias Int16Buffer = NBufferInt16
@Deprecated("") typealias Int32Buffer = NBufferInt32
@Deprecated("") typealias Float32Buffer = NBufferFloat32
@Deprecated("") typealias Float64Buffer = NBufferFloat64
@Deprecated("") typealias Uint8ClampedBuffer = NBufferClampedUInt8
@Deprecated("") typealias Uint32Buffer = NBufferUInt32

@Deprecated("") typealias FBuffer = NBuffer
fun NBuffer.Companion.allocDirect(size: Int): NBuffer = NBuffer(size, direct = true)
fun NBuffer.Companion.allocNoDirect(size: Int): NBuffer = NBuffer(size, direct = false)

@Deprecated("") operator fun NBuffer.get(index: Int): Byte = getInt8(index)
@Deprecated("") operator fun NBuffer.set(index: Int, value: Byte) = setInt8(index, value)
@Deprecated("") operator fun NBuffer.set(index: Int, value: Int) = setUInt8(index, value)

inline fun <T> NBufferTemp(size: Int, callback: (FBuffer) -> T): T = NBuffer(size).run(callback)

fun ByteArray.toNBufferUInt8(): NBufferUInt8 = NBuffer(this).u8

@Deprecated("")
typealias Fast32Buffer = NBufferFast32
