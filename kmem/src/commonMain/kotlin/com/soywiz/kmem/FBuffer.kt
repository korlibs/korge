package com.soywiz.kmem

import com.soywiz.kmem.FBuffer.Companion.sizeAligned

/**
 * FastBuffer holding a chunk of [mem] memory
 */
public class FBuffer private constructor(public val mem: MemBuffer, public val size: Int = mem.size) {
	public val buffer: MemBuffer get() = mem
	public val data: DataBuffer = mem.getData()
	public val arrayByte: Int8Buffer = mem.asInt8Buffer()
	public val arrayShort: Int16Buffer = mem.asInt16Buffer()
	public val arrayInt: Int32Buffer = mem.asInt32Buffer()
	public val arrayFloat: Float32Buffer = mem.asFloat32Buffer()
	public val arrayDouble: Float64Buffer = mem.asFloat64Buffer()
    public val fast32: Fast32Buffer = NewFast32Buffer(mem)

	public inline val i8: Int8Buffer get() = arrayByte
	public inline val i16: Int16Buffer get() = arrayShort
	public inline val i32: Int32Buffer get() = arrayInt
	public inline val f32: Float32Buffer get() = arrayFloat
	public inline val f64: Float64Buffer get() = arrayDouble

	public inline val u8: Uint8Buffer get() = Uint8Buffer(arrayByte)
	public inline val u16: Uint16Buffer get() = Uint16Buffer(arrayShort)

    public companion object {
        public fun getSizeAligned(size: Int): Int = (size + 0xF) and 0xF.inv()

		private fun Int.sizeAligned() = getSizeAligned(this)

        public fun allocUnaligned(size: Int): FBuffer = FBuffer(MemBufferAlloc(size), size)
		public fun alloc(size: Int): FBuffer = allocUnaligned(size.sizeAligned())

        public fun allocNoDirectUnaligned(size: Int): FBuffer = FBuffer(MemBufferAllocNoDirect(size), size)
        public fun allocNoDirect(size: Int): FBuffer = allocNoDirectUnaligned(size.sizeAligned())

		public fun wrap(buffer: MemBuffer, size: Int = buffer.size): FBuffer = FBuffer(buffer, size)
		public fun wrap(array: ByteArray): FBuffer = FBuffer(MemBufferWrap(array), array.size)

		public operator fun invoke(size: Int, direct: Boolean): FBuffer = FBuffer(if (direct) MemBufferAlloc(size.sizeAligned()) else MemBufferAllocNoDirect(size.sizeAligned()), size)
		public operator fun invoke(size: Int): FBuffer = FBuffer(MemBufferAlloc(size.sizeAligned()), size)
		public operator fun invoke(buffer: MemBuffer, size: Int = buffer.size): FBuffer = FBuffer(buffer, size)
		public operator fun invoke(array: ByteArray): FBuffer = FBuffer(MemBufferWrap(array), array.size)

        public fun copy(src: FBuffer, srcPos: Int, dst: FBuffer, dstPos: Int, length: Int): Unit =
			arraycopy(src.buffer, srcPos, dst.buffer, dstPos, length)

        public fun copy(src: FBuffer, srcPos: Int, dst: ByteArray, dstPos: Int, length: Int): Unit =
			arraycopy(src.buffer, srcPos, dst, dstPos, length)

        public fun copy(src: ByteArray, srcPos: Int, dst: FBuffer, dstPos: Int, length: Int): Unit =
			arraycopy(src, srcPos, dst.buffer, dstPos, length)

        public fun copyAligned(src: FBuffer, srcPosAligned: Int, dst: ShortArray, dstPosAligned: Int, length: Int): Unit =
			arraycopy(src.buffer, srcPosAligned, dst, dstPosAligned, length)

        public fun copyAligned(src: ShortArray, srcPosAligned: Int, dst: FBuffer, dstPosAligned: Int, length: Int): Unit =
			arraycopy(src, srcPosAligned, dst.buffer, dstPosAligned, length)

        public fun copyAligned(src: FBuffer, srcPosAligned: Int, dst: IntArray, dstPosAligned: Int, length: Int): Unit =
			arraycopy(src.buffer, srcPosAligned, dst, dstPosAligned, length)

        public fun copyAligned(src: IntArray, srcPosAligned: Int, dst: FBuffer, dstPosAligned: Int, length: Int): Unit =
			arraycopy(src, srcPosAligned, dst.buffer, dstPosAligned, length)

        public fun copyAligned(src: FBuffer, srcPosAligned: Int, dst: FloatArray, dstPosAligned: Int, length: Int): Unit =
			arraycopy(src.buffer, srcPosAligned, dst, dstPosAligned, length)

        public fun copyAligned(src: FloatArray, srcPosAligned: Int, dst: FBuffer, dstPosAligned: Int, length: Int): Unit =
			arraycopy(src, srcPosAligned, dst.buffer, dstPosAligned, length)

        public fun copyAligned(src: FBuffer, srcPosAligned: Int, dst: DoubleArray, dstPosAligned: Int, length: Int): Unit =
			arraycopy(src.buffer, srcPosAligned, dst, dstPosAligned, length)

        public fun copyAligned(src: DoubleArray, srcPosAligned: Int, dst: FBuffer, dstPosAligned: Int, length: Int): Unit =
			arraycopy(src, srcPosAligned, dst.buffer, dstPosAligned, length)
	}

    public operator fun get(index: Int): Int = i8[index].toInt() and 0xFF
    public operator fun set(index: Int, value: Int): Unit { i8[index] = value.toByte() }

	public fun getByte(index: Int): Byte = arrayByte[index]
	public fun getShort(index: Int): Short = arrayShort[index]
	public fun getInt(index: Int): Int = arrayInt[index]
	public fun getFloat(index: Int): Float = arrayFloat[index]
	public fun getDouble(index: Int): Double = arrayDouble[index]

	public fun setByte(index: Int, value: Byte): Unit { arrayByte[index] = value }
	public fun setShort(index: Int, value: Short): Unit { arrayShort[index] = value }
	public fun setInt(index: Int, value: Int): Unit { arrayInt[index] = value }
	public fun setFloat(index: Int, value: Float): Unit { arrayFloat[index] = value }
	public fun setDouble(index: Int, value: Double): Unit { arrayDouble[index] = value }

    public fun dispose(): Unit = Unit

    public fun setAlignedInt8(index: Int, value: Int): Unit { i8[index] = value.toByte() }
    public fun getAlignedInt8(index: Int): Int = i8[index].toInt()

    public fun setAlignedUInt8(index: Int, value: Int): Unit { u8[index] = value }
    public fun getAlignedUInt8(index: Int): Int = u8[index].toInt() and 0xFF

    public fun setAlignedUInt16(index: Int, value: Int): Unit { i16[index] = value.toShort() }
    public fun getAlignedUInt16(index: Int): Int = i16[index].toInt() and 0xFFFF

    public fun setAlignedInt16(index: Int, value: Short): Unit { i16[index] = value }
	public fun getAlignedInt16(index: Int): Short = i16[index]
	public fun setAlignedInt32(index: Int, value: Int): Unit { i32[index] = value }
	public fun getAlignedInt32(index: Int): Int = i32[index]
    public fun getAlignedInt64(index: Int): Long {
        val low = i32[index * 2 + 0]
        val high = i32[index * 2 + 1]
        return ((high.toLong() and 0xFFFFFFFFL) shl 32) or (low.toLong() and 0xFFFFFFFFL)
    }
	public fun setAlignedFloat32(index: Int, value: Float): Unit { f32[index] = value }
	public fun getAlignedFloat32(index: Int): Float = f32[index]
	public fun setAlignedFloat64(index: Int, value: Double): Unit { f64[index] = value }
	public fun getAlignedFloat64(index: Int): Double = f64[index]

    public fun getUnalignedInt8(index: Int): Byte = data.getByte(index)
    public fun setUnalignedInt8(index: Int, value: Byte): Unit { data.setByte(index, value) }

    public fun getUnalignedUInt8(index: Int): Int = data.getByte(index).toInt() and 0xFF
    public fun setUnalignedUInt8(index: Int, value: Int): Unit { data.setByte(index, value.toByte()) }

    public fun getUnalignedInt16(index: Int): Short = data.getShort(index)
	public fun setUnalignedInt16(index: Int, value: Short): Unit { data.setShort(index, value) }

    public fun getUnalignedUInt16(index: Int): Int = data.getShort(index).toInt() and 0xFFFF
    public fun setUnalignedUInt16(index: Int, value: Int): Unit { data.setShort(index, value.toShort()) }

    public fun setUnalignedInt32(index: Int, value: Int): Unit { data.setInt(index, value) }
	public fun getUnalignedInt32(index: Int): Int = data.getInt(index)
	public fun setUnalignedFloat32(index: Int, value: Float): Unit { data.setFloat(index, value) }
	public fun getUnalignedFloat32(index: Int): Float = data.getFloat(index)
	public fun setUnalignedFloat64(index: Int, value: Double): Unit { data.setDouble(index, value) }
	public fun getUnalignedFloat64(index: Int): Double = data.getDouble(index)

	public fun setArrayInt8(dstPos: Int, src: ByteArray, srcPos: Int, len: Int): Unit = copy(src, srcPos, this, dstPos, len)
	public fun setAlignedArrayInt8(dstPos: Int, src: ByteArray, srcPos: Int, len: Int): Unit = copy(src, srcPos, this, dstPos, len)
	public fun setAlignedArrayInt16(dstPos: Int, src: ShortArray, srcPos: Int, len: Int): Unit =
		copyAligned(src, srcPos, this, dstPos, len)

    public fun setAlignedArrayInt32(dstPos: Int, src: IntArray, srcPos: Int, len: Int): Unit =
		copyAligned(src, srcPos, this, dstPos, len)

    public fun setAlignedArrayFloat32(dstPos: Int, src: FloatArray, srcPos: Int, len: Int): Unit =
		copyAligned(src, srcPos, this, dstPos, len)

    public fun setAlignedArrayFloat64(dstPos: Int, src: DoubleArray, srcPos: Int, len: Int): Unit =
		copyAligned(src, srcPos, this, dstPos, len)

	public fun getArrayInt8(srcPos: Int, dst: ByteArray, dstPos: Int, len: Int): Unit = copy(this, srcPos, dst, dstPos, len)
	public fun getAlignedArrayInt8(srcPos: Int, dst: ByteArray, dstPos: Int, len: Int): Unit = copy(this, srcPos, dst, dstPos, len)
	public fun getAlignedArrayInt16(srcPos: Int, dst: ShortArray, dstPos: Int, len: Int): Unit =
		copyAligned(this, srcPos, dst, dstPos, len)

    public fun getAlignedArrayInt32(srcPos: Int, dst: IntArray, dstPos: Int, len: Int): Unit =
		copyAligned(this, srcPos, dst, dstPos, len)

    public fun getAlignedArrayFloat32(srcPos: Int, dst: FloatArray, dstPos: Int, len: Int): Unit =
		copyAligned(this, srcPos, dst, dstPos, len)

    public fun getAlignedArrayFloat64(srcPos: Int, dst: DoubleArray, dstPos: Int, len: Int): Unit =
		copyAligned(this, srcPos, dst, dstPos, len)
}

public inline class Uint8Buffer(public val b: Int8Buffer) {
	public companion object;
    public val size: Int get() = b.size
	public operator fun get(index: Int): Int = b[index].toInt() and 0xFF
	public operator fun set(index: Int, value: Int): Unit { b[index] = value.toByte() }
}

public inline class Uint16Buffer(public val b: Int16Buffer) {
	public companion object;
	public val size: Int get() = b.size
	public operator fun get(index: Int): Int = b[index].toInt() and 0xFFFF
	public operator fun set(index: Int, value: Int): Unit { b[index] = value.toShort() }
}

public fun Uint8BufferAlloc(size: Int): Uint8Buffer = Uint8Buffer(Int8BufferAlloc(size))
public fun Uint16BufferAlloc(size: Int): Uint16Buffer = Uint16Buffer(Int16BufferAlloc(size))

public inline fun <T> fbuffer(size: Int, callback: (FBuffer) -> T): T = FBuffer(size).run(callback)

//fun FBuffer.setFloats(offset: Int, data: FloatArray, dataOffset: Int, count: Int) = this.apply { for (n in 0 until count) this.setFloat(offset + n, data[dataOffset + n]) }
