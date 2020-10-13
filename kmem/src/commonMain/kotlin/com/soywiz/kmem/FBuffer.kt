package com.soywiz.kmem

/**
 * FastBuffer holding a chunk of [mem] memory
 */
class FBuffer private constructor(val mem: MemBuffer, val size: Int = mem.size) {
	val buffer: MemBuffer get() = mem
	val data: DataBuffer = mem.getData()
	val arrayByte: Int8Buffer = mem.asInt8Buffer()
	val arrayShort: Int16Buffer = mem.asInt16Buffer()
	val arrayInt: Int32Buffer = mem.asInt32Buffer()
	val arrayFloat: Float32Buffer = mem.asFloat32Buffer()
	val arrayDouble: Float64Buffer = mem.asFloat64Buffer()

	inline val i8 get() = arrayByte
	inline val i16 get() = arrayShort
	inline val i32 get() = arrayInt
	inline val f32 get() = arrayFloat
	inline val f64 get() = arrayDouble

	inline val u8 get() = Uint8Buffer(arrayByte)
	inline val u16 get() = Uint16Buffer(arrayShort)

	companion object {
		private fun Int.sizeAligned() = (this + 0xF) and 0xF.inv()

        fun allocUnaligned(size: Int): FBuffer = FBuffer(MemBufferAlloc(size), size)
		fun alloc(size: Int): FBuffer = allocUnaligned(size.sizeAligned())
		fun wrap(buffer: MemBuffer, size: Int = buffer.size): FBuffer = FBuffer(buffer, size)
		fun wrap(array: ByteArray): FBuffer = FBuffer(MemBufferWrap(array), array.size)

		operator fun invoke(size: Int, direct: Boolean) = FBuffer(if (direct) MemBufferAlloc(size.sizeAligned()) else MemBufferAllocNoDirect(size.sizeAligned()), size)
		operator fun invoke(size: Int): FBuffer = FBuffer(MemBufferAlloc(size.sizeAligned()), size)
		operator fun invoke(buffer: MemBuffer, size: Int = buffer.size): FBuffer = FBuffer(buffer, size)
		operator fun invoke(array: ByteArray): FBuffer = FBuffer(MemBufferWrap(array), array.size)

		fun copy(src: FBuffer, srcPos: Int, dst: FBuffer, dstPos: Int, length: Int): Unit =
			arraycopy(src.buffer, srcPos, dst.buffer, dstPos, length)

		fun copy(src: FBuffer, srcPos: Int, dst: ByteArray, dstPos: Int, length: Int): Unit =
			arraycopy(src.buffer, srcPos, dst, dstPos, length)

		fun copy(src: ByteArray, srcPos: Int, dst: FBuffer, dstPos: Int, length: Int): Unit =
			arraycopy(src, srcPos, dst.buffer, dstPos, length)

		fun copyAligned(src: FBuffer, srcPosAligned: Int, dst: ShortArray, dstPosAligned: Int, length: Int): Unit =
			arraycopy(src.buffer, srcPosAligned, dst, dstPosAligned, length)

		fun copyAligned(src: ShortArray, srcPosAligned: Int, dst: FBuffer, dstPosAligned: Int, length: Int): Unit =
			arraycopy(src, srcPosAligned, dst.buffer, dstPosAligned, length)

		fun copyAligned(src: FBuffer, srcPosAligned: Int, dst: IntArray, dstPosAligned: Int, length: Int): Unit =
			arraycopy(src.buffer, srcPosAligned, dst, dstPosAligned, length)

		fun copyAligned(src: IntArray, srcPosAligned: Int, dst: FBuffer, dstPosAligned: Int, length: Int): Unit =
			arraycopy(src, srcPosAligned, dst.buffer, dstPosAligned, length)

		fun copyAligned(src: FBuffer, srcPosAligned: Int, dst: FloatArray, dstPosAligned: Int, length: Int): Unit =
			arraycopy(src.buffer, srcPosAligned, dst, dstPosAligned, length)

		fun copyAligned(src: FloatArray, srcPosAligned: Int, dst: FBuffer, dstPosAligned: Int, length: Int): Unit =
			arraycopy(src, srcPosAligned, dst.buffer, dstPosAligned, length)

		fun copyAligned(src: FBuffer, srcPosAligned: Int, dst: DoubleArray, dstPosAligned: Int, length: Int): Unit =
			arraycopy(src.buffer, srcPosAligned, dst, dstPosAligned, length)

		fun copyAligned(src: DoubleArray, srcPosAligned: Int, dst: FBuffer, dstPosAligned: Int, length: Int): Unit =
			arraycopy(src, srcPosAligned, dst.buffer, dstPosAligned, length)
	}

	operator fun get(index: Int): Int = i8[index].toInt() and 0xFF
	operator fun set(index: Int, value: Int): Unit { i8[index] = value.toByte() }

	fun getByte(index: Int): Byte = arrayByte[index]
	fun getShort(index: Int): Short = arrayShort[index]
	fun getInt(index: Int): Int = arrayInt[index]
	fun getFloat(index: Int): Float = arrayFloat[index]
	fun getDouble(index: Int): Double = arrayDouble[index]

	fun setByte(index: Int, value: Byte): Unit { arrayByte[index] = value }
	fun setShort(index: Int, value: Short): Unit { arrayShort[index] = value }
	fun setInt(index: Int, value: Int): Unit { arrayInt[index] = value }
	fun setFloat(index: Int, value: Float): Unit { arrayFloat[index] = value }
	fun setDouble(index: Int, value: Double): Unit { arrayDouble[index] = value }

	fun dispose() = Unit

    fun setAlignedUInt16(index: Int, value: Int): Unit { i16[index] = value.toShort() }
    fun getAlignedUInt16(index: Int): Int = i16[index].toInt() and 0xFFFF

    fun setAlignedInt16(index: Int, value: Short): Unit { i16[index] = value }
	fun getAlignedInt16(index: Int): Short = i16[index]
	fun setAlignedInt32(index: Int, value: Int): Unit { i32[index] = value }
	fun getAlignedInt32(index: Int): Int = i32[index]
    fun getAlignedInt64(index: Int): Long {
        val low = i32[index * 2 + 0]
        val high = i32[index * 2 + 1]
        return ((high.toLong() and 0xFFFFFFFFL) shl 32) or (low.toLong() and 0xFFFFFFFFL)
    }
	fun setAlignedFloat32(index: Int, value: Float): Unit { f32[index] = value }
	fun getAlignedFloat32(index: Int): Float = f32[index]
	fun setAlignedFloat64(index: Int, value: Double): Unit { f64[index] = value }
	fun getAlignedFloat64(index: Int): Double = f64[index]

	fun getUnalignedInt16(index: Int): Short = data.getShort(index)
	fun setUnalignedInt16(index: Int, value: Short): Unit { data.setShort(index, value) }
	fun setUnalignedInt32(index: Int, value: Int): Unit { data.setInt(index, value) }
	fun getUnalignedInt32(index: Int): Int = data.getInt(index)
	fun setUnalignedFloat32(index: Int, value: Float): Unit { data.setFloat(index, value) }
	fun getUnalignedFloat32(index: Int): Float = data.getFloat(index)
	fun setUnalignedFloat64(index: Int, value: Double): Unit { data.setDouble(index, value) }
	fun getUnalignedFloat64(index: Int): Double = data.getDouble(index)

	fun setArrayInt8(dstPos: Int, src: ByteArray, srcPos: Int, len: Int) = copy(src, srcPos, this, dstPos, len)
	fun setAlignedArrayInt8(dstPos: Int, src: ByteArray, srcPos: Int, len: Int) = copy(src, srcPos, this, dstPos, len)
	fun setAlignedArrayInt16(dstPos: Int, src: ShortArray, srcPos: Int, len: Int) =
		copyAligned(src, srcPos, this, dstPos, len)

	fun setAlignedArrayInt32(dstPos: Int, src: IntArray, srcPos: Int, len: Int) =
		copyAligned(src, srcPos, this, dstPos, len)

	fun setAlignedArrayFloat32(dstPos: Int, src: FloatArray, srcPos: Int, len: Int) =
		copyAligned(src, srcPos, this, dstPos, len)

	fun setAlignedArrayFloat64(dstPos: Int, src: DoubleArray, srcPos: Int, len: Int) =
		copyAligned(src, srcPos, this, dstPos, len)

	fun getArrayInt8(srcPos: Int, dst: ByteArray, dstPos: Int, len: Int) = copy(this, srcPos, dst, dstPos, len)
	fun getAlignedArrayInt8(srcPos: Int, dst: ByteArray, dstPos: Int, len: Int) = copy(this, srcPos, dst, dstPos, len)
	fun getAlignedArrayInt16(srcPos: Int, dst: ShortArray, dstPos: Int, len: Int) =
		copyAligned(this, srcPos, dst, dstPos, len)

	fun getAlignedArrayInt32(srcPos: Int, dst: IntArray, dstPos: Int, len: Int) =
		copyAligned(this, srcPos, dst, dstPos, len)

	fun getAlignedArrayFloat32(srcPos: Int, dst: FloatArray, dstPos: Int, len: Int) =
		copyAligned(this, srcPos, dst, dstPos, len)

	fun getAlignedArrayFloat64(srcPos: Int, dst: DoubleArray, dstPos: Int, len: Int) =
		copyAligned(this, srcPos, dst, dstPos, len)
}

inline class Uint8Buffer(val b: Int8Buffer) {
	companion object
	val size: Int get() = b.size
	operator fun get(index: Int): Int = b[index].toInt() and 0xFF
	operator fun set(index: Int, value: Int): Unit { b[index] = value.toByte() }
}

inline class Uint16Buffer(val b: Int16Buffer) {
	companion object
	val size: Int get() = b.size
	operator fun get(index: Int): Int = b[index].toInt() and 0xFFFF
	operator fun set(index: Int, value: Int): Unit { b[index] = value.toShort() }
}

fun Uint8BufferAlloc(size: Int): Uint8Buffer = Uint8Buffer(Int8BufferAlloc(size))
fun Uint16BufferAlloc(size: Int): Uint16Buffer = Uint16Buffer(Int16BufferAlloc(size))

inline fun <T> fbuffer(size: Int, callback: (FBuffer) -> T): T = FBuffer(size).run(callback)

//fun FBuffer.setFloats(offset: Int, data: FloatArray, dataOffset: Int, count: Int) = this.apply { for (n in 0 until count) this.setFloat(offset + n, data[dataOffset + n]) }
