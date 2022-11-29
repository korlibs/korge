package com.soywiz.kmem

expect class NBuffer
expect fun NBuffer(size: Int, direct: Boolean = false): NBuffer
expect fun NBuffer(array: ByteArray, offset: Int = 0, size: Int = array.size - offset): NBuffer
expect val NBuffer.byteOffset: Int
expect val NBuffer.sizeInBytes: Int
internal expect fun NBuffer.sliceInternal(start: Int, end: Int): NBuffer
fun NBuffer.sliceWithSize(start: Int, size: Int): NBuffer = slice(start, start + size)
fun NBuffer.slice(start: Int, end: Int): NBuffer {
    if (start > end) error("start:$start > end:$end")
    if (start !in 0 .. sizeInBytes) error("start:$start not in 0..$sizeInBytes")
    if (end !in 0 .. sizeInBytes) error("end:$end not in 0..$sizeInBytes")
    return sliceInternal(start, end)
}

// Unaligned versions

expect fun NBuffer.getUnalignedInt8(byteOffset: Int): Byte
expect fun NBuffer.getUnalignedInt16(byteOffset: Int): Short
expect fun NBuffer.getUnalignedInt32(byteOffset: Int): Int
expect fun NBuffer.getUnalignedInt64(byteOffset: Int): Long
expect fun NBuffer.getUnalignedFloat32(byteOffset: Int): Float
expect fun NBuffer.getUnalignedFloat64(byteOffset: Int): Double

expect fun NBuffer.setUnalignedInt8(byteOffset: Int, value: Byte)
expect fun NBuffer.setUnalignedInt16(byteOffset: Int, value: Short)
expect fun NBuffer.setUnalignedInt32(byteOffset: Int, value: Int)
expect fun NBuffer.setUnalignedInt64(byteOffset: Int, value: Long)
expect fun NBuffer.setUnalignedFloat32(byteOffset: Int, value: Float)
expect fun NBuffer.setUnalignedFloat64(byteOffset: Int, value: Double)

// Array versions

fun NBuffer.getUnalignedArrayInt8(byteOffset: Int, size: Int, out: ByteArray = ByteArray(size), offset: Int = 0): ByteArray { for (n in 0 until size) out[offset + n] = getUnalignedInt8(byteOffset + n * 1); return out }
fun NBuffer.getUnalignedArrayInt16(byteOffset: Int, size: Int, out: ShortArray = ShortArray(size), offset: Int = 0): ShortArray { for (n in 0 until size) out[offset + n] = getUnalignedInt16(byteOffset + n * 2); return out }
fun NBuffer.getUnalignedArrayInt32(byteOffset: Int, size: Int, out: IntArray = IntArray(size), offset: Int = 0): IntArray { for (n in 0 until size) out[offset + n] = getUnalignedInt32(byteOffset + n * 4); return out }
fun NBuffer.getUnalignedArrayInt64(byteOffset: Int, size: Int, out: LongArray = LongArray(size), offset: Int = 0): LongArray { for (n in 0 until size) out[offset + n] = getUnalignedInt64(byteOffset + n * 8); return out }
fun NBuffer.getUnalignedArrayFloat32(byteOffset: Int, size: Int, out: FloatArray = FloatArray(size), offset: Int = 0): FloatArray { for (n in 0 until size) out[offset + n] = getUnalignedFloat32(byteOffset + n * 4); return out }
fun NBuffer.getUnalignedArrayFloat64(byteOffset: Int, size: Int, out: DoubleArray = DoubleArray(size), offset: Int = 0): DoubleArray { for (n in 0 until size) out[offset + n] = getUnalignedFloat64(byteOffset + n * 8); return out }

fun NBuffer.setUnalignedArrayInt8(byteOffset: Int, inp: ByteArray, offset: Int = 0, size: Int = inp.size): Unit { for (n in 0 until size) setUnalignedInt8(byteOffset + n * 1, inp[offset + n]) }
fun NBuffer.setUnalignedArrayInt16(byteOffset: Int, inp: ShortArray, offset: Int = 0, size: Int = inp.size): Unit { for (n in 0 until size) setUnalignedInt16(byteOffset + n * 2, inp[offset + n]) }
fun NBuffer.setUnalignedArrayInt32(byteOffset: Int, inp: IntArray, offset: Int = 0, size: Int = inp.size): Unit { for (n in 0 until size) setUnalignedInt32(byteOffset + n * 4, inp[offset + n]) }
fun NBuffer.setUnalignedArrayInt64(byteOffset: Int, inp: LongArray, offset: Int = 0, size: Int = inp.size): Unit { for (n in 0 until size) setUnalignedInt64(byteOffset + n * 8, inp[offset + n]) }
fun NBuffer.setUnalignedArrayFloat32(byteOffset: Int, inp: FloatArray, offset: Int = 0, size: Int = inp.size): Unit { for (n in 0 until size) setUnalignedFloat32(byteOffset + n * 4, inp[offset + n]) }
fun NBuffer.setUnalignedArrayFloat64(byteOffset: Int, inp: DoubleArray, offset: Int = 0, size: Int = inp.size): Unit { for (n in 0 until size) setUnalignedFloat64(byteOffset + n * 8, inp[offset + n]) }

// Aligned versions

fun NBuffer.getArrayInt8(index: Int, size: Int, out: ByteArray = ByteArray(size), offset: Int = 0): ByteArray = getUnalignedArrayInt8(index * 1, size, out, offset)
fun NBuffer.getArrayInt16(index: Int, size: Int, out: ShortArray = ShortArray(size), offset: Int = 0): ShortArray = getUnalignedArrayInt16(index * 2, size, out, offset)
fun NBuffer.getArrayInt32(index: Int, size: Int, out: IntArray = IntArray(size), offset: Int = 0): IntArray = getUnalignedArrayInt32(index * 4, size, out, offset)
fun NBuffer.getArrayInt64(index: Int, size: Int, out: LongArray = LongArray(size), offset: Int = 0): LongArray = getUnalignedArrayInt64(index * 8, size, out, offset)
fun NBuffer.getArrayFloat32(index: Int, size: Int, out: FloatArray = FloatArray(size), offset: Int = 0): FloatArray = getUnalignedArrayFloat32(index * 4, size, out, offset)
fun NBuffer.getArrayFloat64(index: Int, size: Int, out: DoubleArray = DoubleArray(size), offset: Int = 0): DoubleArray = getUnalignedArrayFloat64(index * 8, size, out, offset)

fun NBuffer.setArrayInt8(index: Int, inp: ByteArray, offset: Int = 0, size: Int = inp.size): Unit = setUnalignedArrayInt8(index * 1, inp, offset, size)
fun NBuffer.setArrayInt16(index: Int, inp: ShortArray, offset: Int = 0, size: Int = inp.size): Unit = setUnalignedArrayInt16(index * 2, inp, offset, size)
fun NBuffer.setArrayInt32(index: Int, inp: IntArray, offset: Int = 0, size: Int = inp.size): Unit = setUnalignedArrayInt32(index * 4, inp, offset, size)
fun NBuffer.setArrayInt64(index: Int, inp: LongArray, offset: Int = 0, size: Int = inp.size): Unit = setUnalignedArrayInt64(index * 8, inp, offset, size)
fun NBuffer.setArrayFloat32(index: Int, inp: FloatArray, offset: Int = 0, size: Int = inp.size): Unit = setUnalignedArrayFloat32(index * 4, inp, offset, size)
fun NBuffer.setArrayFloat64(index: Int, inp: DoubleArray, offset: Int = 0, size: Int = inp.size): Unit = setUnalignedArrayFloat64(index * 8, inp, offset, size)

fun NBuffer.getInt8(index: Int): Byte = getUnalignedInt8(index)
fun NBuffer.getInt16(index: Int): Short = getUnalignedInt16(index * 2)
fun NBuffer.getInt32(index: Int): Int = getUnalignedInt32(index * 4)
fun NBuffer.getInt64(index: Int): Long = getUnalignedInt64(index * 8)
fun NBuffer.getFloat32(index: Int): Float = getUnalignedFloat32(index * 4)
fun NBuffer.getFloat64(index: Int): Double = getUnalignedFloat64(index * 8)

fun NBuffer.setInt8(index: Int, value: Byte) = setUnalignedInt8(index, value)
fun NBuffer.setInt16(index: Int, value: Short) = setUnalignedInt16(index * 2, value)
fun NBuffer.setInt32(index: Int, value: Int) = setUnalignedInt32(index * 4, value)
fun NBuffer.setInt64(index: Int, value: Long) = setUnalignedInt64(index * 8, value)
fun NBuffer.setFloat32(index: Int, value: Float) = setUnalignedFloat32(index * 4, value)
fun NBuffer.setFloat64(index: Int, value: Double) = setUnalignedFloat64(index * 8, value)

// Array versions

fun NBuffer.asInt8(): NBufferInt8 = NBufferInt8(this)
inline class NBufferInt8(val buffer: NBuffer) {
    constructor(size: Int) : this(NBuffer(size * 1))
    val size: Int get() = buffer.sizeInBytes
    operator fun get(index: Int): Byte = buffer.getInt8(index)
    operator fun set(index: Int, value: Byte) = buffer.setInt8(index, value)
    fun getArray(index: Int, size: Int, out: ByteArray = ByteArray(size), offset: Int = 0): ByteArray = buffer.getArrayInt8(index, size, out, offset)
    fun setArray(index: Int, inp: ByteArray, size: Int = inp.size, offset: Int = 0): Unit = buffer.setArrayInt8(index, inp, offset, size)

    fun slice(start: Int, end: Int): NBufferInt8 = NBufferInt8(buffer.slice(start, end))
    fun sliceWithSize(start: Int, size: Int): NBufferInt8 = NBufferInt8(buffer.sliceWithSize(start, size))
}

fun NBuffer.asInt16(): NBufferInt16 = NBufferInt16(this)
inline class NBufferInt16(val buffer: NBuffer) {
    constructor(size: Int) : this(NBuffer(size * 2))
    val size: Int get() = buffer.sizeInBytes / 2
    operator fun get(index: Int): Short = buffer.getInt16(index)
    operator fun set(index: Int, value: Short) = buffer.setInt16(index, value)
    fun getArray(index: Int, size: Int, out: ShortArray = ShortArray(size), offset: Int = 0): ShortArray = buffer.getArrayInt16(index, size, out, offset)
    fun setArray(index: Int, inp: ShortArray, size: Int = inp.size, offset: Int = 0): Unit = buffer.setArrayInt16(index, inp, offset, size)

    fun slice(start: Int, end: Int): NBufferInt16 = NBufferInt16(buffer.slice(start * 2, end * 2))
    fun sliceWithSize(start: Int, size: Int): NBufferInt16 = NBufferInt16(buffer.sliceWithSize(start * 2, size * 2))
}

fun NBuffer.asInt32(): NBufferInt32 = NBufferInt32(this)
inline class NBufferInt32(val buffer: NBuffer) {
    constructor(size: Int) : this(NBuffer(size * 4))
    val size: Int get() = buffer.sizeInBytes / 4
    operator fun get(index: Int): Int = buffer.getInt32(index)
    operator fun set(index: Int, value: Int) = buffer.setInt32(index, value)
    fun getArray(index: Int, size: Int, out: IntArray = IntArray(size), offset: Int = 0): IntArray = buffer.getArrayInt32(index, size, out, offset)
    fun setArray(index: Int, inp: IntArray, size: Int = inp.size, offset: Int = 0): Unit = buffer.setArrayInt32(index, inp, offset, size)

    fun slice(start: Int, end: Int): NBufferInt32 = NBufferInt32(buffer.slice(start * 4, end * 4))
    fun sliceWithSize(start: Int, size: Int): NBufferInt32 = NBufferInt32(buffer.sliceWithSize(start * 4, size * 4))
}

fun NBuffer.asInt64(): NBufferInt64 = NBufferInt64(this)
inline class NBufferInt64(val buffer: NBuffer) {
    constructor(size: Int) : this(NBuffer(size * 8))
    val size: Int get() = buffer.sizeInBytes / 8
    operator fun get(index: Int): Long = buffer.getInt64(index)
    operator fun set(index: Int, value: Long) = buffer.setInt64(index, value)
    fun getArray(index: Int, size: Int, out: LongArray = LongArray(size), offset: Int = 0): LongArray = buffer.getArrayInt64(index, size, out, offset)
    fun setArray(index: Int, inp: LongArray, size: Int = inp.size, offset: Int = 0): Unit = buffer.setArrayInt64(index, inp, offset, size)

    fun slice(start: Int, end: Int): NBufferInt64 = NBufferInt64(buffer.slice(start * 8, end * 8))
    fun sliceWithSize(start: Int, size: Int): NBufferInt64 = NBufferInt64(buffer.sliceWithSize(start * 8, size * 8))
}

fun NBuffer.asFloat32(): NBufferFloat32 = NBufferFloat32(this)
inline class NBufferFloat32(val buffer: NBuffer) {
    constructor(size: Int) : this(NBuffer(size * 4))
    val size: Int get() = buffer.sizeInBytes / 4
    operator fun get(index: Int): Float = buffer.getFloat32(index)
    operator fun set(index: Int, value: Float) = buffer.setFloat32(index, value)
    fun getArray(index: Int, size: Int, out: FloatArray = FloatArray(size), offset: Int = 0): FloatArray = buffer.getArrayFloat32(index, size, out, offset)
    fun setArray(index: Int, inp: FloatArray, size: Int = inp.size, offset: Int = 0): Unit = buffer.setArrayFloat32(index, inp, offset, size)

    fun slice(start: Int, end: Int): NBufferFloat32 = NBufferFloat32(buffer.slice(start * 4, end * 4))
    fun sliceWithSize(start: Int, size: Int): NBufferFloat32 = NBufferFloat32(buffer.sliceWithSize(start * 4, size * 4))
}

fun NBuffer.asFloat64(): NBufferFloat64 = NBufferFloat64(this)
inline class NBufferFloat64(val buffer: NBuffer) {
    constructor(size: Int) : this(NBuffer(size * 8))
    val size: Int get() = buffer.sizeInBytes / 8
    operator fun get(index: Int): Double = buffer.getFloat64(index)
    operator fun set(index: Int, value: Double) = buffer.setFloat64(index, value)
    fun getArray(index: Int, size: Int, out: DoubleArray = DoubleArray(size), offset: Int = 0): DoubleArray = buffer.getArrayFloat64(index, size, out, offset)
    fun setArray(index: Int, inp: DoubleArray, size: Int = inp.size, offset: Int = 0): Unit = buffer.setArrayFloat64(index, inp, offset, size)

    fun slice(start: Int, end: Int): NBufferFloat64 = NBufferFloat64(buffer.slice(start * 8, end * 8))
    fun sliceWithSize(start: Int, size: Int): NBufferFloat64 = NBufferFloat64(buffer.sliceWithSize(start * 8, size * 8))
}
