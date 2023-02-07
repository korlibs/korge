package com.soywiz.kmem

import kotlin.jvm.*

expect class Buffer {
    companion object {
        fun copy(src: Buffer, srcPosBytes: Int, dst: Buffer, dstPosBytes: Int, sizeInBytes: Int)
    }
}
val Buffer.size: Int get() = sizeInBytes
internal fun NBuffer_toString(buffer: Buffer): String = "Buffer(size=${buffer.size})"
internal fun checkNBufferSize(size: Int) {
    if (size < 0) throw IllegalArgumentException("invalid size $size")
}
internal fun checkNBufferWrap(array: ByteArray, offset: Int, size: Int) {
    val end = offset + size
    if (size < 0 || offset !in 0..array.size || end !in 0..array.size) {
        throw IllegalArgumentException("invalid arguments offset=$offset, size=$size for array.size=${array.size}")
    }
}
expect fun Buffer(size: Int, direct: Boolean = false): Buffer
expect fun Buffer(array: ByteArray, offset: Int = 0, size: Int = array.size - offset): Buffer
fun Buffer.Companion.allocDirect(size: Int): Buffer = Buffer(size, direct = true)
fun Buffer.Companion.allocNoDirect(size: Int): Buffer = Buffer(size, direct = false)

fun Buffer.clone(direct: Boolean = false): Buffer {
    val out = Buffer(this.size, direct)
    arraycopy(this, 0, out, 0, size)
    return out
}

expect val Buffer.byteOffset: Int
expect val Buffer.sizeInBytes: Int
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
internal expect fun Buffer.sliceInternal(start: Int, end: Int): Buffer
fun Buffer.sliceWithSize(start: Int, size: Int): Buffer = slice(start, start + size)
fun Buffer.slice(start: Int = 0, end: Int = sizeInBytes): Buffer {
    if (start > end || start !in 0 .. sizeInBytes || end !in 0 .. sizeInBytes) {
        throw IllegalArgumentException("invalid slice start:$start, end:$end not in 0..$sizeInBytes")
    }
    return sliceInternal(start, end)
}

// Unaligned versions

fun Buffer.getUnalignedUInt8(byteOffset: Int): Int = getUnalignedInt8(byteOffset).toInt() and 0xFF
fun Buffer.getUnalignedUInt16(byteOffset: Int): Int = getUnalignedInt16(byteOffset).toInt() and 0xFFFF
expect fun Buffer.getUnalignedInt8(byteOffset: Int): Byte
expect fun Buffer.getUnalignedInt16(byteOffset: Int): Short
expect fun Buffer.getUnalignedInt32(byteOffset: Int): Int
expect fun Buffer.getUnalignedInt64(byteOffset: Int): Long
expect fun Buffer.getUnalignedFloat32(byteOffset: Int): Float
expect fun Buffer.getUnalignedFloat64(byteOffset: Int): Double

fun Buffer.setUnalignedUInt8(byteOffset: Int, value: Int) = setUnalignedInt8(byteOffset, value.toByte())
fun Buffer.setUnalignedUInt8Clamped(byteOffset: Int, value: Int) = setUnalignedInt8(byteOffset, value.clampUByte().toByte())
fun Buffer.setUnalignedUInt16(byteOffset: Int, value: Int) = setUnalignedInt16(byteOffset, value.toShort())
expect fun Buffer.setUnalignedInt8(byteOffset: Int, value: Byte)
fun Buffer.setUnalignedInt8(byteOffset: Int, value: Int) = setUnalignedInt8(byteOffset, value.toByte())
expect fun Buffer.setUnalignedInt16(byteOffset: Int, value: Short)
expect fun Buffer.setUnalignedInt32(byteOffset: Int, value: Int)
expect fun Buffer.setUnalignedInt64(byteOffset: Int, value: Long)
expect fun Buffer.setUnalignedFloat32(byteOffset: Int, value: Float)
expect fun Buffer.setUnalignedFloat64(byteOffset: Int, value: Double)

// Array versions

fun Buffer.getUnalignedArrayInt8(byteOffset: Int, out: ByteArray, offset: Int = 0, size: Int = out.size - offset): ByteArray { for (n in 0 until size) out[offset + n] = getUnalignedInt8(byteOffset + n * 1); return out }
fun Buffer.getUnalignedArrayInt16(byteOffset: Int, out: ShortArray, offset: Int = 0, size: Int = out.size - offset): ShortArray { for (n in 0 until size) out[offset + n] = getUnalignedInt16(byteOffset + n * 2); return out }
fun Buffer.getUnalignedArrayInt32(byteOffset: Int, out: IntArray, offset: Int = 0, size: Int = out.size - offset): IntArray { for (n in 0 until size) out[offset + n] = getUnalignedInt32(byteOffset + n * 4); return out }
fun Buffer.getUnalignedArrayInt64(byteOffset: Int, out: LongArray, offset: Int = 0, size: Int = out.size - offset): LongArray { for (n in 0 until size) out[offset + n] = getUnalignedInt64(byteOffset + n * 8); return out }
fun Buffer.getUnalignedArrayFloat32(byteOffset: Int, out: FloatArray, offset: Int = 0, size: Int = out.size - offset): FloatArray { for (n in 0 until size) out[offset + n] = getUnalignedFloat32(byteOffset + n * 4); return out }
fun Buffer.getUnalignedArrayFloat64(byteOffset: Int, out: DoubleArray, offset: Int = 0, size: Int = out.size - offset): DoubleArray { for (n in 0 until size) out[offset + n] = getUnalignedFloat64(byteOffset + n * 8); return out }

fun Buffer.setUnalignedArrayInt8(byteOffset: Int, inp: ByteArray, offset: Int = 0, size: Int = inp.size - offset): Unit { for (n in 0 until size) setUnalignedInt8(byteOffset + n * 1, inp[offset + n]) }
fun Buffer.setUnalignedArrayInt16(byteOffset: Int, inp: ShortArray, offset: Int = 0, size: Int = inp.size - offset): Unit { for (n in 0 until size) setUnalignedInt16(byteOffset + n * 2, inp[offset + n]) }
fun Buffer.setUnalignedArrayInt32(byteOffset: Int, inp: IntArray, offset: Int = 0, size: Int = inp.size - offset): Unit { for (n in 0 until size) setUnalignedInt32(byteOffset + n * 4, inp[offset + n]) }
fun Buffer.setUnalignedArrayInt64(byteOffset: Int, inp: LongArray, offset: Int = 0, size: Int = inp.size - offset): Unit { for (n in 0 until size) setUnalignedInt64(byteOffset + n * 8, inp[offset + n]) }
fun Buffer.setUnalignedArrayFloat32(byteOffset: Int, inp: FloatArray, offset: Int = 0, size: Int = inp.size - offset): Unit { for (n in 0 until size) setUnalignedFloat32(byteOffset + n * 4, inp[offset + n]) }
fun Buffer.setUnalignedArrayFloat64(byteOffset: Int, inp: DoubleArray, offset: Int = 0, size: Int = inp.size - offset): Unit { for (n in 0 until size) setUnalignedFloat64(byteOffset + n * 8, inp[offset + n]) }

fun Buffer.getUInt8(index: Int): Int = getUnalignedUInt8(index)
fun Buffer.getUInt16(index: Int): Int = getUnalignedUInt16(index * 2).toInt() and 0xFFFF
fun Buffer.getInt8(index: Int): Byte = getUnalignedInt8(index)
fun Buffer.getInt16(index: Int): Short = getUnalignedInt16(index * 2)
fun Buffer.getInt32(index: Int): Int = getUnalignedInt32(index * 4)
fun Buffer.getInt64(index: Int): Long = getUnalignedInt64(index * 8)
fun Buffer.getFloat32(index: Int): Float = getUnalignedFloat32(index * 4)
fun Buffer.getFloat64(index: Int): Double = getUnalignedFloat64(index * 8)

fun Buffer.setUInt8(index: Int, value: Int) = setUnalignedUInt8(index, value)
fun Buffer.setUInt8Clamped(index: Int, value: Int) = setUnalignedUInt8Clamped(index, value)
fun Buffer.setUInt16(index: Int, value: Int) = setUnalignedUInt16(index * 2, value)
fun Buffer.setInt8(index: Int, value: Byte) = setUnalignedInt8(index, value)
fun Buffer.setInt8(index: Int, value: Int) = setUnalignedInt8(index, value)
fun Buffer.setInt16(index: Int, value: Short) = setUnalignedInt16(index * 2, value)
fun Buffer.setInt32(index: Int, value: Int) = setUnalignedInt32(index * 4, value)
fun Buffer.setInt64(index: Int, value: Long) = setUnalignedInt64(index * 8, value)
fun Buffer.setFloat32(index: Int, value: Float) = setUnalignedFloat32(index * 4, value)
fun Buffer.setFloat64(index: Int, value: Double) = setUnalignedFloat64(index * 8, value)

// ALIGNED ARRAYS

fun Buffer.getArrayUInt8(index: Int, out: UByteArrayInt, offset: Int = 0, size: Int = out.size - offset): UByteArrayInt = UByteArrayInt(getUnalignedArrayInt8(index * 1, out.data, offset, size))
fun Buffer.getArrayUInt16(index: Int, out: UShortArrayInt, offset: Int = 0, size: Int = out.size - offset): UShortArrayInt = UShortArrayInt(getUnalignedArrayInt16(index * 2, out.data, offset, size))
fun Buffer.getArrayInt8(index: Int, out: ByteArray, offset: Int = 0, size: Int = out.size - offset): ByteArray = getUnalignedArrayInt8(index * 1, out, offset, size)
fun Buffer.getArrayInt16(index: Int, out: ShortArray, offset: Int = 0, size: Int = out.size - offset): ShortArray = getUnalignedArrayInt16(index * 2, out, offset, size)
fun Buffer.getArrayInt32(index: Int, out: IntArray, offset: Int = 0, size: Int = out.size - offset): IntArray = getUnalignedArrayInt32(index * 4, out, offset, size)
fun Buffer.getArrayInt64(index: Int, out: LongArray, offset: Int = 0, size: Int = out.size - offset): LongArray = getUnalignedArrayInt64(index * 8, out, offset, size)
fun Buffer.getArrayFloat32(index: Int, out: FloatArray, offset: Int = 0, size: Int = out.size - offset): FloatArray = getUnalignedArrayFloat32(index * 4, out, offset, size)
fun Buffer.getArrayFloat64(index: Int, out: DoubleArray, offset: Int = 0, size: Int = out.size - offset): DoubleArray = getUnalignedArrayFloat64(index * 8, out, offset, size)

fun Buffer.setArrayUInt8(index: Int, inp: UByteArrayInt, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayInt8(index * 1, inp.data, offset, size)
fun Buffer.setArrayUInt16(index: Int, inp: UShortArrayInt, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayInt16(index * 2, inp.data, offset, size)
fun Buffer.setArrayInt8(index: Int, inp: ByteArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayInt8(index * 1, inp, offset, size)
fun Buffer.setArrayInt16(index: Int, inp: ShortArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayInt16(index * 2, inp, offset, size)
fun Buffer.setArrayInt32(index: Int, inp: IntArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayInt32(index * 4, inp, offset, size)
fun Buffer.setArrayInt64(index: Int, inp: LongArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayInt64(index * 8, inp, offset, size)
fun Buffer.setArrayFloat32(index: Int, inp: FloatArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayFloat32(index * 4, inp, offset, size)
fun Buffer.setArrayFloat64(index: Int, inp: DoubleArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayFloat64(index * 8, inp, offset, size)

interface BaseBuffer {
    val size: Int
}

interface TypedBuffer : BaseBuffer {
    val buffer: Buffer
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

inline class Int8Buffer(override val buffer: Buffer) : TypedBuffer {
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * 1, direct))
    constructor(data: ByteArray, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size * 1).also { it.setArrayInt8(0, data, offset, size) })

    override val size: Int get() = buffer.sizeInBytes
    operator fun get(index: Int): Byte = buffer.getInt8(index)
    operator fun set(index: Int, value: Byte) = buffer.setInt8(index, value)
    fun getArray(index: Int, out: ByteArray, offset: Int = 0, size: Int = out.size - offset): ByteArray = buffer.getArrayInt8(index, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): ByteArray = getArray(index, ByteArray(size))
    fun setArray(index: Int, inp: ByteArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayInt8(index, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Int8Buffer = Int8Buffer(buffer.slice(start, end))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Int8Buffer =
        Int8Buffer(buffer.sliceWithSize(start, size))
}

inline class Int16Buffer(override val buffer: Buffer) : TypedBuffer {
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * 2, direct))
    constructor(data: ShortArray, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size * 2).also { it.setArrayInt16(0, data, offset, size) })

    override val size: Int get() = buffer.sizeInBytes / 2
    operator fun get(index: Int): Short = buffer.getInt16(index)
    operator fun set(index: Int, value: Short) = buffer.setInt16(index, value)
    fun getArray(index: Int, out: ShortArray, offset: Int = 0, size: Int = out.size - offset): ShortArray = buffer.getArrayInt16(index, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): ShortArray = getArray(index, ShortArray(size))
    fun setArray(index: Int, inp: ShortArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayInt16(index, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Int16Buffer = Int16Buffer(buffer.slice(start * 2, end * 2))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Int16Buffer =
        Int16Buffer(buffer.sliceWithSize(start * 2, size * 2))
}

inline class Uint8Buffer(override val buffer: Buffer) : TypedBuffer, BaseIntBuffer {
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * 1, direct))
    constructor(data: UByteArrayInt, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size).also { it.setArrayUInt8(0, data, offset, size) })
    companion object {
        operator fun invoke(data: ByteArray) = Uint8Buffer(UByteArrayInt(data))
    }

    override val size: Int get() = buffer.sizeInBytes
    override operator fun get(index: Int): Int = buffer.getUInt8(index)
    override operator fun set(index: Int, value: Int) = buffer.setUInt8(index, value)
    fun getArray(index: Int, out: UByteArrayInt, offset: Int = 0, size: Int = out.size - offset): UByteArrayInt = buffer.getArrayUInt8(index, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): UByteArrayInt = getArray(index, UByteArrayInt(size))
    fun setArray(index: Int, inp: UByteArrayInt, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayUInt8(index, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Uint8Buffer = Uint8Buffer(buffer.slice(start, end))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Uint8Buffer = Uint8Buffer(buffer.sliceWithSize(start, size))
}


inline class Uint8ClampedBuffer(override val buffer: Buffer) : TypedBuffer, BaseIntBuffer {
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * 1, direct))
    constructor(data: UByteArrayInt, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size).also { it.setArrayUInt8(0, data, offset, size) })
    companion object {
        operator fun invoke(data: ByteArray) = Uint8ClampedBuffer(UByteArrayInt(data))
    }

    override val size: Int get() = buffer.sizeInBytes
    override operator fun get(index: Int): Int = buffer.getUInt8(index)
    override operator fun set(index: Int, value: Int) = buffer.setUInt8Clamped(index, value)

    fun slice(start: Int = 0, end: Int = this.size): Uint8ClampedBuffer =
        Uint8ClampedBuffer(buffer.slice(start, end))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Uint8ClampedBuffer =
        Uint8ClampedBuffer(buffer.sliceWithSize(start, size))
}

inline class Uint16Buffer(override val buffer: Buffer) : TypedBuffer, BaseIntBuffer {
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * 2, direct))
    constructor(data: UShortArrayInt, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size * 2).also { it.setArrayUInt16(0, data, offset, size) })

    override val size: Int get() = buffer.sizeInBytes / 2
    override operator fun get(index: Int): Int = buffer.getUInt16(index)
    override operator fun set(index: Int, value: Int) = buffer.setUInt16(index, value)
    fun getArray(index: Int, out: UShortArrayInt, offset: Int = 0, size: Int = out.size - offset): UShortArrayInt = buffer.getArrayUInt16(index, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): UShortArrayInt = getArray(index, UShortArrayInt(size))
    fun setArray(index: Int, inp: UShortArrayInt, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayUInt16(index, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Uint16Buffer = Uint16Buffer(buffer.slice(start * 2, end * 2))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Uint16Buffer = Uint16Buffer(buffer.sliceWithSize(start * 2, size * 2))
}

inline class Int32Buffer(override val buffer: Buffer) : TypedBuffer, BaseIntBuffer {
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * 4, direct))
    constructor(data: IntArray, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size * 4).also { it.setArrayInt32(0, data, offset, size) })

    override val size: Int get() = buffer.sizeInBytes / 4
    override operator fun get(index: Int): Int = buffer.getInt32(index)
    override operator fun set(index: Int, value: Int) = buffer.setInt32(index, value)
    fun getArray(index: Int, out: IntArray, offset: Int = 0, size: Int = out.size - offset): IntArray = buffer.getArrayInt32(index, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): IntArray = getArray(index, IntArray(size))
    fun setArray(index: Int, inp: IntArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayInt32(index, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Int32Buffer = Int32Buffer(buffer.slice(start * 4, end * 4))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Int32Buffer =
        Int32Buffer(buffer.sliceWithSize(start * 4, size * 4))
}

inline class Uint32Buffer(override val buffer: Buffer) : TypedBuffer {
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * 4, direct))
    constructor(data: UIntArray, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size * 4).also { it.setArrayInt32(0, data.toIntArray(), offset, size) })

    override val size: Int get() = buffer.sizeInBytes / 4
    operator fun get(index: Int): UInt = buffer.getInt32(index).toUInt()
    operator fun set(index: Int, value: UInt) = buffer.setInt32(index, value.toInt())
    operator fun set(index: Int, value: Int) = buffer.setInt32(index, value)
    fun getArray(index: Int, out: UIntArray, offset: Int = 0, size: Int = out.size - offset): UIntArray = buffer.getArrayInt32(index, out.asIntArray(), offset, size).asUIntArray()
    fun getArray(index: Int = 0, size: Int = this.size - index): UIntArray = getArray(index, UIntArray(size))
    fun setArray(index: Int, inp: UIntArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayInt32(index, inp.asIntArray(), offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Int32Buffer = Int32Buffer(buffer.slice(start * 4, end * 4))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Int32Buffer =
        Int32Buffer(buffer.sliceWithSize(start * 4, size * 4))
}

inline class Int64Buffer(override val buffer: Buffer) : TypedBuffer {
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * 8, direct))
    constructor(data: LongArray, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size * 8).also { it.setArrayInt64(0, data, offset, size) })

    override val size: Int get() = buffer.sizeInBytes / 8
    operator fun get(index: Int): Long = buffer.getInt64(index)
    operator fun set(index: Int, value: Long) = buffer.setInt64(index, value)
    fun getArray(index: Int, out: LongArray, offset: Int = 0, size: Int = out.size - offset): LongArray = buffer.getArrayInt64(index, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): LongArray = getArray(index, LongArray(size))
    fun setArray(index: Int, inp: LongArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayInt64(index, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Int64Buffer = Int64Buffer(buffer.slice(start * 8, end * 8))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Int64Buffer = Int64Buffer(buffer.sliceWithSize(start * 8, size * 8))
}

inline class Float32Buffer(override val buffer: Buffer) : TypedBuffer, BaseFloatBuffer {
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * 4, direct))
    constructor(data: FloatArray, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size * 4).also { it.setArrayFloat32(0, data, offset, size) })

    override val size: Int get() = buffer.sizeInBytes / 4
    override operator fun get(index: Int): Float = buffer.getFloat32(index)
    override operator fun set(index: Int, value: Float) = buffer.setFloat32(index, value)
    fun getArray(index: Int, out: FloatArray, offset: Int = 0, size: Int = out.size - offset): FloatArray = buffer.getArrayFloat32(index, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): FloatArray = getArray(index, FloatArray(size))
    fun setArray(index: Int, inp: FloatArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayFloat32(index, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Float32Buffer =
        Float32Buffer(buffer.slice(start * 4, end * 4))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Float32Buffer =
        Float32Buffer(buffer.sliceWithSize(start * 4, size * 4))
}

inline class Float64Buffer(override val buffer: Buffer) : TypedBuffer {
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * 8, direct))
    constructor(data: DoubleArray, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size * 8).also { it.setArrayFloat64(0, data, offset, size) })

    override val size: Int get() = buffer.sizeInBytes / 8
    operator fun get(index: Int): Double = buffer.getFloat64(index)
    operator fun set(index: Int, value: Double) = buffer.setFloat64(index, value)
    fun getArray(index: Int, out: DoubleArray, offset: Int = 0, size: Int = out.size - offset): DoubleArray = buffer.getArrayFloat64(index, out, offset, size)
    fun getArray(index: Int = 0, size: Int = this.size - index): DoubleArray = getArray(index, DoubleArray(size))
    fun setArray(index: Int, inp: DoubleArray, offset: Int = 0, size: Int = inp.size - offset): Unit = buffer.setArrayFloat64(index, inp, offset, size)

    fun slice(start: Int = 0, end: Int = this.size): Float64Buffer =
        Float64Buffer(buffer.slice(start * 8, end * 8))
    fun sliceWithSize(start: Int = 0, size: Int = this.size - start): Float64Buffer =
        Float64Buffer(buffer.sliceWithSize(start * 8, size * 8))
}

val Buffer.u8: Uint8Buffer get() = Uint8Buffer(this)
val Buffer.u16: Uint16Buffer get() = Uint16Buffer(this)
val Buffer.i8: Int8Buffer get() = Int8Buffer(this)
val Buffer.i16: Int16Buffer get() = Int16Buffer(this)
val Buffer.i32: Int32Buffer get() = Int32Buffer(this)
val Buffer.i64: Int64Buffer get() = Int64Buffer(this)
val Buffer.f32: Float32Buffer get() = Float32Buffer(this)
val Buffer.f64: Float64Buffer get() = Float64Buffer(this)

fun TypedBuffer.asUInt8(): Uint8Buffer = this.buffer.u8
fun TypedBuffer.asUInt16(): Uint16Buffer = this.buffer.u16
fun TypedBuffer.asInt8(): Int8Buffer = this.buffer.i8
fun TypedBuffer.asInt16(): Int16Buffer = this.buffer.i16
fun TypedBuffer.asInt32(): Int32Buffer = this.buffer.i32
fun TypedBuffer.asInt64(): Int64Buffer = this.buffer.i64
fun TypedBuffer.asFloat32(): Float32Buffer = this.buffer.f32
fun TypedBuffer.asFloat64(): Float64Buffer = this.buffer.f64

@Deprecated("") operator fun Buffer.get(index: Int): Byte = getInt8(index)
@Deprecated("") operator fun Buffer.set(index: Int, value: Byte) = setInt8(index, value)
@Deprecated("") operator fun Buffer.set(index: Int, value: Int) = setUInt8(index, value)

inline fun <T> BufferTemp(size: Int, callback: (Buffer) -> T): T = Buffer.allocDirect(size).run(callback)

fun ByteArray.toNBufferUInt8(): Uint8Buffer = Buffer(this).u8
