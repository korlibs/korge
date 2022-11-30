package com.soywiz.kmem

import com.soywiz.kmem.internal.*
import org.khronos.webgl.*

actual class NBuffer(val dataView: DataView) {
    val buffer: ArrayBuffer get() = dataView.buffer

    fun sliceUint8Array(offset: Int = 0, size: Int = dataView.byteLength - offset): Uint8Array =
        Uint8Array(buffer, dataView.byteOffset + offset, size)
    override fun toString(): String = NBuffer_toString(this)
    actual companion object {}
}
actual fun NBuffer(size: Int, direct: Boolean): NBuffer {
    checkNBufferSize(size)
    return NBuffer(DataView(ArrayBuffer(size)))
}
actual fun NBuffer(array: ByteArray, offset: Int, size: Int): NBuffer {
    checkNBufferWrap(array, offset, size)
    return NBuffer(DataView(array.unsafeCast<Int8Array>().buffer, offset, size))
}
actual val NBuffer.byteOffset: Int get() = this.dataView.byteOffset
actual val NBuffer.sizeInBytes: Int get() = this.dataView.byteLength
actual fun NBuffer.sliceInternal(start: Int, end: Int): NBuffer = NBuffer(DataView(this.buffer, this.byteOffset + start, end - start))

actual fun NBuffer.Companion.copy(src: NBuffer, srcPosBytes: Int, dst: NBuffer, dstPosBytes: Int, sizeInBytes: Int) {
    dst.sliceUint8Array(dstPosBytes, sizeInBytes).set(src.sliceUint8Array(srcPosBytes, sizeInBytes), 0)
}

// Unaligned versions

actual fun NBuffer.getUnalignedInt8(byteOffset: Int): Byte = dataView.getInt8(byteOffset)
actual fun NBuffer.getUnalignedInt16(byteOffset: Int): Short = dataView.getInt16(byteOffset, currentIsLittleEndian)
actual fun NBuffer.getUnalignedInt32(byteOffset: Int): Int = dataView.getInt32(byteOffset, currentIsLittleEndian)
actual fun NBuffer.getUnalignedInt64(byteOffset: Int): Long {
    val v0 = getUnalignedInt32(byteOffset).toLong() and 0xFFFFFFFFL
    val v1 = getUnalignedInt32(byteOffset + 4).toLong() and 0xFFFFFFFFL
    return if (currentIsLittleEndian) (v1 shl 32) or v0 else (v0 shl 32) or v1
}
actual fun NBuffer.getUnalignedFloat32(byteOffset: Int): Float = dataView.getFloat32(byteOffset, currentIsLittleEndian)
actual fun NBuffer.getUnalignedFloat64(byteOffset: Int): Double = dataView.getFloat64(byteOffset, currentIsLittleEndian)

actual fun NBuffer.setUnalignedInt8(byteOffset: Int, value: Byte) = dataView.setInt8(byteOffset, value)
actual fun NBuffer.setUnalignedInt16(byteOffset: Int, value: Short) = dataView.setInt16(byteOffset, value, currentIsLittleEndian)
actual fun NBuffer.setUnalignedInt32(byteOffset: Int, value: Int) = dataView.setInt32(byteOffset, value, currentIsLittleEndian)
actual fun NBuffer.setUnalignedInt64(byteOffset: Int, value: Long) {
    setUnalignedInt32(byteOffset, if (currentIsLittleEndian) value.toInt() else (value ushr 32).toInt())
    setUnalignedInt32(byteOffset + 4, if (!currentIsLittleEndian) value.toInt() else (value ushr 32).toInt())
}
actual fun NBuffer.setUnalignedFloat32(byteOffset: Int, value: Float) = dataView.setFloat32(byteOffset, value, currentIsLittleEndian)
actual fun NBuffer.setUnalignedFloat64(byteOffset: Int, value: Double) = dataView.setFloat64(byteOffset, value, currentIsLittleEndian)

fun ArrayBuffer.asUint8ClampedArray(): Uint8ClampedArray = Uint8ClampedArray(this)
fun ArrayBuffer.asUint8Array(): Uint8Array = Uint8Array(this)
fun ArrayBuffer.asInt8Array(): Int8Array = Int8Array(this)
fun ArrayBuffer.asInt16Array(): Int16Array = Int16Array(this)
fun ArrayBuffer.asInt32Array(): Int32Array = Int32Array(this)
fun ArrayBuffer.asFloat32Array(): Float32Array = Float32Array(this)
fun ArrayBuffer.asFloat64Array(): Float64Array = Float64Array(this)

fun ArrayBuffer.asUByteArray(): UByteArray = asUint8Array().unsafeCast<ByteArray>().asUByteArray()
fun ArrayBuffer.asByteArray(): ByteArray = asInt8Array().unsafeCast<ByteArray>()
fun ArrayBuffer.asShortArray(): ShortArray = asInt16Array().unsafeCast<ShortArray>()
fun ArrayBuffer.asIntArray(): IntArray = asInt32Array().unsafeCast<IntArray>()
fun ArrayBuffer.asFloatArray(): FloatArray = asFloat32Array().unsafeCast<FloatArray>()
fun ArrayBuffer.asDoubleArray(): DoubleArray = asFloat64Array().unsafeCast<DoubleArray>()

val NBuffer.arrayByte: Int8Array get() = Int8Array(buffer)
val NBuffer.arrayShort: Int16Array get() = Int16Array(buffer)
val NBuffer.arrayInt: Int32Array get() = Int32Array(buffer)
val NBuffer.arrayFloat: Float32Array get() = Float32Array(buffer)
val NBuffer.arrayDouble: Float64Array get() = Float64Array(buffer)
val NBuffer.arrayBuffer: ArrayBuffer get() = this.mem.buffer
val NBuffer.arrayUByte: Uint8Array get() = Uint8Array(this.mem.buffer)
