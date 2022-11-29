package com.soywiz.kmem

import org.khronos.webgl.*

actual class NBuffer(val dataView: DataView) {
    val buffer: ArrayBuffer get() = dataView.buffer
}
actual fun NBuffer(size: Int, direct: Boolean): NBuffer = NBuffer(DataView(ArrayBuffer(size)))
actual fun NBuffer(array: ByteArray, offset: Int, size: Int): NBuffer = NBuffer(DataView(array.toMemBuffer().unsafeCast<Int8Array>().buffer, offset, size))
actual val NBuffer.byteOffset: Int get() = this.dataView.byteOffset
actual val NBuffer.sizeInBytes: Int get() = this.dataView.byteLength
actual fun NBuffer.sliceInternal(start: Int, end: Int): NBuffer = NBuffer(DataView(this.buffer, this.byteOffset + start, end - start))

// Unaligned versions

actual fun NBuffer.getUnalignedInt8(byteOffset: Int): Byte = dataView.getInt8(byteOffset)
actual fun NBuffer.getUnalignedInt16(byteOffset: Int): Short = dataView.getInt16(byteOffset)
actual fun NBuffer.getUnalignedInt32(byteOffset: Int): Int = dataView.getInt32(byteOffset)
actual fun NBuffer.getUnalignedInt64(byteOffset: Int): Long = dataView.getFloat64(byteOffset).reinterpretAsLong()
actual fun NBuffer.getUnalignedFloat32(byteOffset: Int): Float = dataView.getFloat32(byteOffset)
actual fun NBuffer.getUnalignedFloat64(byteOffset: Int): Double = dataView.getFloat64(byteOffset)

actual fun NBuffer.setUnalignedInt8(byteOffset: Int, value: Byte) = dataView.setInt8(byteOffset, value)
actual fun NBuffer.setUnalignedInt16(byteOffset: Int, value: Short) = dataView.setInt16(byteOffset, value)
actual fun NBuffer.setUnalignedInt32(byteOffset: Int, value: Int) = dataView.setInt32(byteOffset, value)
actual fun NBuffer.setUnalignedInt64(byteOffset: Int, value: Long) = dataView.setFloat64(byteOffset, value.reinterpretAsDouble())
actual fun NBuffer.setUnalignedFloat32(byteOffset: Int, value: Float) = dataView.setFloat32(byteOffset, value)
actual fun NBuffer.setUnalignedFloat64(byteOffset: Int, value: Double) = dataView.setFloat64(byteOffset, value)
