package com.soywiz.kmem

actual class NBuffer(val data: ByteArray, val offset: Int, val size: Int, dummy: Unit) {
    val end: Int = offset + size
    actual companion object
}
actual fun NBuffer(size: Int, direct: Boolean): NBuffer {
    checkNBufferSize(size)
    return NBuffer(ByteArray(size), 0, size, Unit)
}
actual fun NBuffer(array: ByteArray, offset: Int, size: Int): NBuffer {
    checkNBufferWrap(array, offset, size)
    return NBuffer(array, offset, size, Unit)
}
actual val NBuffer.byteOffset: Int get() = offset
actual val NBuffer.sizeInBytes: Int get() = size
internal actual fun NBuffer.sliceInternal(start: Int, end: Int): NBuffer = NBuffer(data, offset + start, end - start)

actual fun NBuffer.Companion.copy(src: NBuffer, srcPosBytes: Int, dst: NBuffer, dstPosBytes: Int, sizeInBytes: Int) {
    arraycopy(
        src.data, src.offset + srcPosBytes,
        dst.data, dst.offset + dstPosBytes,
        sizeInBytes
    )
}

// Unaligned versions

actual fun NBuffer.getUnalignedInt8(byteOffset: Int): Byte = data[offset + byteOffset]
actual fun NBuffer.getUnalignedInt16(byteOffset: Int): Short = data.getShortAt(offset + byteOffset)
actual fun NBuffer.getUnalignedInt32(byteOffset: Int): Int = data.getIntAt(offset + byteOffset)
actual fun NBuffer.getUnalignedInt64(byteOffset: Int): Long = data.getLongAt(offset + byteOffset)
actual fun NBuffer.getUnalignedFloat32(byteOffset: Int): Float = data.getFloatAt(offset + byteOffset)
actual fun NBuffer.getUnalignedFloat64(byteOffset: Int): Double = data.getDoubleAt(offset + byteOffset)

actual fun NBuffer.setUnalignedInt8(byteOffset: Int, value: Byte) { data[offset + byteOffset] = value }
actual fun NBuffer.setUnalignedInt16(byteOffset: Int, value: Short) { data.setShortAt(offset + byteOffset, value) }
actual fun NBuffer.setUnalignedInt32(byteOffset: Int, value: Int) { data.setIntAt(offset + byteOffset, value) }
actual fun NBuffer.setUnalignedInt64(byteOffset: Int, value: Long) { data.setLongAt(offset + byteOffset, value) }
actual fun NBuffer.setUnalignedFloat32(byteOffset: Int, value: Float) { data.setFloatAt(offset + byteOffset, value) }
actual fun NBuffer.setUnalignedFloat64(byteOffset: Int, value: Double) { data.setDoubleAt(offset + byteOffset, value) }
