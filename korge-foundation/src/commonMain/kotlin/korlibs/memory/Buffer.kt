@file:OptIn(ExperimentalUnsignedTypes::class)

package korlibs.memory

import korlibs.math.*
import korlibs.memory.arrays.*
import kotlin.jvm.*

class Buffer(val data: DataView) {
    constructor(size: Int, direct: Boolean = false) : this(ArrayBuffer(checkNBufferSize(size), direct).dataView())
    constructor(array: ByteArray, offset: Int = 0, size: Int = array.size - offset) : this(
        ArrayBufferWrap(checkNBufferWrap(array, offset, size)).dataView(offset, size)
    )

    fun copyOf(newSize: Int): Buffer {
        val out = Buffer(newSize)
        arraycopy(this, 0, out, 0, kotlin.math.min(this.sizeInBytes, newSize))
        return out
    }
    fun clone(direct: Boolean = false): Buffer {
        val out = Buffer(this.size, direct)
        arraycopy(this, 0, out, 0, size)
        return out
    }

    internal val byteOffset: Int get() = data.byteOffset
    val sizeInBytes: Int get() = data.byteLength
    val size: Int get() = sizeInBytes

    internal fun sliceInternal(start: Int, end: Int): Buffer = Buffer(data.subarray(start, end))
    fun sliceWithSize(start: Int, size: Int): Buffer = slice(start, start + size)
    fun slice(start: Int = 0, end: Int = sizeInBytes): Buffer {
        if (start > end || start !in 0 .. sizeInBytes || end !in 0 .. sizeInBytes) {
            throw IllegalArgumentException("invalid slice start:$start, end:$end not in 0..$sizeInBytes")
        }
        return sliceInternal(start, end)
    }

    // @TODO: Optimize by using words instead o bytes
    override fun hashCode(): Int {
        var h = 1
        for (n in 0 until size) h = 31 * h + data.getS8(n)
        return h
    }

    // @TODO: Optimize by using words instead o bytes
    override fun equals(other: Any?): Boolean {
        if (other !is Buffer || this.size != other.size) return false
        val t = this.data
        val o = other.data
        for (n in 0 until size) if (t.getS8(n) != o.getS8(n)) return false
        return true
    }

    override fun toString(): String = NBuffer_toString(this)

    // Unaligned versions

    fun getUnalignedUInt8(byteOffset: Int): Int = getUnalignedInt8(byteOffset).toInt() and 0xFF
    fun getUnalignedUInt16(byteOffset: Int): Int = getUnalignedInt16(byteOffset).toInt() and 0xFFFF
    fun getUnalignedInt8(byteOffset: Int): Byte = data.getS8(byteOffset)
    fun getUnalignedInt16(byteOffset: Int): Short = data.getS16LE(byteOffset)
    fun getUnalignedInt32(byteOffset: Int): Int = data.getS32LE(byteOffset)
    fun getUnalignedInt64(byteOffset: Int): Long = data.getS64LE(byteOffset)
    fun getUnalignedFloat32(byteOffset: Int): Float = data.getF32LE(byteOffset)
    fun getUnalignedFloat64(byteOffset: Int): Double = data.getF64LE(byteOffset)

    fun setUnalignedUInt8(byteOffset: Int, value: Int) = setUnalignedInt8(byteOffset, value.toByte())
    fun setUnalignedUInt8Clamped(byteOffset: Int, value: Int) = setUnalignedInt8(byteOffset, value.clampUByte().toByte())
    fun setUnalignedUInt16(byteOffset: Int, value: Int) = setUnalignedInt16(byteOffset, value.toShort())
    fun setUnalignedInt8(byteOffset: Int, value: Int) = setUnalignedInt8(byteOffset, value.toByte())
    fun setUnalignedInt8(byteOffset: Int, value: Byte) = data.setS8(byteOffset, value)
    fun setUnalignedInt16(byteOffset: Int, value: Short) = data.setS16LE(byteOffset, value)
    fun setUnalignedInt32(byteOffset: Int, value: Int) = data.setS32LE(byteOffset, value)
    fun setUnalignedInt64(byteOffset: Int, value: Long) = data.setS64LE(byteOffset, value)
    fun setUnalignedFloat32(byteOffset: Int, value: Float) = data.setF32LE(byteOffset, value)
    fun setUnalignedFloat64(byteOffset: Int, value: Double) = data.setF64LE(byteOffset, value)

// Array versions

    fun getUnalignedArrayInt8(byteOffset: Int, out: ByteArray, offset: Int = 0, size: Int = out.size - offset): ByteArray { for (n in 0 until size) out[offset + n] = getUnalignedInt8(byteOffset + n * 1); return out }
    fun getUnalignedArrayInt16(byteOffset: Int, out: ShortArray, offset: Int = 0, size: Int = out.size - offset): ShortArray { for (n in 0 until size) out[offset + n] = getUnalignedInt16(byteOffset + n * 2); return out }
    fun getUnalignedArrayInt32(byteOffset: Int, out: IntArray, offset: Int = 0, size: Int = out.size - offset): IntArray { for (n in 0 until size) out[offset + n] = getUnalignedInt32(byteOffset + n * 4); return out }
    fun getUnalignedArrayInt64(byteOffset: Int, out: LongArray, offset: Int = 0, size: Int = out.size - offset): LongArray { for (n in 0 until size) out[offset + n] = getUnalignedInt64(byteOffset + n * 8); return out }
    fun getUnalignedArrayFloat32(byteOffset: Int, out: FloatArray, offset: Int = 0, size: Int = out.size - offset): FloatArray { for (n in 0 until size) out[offset + n] = getUnalignedFloat32(byteOffset + n * 4); return out }
    fun getUnalignedArrayFloat64(byteOffset: Int, out: DoubleArray, offset: Int = 0, size: Int = out.size - offset): DoubleArray { for (n in 0 until size) out[offset + n] = getUnalignedFloat64(byteOffset + n * 8); return out }

    fun setUnalignedArrayInt8(byteOffset: Int, inp: ByteArray, offset: Int = 0, size: Int = inp.size - offset) { for (n in 0 until size) setUnalignedInt8(byteOffset + n * 1, inp[offset + n]) }
    fun setUnalignedArrayInt16(byteOffset: Int, inp: ShortArray, offset: Int = 0, size: Int = inp.size - offset) { for (n in 0 until size) setUnalignedInt16(byteOffset + n * 2, inp[offset + n]) }
    fun setUnalignedArrayInt32(byteOffset: Int, inp: IntArray, offset: Int = 0, size: Int = inp.size - offset) { for (n in 0 until size) setUnalignedInt32(byteOffset + n * 4, inp[offset + n]) }
    fun setUnalignedArrayInt64(byteOffset: Int, inp: LongArray, offset: Int = 0, size: Int = inp.size - offset) { for (n in 0 until size) setUnalignedInt64(byteOffset + n * 8, inp[offset + n]) }
    fun setUnalignedArrayFloat32(byteOffset: Int, inp: FloatArray, offset: Int = 0, size: Int = inp.size - offset) { for (n in 0 until size) setUnalignedFloat32(byteOffset + n * 4, inp[offset + n]) }
    fun setUnalignedArrayFloat64(byteOffset: Int, inp: DoubleArray, offset: Int = 0, size: Int = inp.size - offset) { for (n in 0 until size) setUnalignedFloat64(byteOffset + n * 8, inp[offset + n]) }

    fun getUInt8(index: Int): Int = getUnalignedUInt8(index)
    fun getUInt16(index: Int): Int = getUnalignedUInt16(index * 2) and 0xFFFF
    fun getInt8(index: Int): Byte = getUnalignedInt8(index)
    fun getInt16(index: Int): Short = getUnalignedInt16(index * 2)
    fun getInt32(index: Int): Int = getUnalignedInt32(index * 4)
    fun getInt64(index: Int): Long = getUnalignedInt64(index * 8)
    fun getFloat32(index: Int): Float = getUnalignedFloat32(index * 4)
    fun getFloat64(index: Int): Double = getUnalignedFloat64(index * 8)

    fun setUInt8(index: Int, value: Int) = setUnalignedUInt8(index, value)
    fun setUInt8Clamped(index: Int, value: Int) = setUnalignedUInt8Clamped(index, value)
    fun setUInt16(index: Int, value: Int) = setUnalignedUInt16(index * 2, value)
    fun setInt8(index: Int, value: Byte) = setUnalignedInt8(index, value)
    fun setInt8(index: Int, value: Int) = setUnalignedInt8(index, value)
    fun setInt16(index: Int, value: Short) = setUnalignedInt16(index * 2, value)
    fun setInt32(index: Int, value: Int) = setUnalignedInt32(index * 4, value)
    fun setInt64(index: Int, value: Long) = setUnalignedInt64(index * 8, value)
    fun setFloat32(index: Int, value: Float) = setUnalignedFloat32(index * 4, value)
    fun setFloat64(index: Int, value: Double) = setUnalignedFloat64(index * 8, value)

// ALIGNED ARRAYS

    fun getArrayUInt8(index: Int, out: UByteArrayInt, offset: Int = 0, size: Int = out.size - offset): UByteArrayInt = UByteArrayInt(getUnalignedArrayInt8(index * 1, out.data, offset, size))
    fun getArrayUInt16(index: Int, out: UShortArrayInt, offset: Int = 0, size: Int = out.size - offset): UShortArrayInt = UShortArrayInt(getUnalignedArrayInt16(index * 2, out.data, offset, size))
    fun getArrayInt8(index: Int, out: ByteArray, offset: Int = 0, size: Int = out.size - offset): ByteArray = getUnalignedArrayInt8(index * 1, out, offset, size)
    fun getArrayInt16(index: Int, out: ShortArray, offset: Int = 0, size: Int = out.size - offset): ShortArray = getUnalignedArrayInt16(index * 2, out, offset, size)
    fun getArrayInt32(index: Int, out: IntArray, offset: Int = 0, size: Int = out.size - offset): IntArray = getUnalignedArrayInt32(index * 4, out, offset, size)
    fun getArrayInt64(index: Int, out: LongArray, offset: Int = 0, size: Int = out.size - offset): LongArray = getUnalignedArrayInt64(index * 8, out, offset, size)
    fun getArrayFloat32(index: Int, out: FloatArray, offset: Int = 0, size: Int = out.size - offset): FloatArray = getUnalignedArrayFloat32(index * 4, out, offset, size)
    fun getArrayFloat64(index: Int, out: DoubleArray, offset: Int = 0, size: Int = out.size - offset): DoubleArray = getUnalignedArrayFloat64(index * 8, out, offset, size)

    fun setArrayUInt8(index: Int, inp: UByteArrayInt, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayInt8(index * 1, inp.data, offset, size)
    fun setArrayUInt16(index: Int, inp: UShortArrayInt, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayInt16(index * 2, inp.data, offset, size)
    fun setArrayInt8(index: Int, inp: ByteArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayInt8(index * 1, inp, offset, size)
    fun setArrayInt16(index: Int, inp: ShortArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayInt16(index * 2, inp, offset, size)
    fun setArrayInt32(index: Int, inp: IntArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayInt32(index * 4, inp, offset, size)
    fun setArrayInt64(index: Int, inp: LongArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayInt64(index * 8, inp, offset, size)
    fun setArrayFloat32(index: Int, inp: FloatArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayFloat32(index * 4, inp, offset, size)
    fun setArrayFloat64(index: Int, inp: DoubleArray, offset: Int = 0, size: Int = inp.size - offset): Unit = setUnalignedArrayFloat64(index * 8, inp, offset, size)


    companion object {
        fun allocDirect(size: Int): Buffer = Buffer(size, direct = true)
        fun allocNoDirect(size: Int): Buffer = Buffer(size, direct = false)

        fun copy(src: Buffer, srcPosBytes: Int, dst: Buffer, dstPosBytes: Int, sizeInBytes: Int) {
            arraycopy(
                src.data.buffer, src.data.byteOffset + srcPosBytes,
                dst.data.buffer, dst.data.byteOffset + dstPosBytes,
                sizeInBytes
            )
        }
        fun equals(src: Buffer, srcPosBytes: Int, dst: Buffer, dstPosBytes: Int, sizeInBytes: Int): Boolean {
            return equalsCommon(src, srcPosBytes, dst, dstPosBytes, sizeInBytes)
        }

        internal fun equalsCommon(
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
                    val v0 = src.getUnalignedInt64(srcPosBytes + offset + n * WORD)
                    val v1 = dst.getUnalignedInt64(dstPosBytes + offset + n * WORD)
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
                    val v0 = src.getUnalignedInt32(srcPosBytes + offset + n * WORD)
                    val v1 = dst.getUnalignedInt32(dstPosBytes + offset + n * WORD)
                    if (v0 != v1) {
                        return false
                    }
                }
                offset += words * WORD
            }

            if (true) {
                for (n in 0 until remaining) {
                    val v0 = src.getUnalignedInt8(srcPosBytes + offset + n)
                    val v1 = dst.getUnalignedInt8(dstPosBytes + offset + n)
                    if (v0 != v1) {
                        return false
                    }
                }
            }
            return true
        }

    }
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

@JvmInline
value class Int8Buffer(override val buffer: Buffer) : TypedBuffer {
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

@JvmInline
value class Int16Buffer(override val buffer: Buffer) : TypedBuffer {
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

@JvmInline
value class Uint8Buffer(override val buffer: Buffer) : TypedBuffer, BaseIntBuffer {
    constructor(size: Int, direct: Boolean = false) : this(Buffer(size * 1, direct))
    constructor(data: UByteArrayInt, offset: Int = 0, size: Int = data.size - offset) : this(Buffer(size).also { it.setArrayUInt8(0, data, offset, size) })
    companion object {
        operator fun invoke(data: ByteArray) = Uint8Buffer(UByteArrayInt(data))
        operator fun invoke(data: UByteArray) = Uint8Buffer(UByteArrayInt(data.toByteArray()))
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


@JvmInline
value class Uint8ClampedBuffer(override val buffer: Buffer) : TypedBuffer, BaseIntBuffer {
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

@JvmInline
value class Uint16Buffer(override val buffer: Buffer) : TypedBuffer, BaseIntBuffer {
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

@JvmInline
value class Int32Buffer(override val buffer: Buffer) : TypedBuffer, BaseIntBuffer {
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

@JvmInline
value class Uint32Buffer(override val buffer: Buffer) : TypedBuffer {
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

@JvmInline
value class Int64Buffer(override val buffer: Buffer) : TypedBuffer {
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

@JvmInline
value class Float32Buffer(override val buffer: Buffer) : TypedBuffer, BaseFloatBuffer {
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

@JvmInline
value class Float64Buffer(override val buffer: Buffer) : TypedBuffer {
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

inline fun <T> BufferTemp(size: Int, callback: (Buffer) -> T): T = Buffer.allocDirect(size).run(callback)

fun ByteArray.toNBufferUInt8(): Uint8Buffer = Buffer(this).u8
