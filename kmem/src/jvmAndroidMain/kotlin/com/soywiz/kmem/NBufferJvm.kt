package com.soywiz.kmem

import java.nio.*

actual class NBuffer(val buffer: ByteBuffer, val offset: Int, val size: Int) {
    val end: Int get() = offset + size
}
actual fun NBuffer(size: Int, direct: Boolean): NBuffer = NBuffer(
    if (direct) ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder()) else ByteBuffer.allocate(size).order(ByteOrder.nativeOrder()),
    0, size
)
actual fun NBuffer(array: ByteArray, offset: Int, size: Int): NBuffer =
    NBuffer(ByteBuffer.wrap(array, offset, size).order(ByteOrder.nativeOrder()), offset, size)
actual val NBuffer.byteOffset: Int get() = offset
actual val NBuffer.sizeInBytes: Int get() = size
actual fun NBuffer.sliceInternal(start: Int, end: Int): NBuffer = NBuffer(buffer, offset + start, end - start)

// Unaligned versions

actual fun NBuffer.getUnalignedInt8(byteOffset: Int): Byte = buffer.get(offset + byteOffset)
actual fun NBuffer.getUnalignedInt16(byteOffset: Int): Short = buffer.getShort(offset + byteOffset)
actual fun NBuffer.getUnalignedInt32(byteOffset: Int): Int = buffer.getInt(offset + byteOffset)
actual fun NBuffer.getUnalignedInt64(byteOffset: Int): Long = buffer.getLong(offset + byteOffset)
actual fun NBuffer.getUnalignedFloat32(byteOffset: Int): Float = buffer.getFloat(offset + byteOffset)
actual fun NBuffer.getUnalignedFloat64(byteOffset: Int): Double = buffer.getDouble(offset + byteOffset)

actual fun NBuffer.setUnalignedInt8(byteOffset: Int, value: Byte) { buffer.put(offset + byteOffset, value) }
actual fun NBuffer.setUnalignedInt16(byteOffset: Int, value: Short) { buffer.putShort(offset + byteOffset, value) }
actual fun NBuffer.setUnalignedInt32(byteOffset: Int, value: Int) { buffer.putInt(offset + byteOffset, value) }
actual fun NBuffer.setUnalignedInt64(byteOffset: Int, value: Long) { buffer.putLong(offset + byteOffset, value) }
actual fun NBuffer.setUnalignedFloat32(byteOffset: Int, value: Float) { buffer.putFloat(offset + byteOffset, value) }
actual fun NBuffer.setUnalignedFloat64(byteOffset: Int, value: Double) { buffer.putDouble(offset + byteOffset, value) }
