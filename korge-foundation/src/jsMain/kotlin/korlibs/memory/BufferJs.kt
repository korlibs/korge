package korlibs.memory

import korlibs.memory.internal.*
import org.khronos.webgl.*

actual class Buffer(val dataView: DataView) {
    val buffer: ArrayBuffer get() = dataView.buffer

    fun sliceUint8Array(offset: Int = 0, size: Int = dataView.byteLength - offset): Uint8Array =
        Uint8Array(buffer, dataView.byteOffset + offset, size)
    override fun toString(): String = NBuffer_toString(this)
    actual companion object {
        actual fun copy(src: Buffer, srcPosBytes: Int, dst: Buffer, dstPosBytes: Int, sizeInBytes: Int) {
            dst.sliceUint8Array(dstPosBytes, sizeInBytes).set(src.sliceUint8Array(srcPosBytes, sizeInBytes), 0)
        }

        actual fun equals(src: Buffer, srcPosBytes: Int, dst: Buffer, dstPosBytes: Int, sizeInBytes: Int): Boolean = equalsCommon(src, srcPosBytes, dst, dstPosBytes, sizeInBytes, use64 = false)
    }


    // @TODO: Optimize by using words instead o bytes
    override fun hashCode(): Int {
        var h = 1
        for (n in 0 until size) h = 31 * h + dataView.getInt8(n)
        return h
    }

    // @TODO: Optimize by using words instead o bytes
    override fun equals(other: Any?): Boolean {
        if (other !is Buffer || this.size != other.size) return false
        val t = this.dataView
        val o = other.dataView
        for (n in 0 until size) if (t.getInt8(n) != o.getInt8(n)) return false
        return true
    }
}
actual fun Buffer(size: Int, direct: Boolean): Buffer {
    checkNBufferSize(size)
    return Buffer(DataView(ArrayBuffer(size)))
}
actual fun Buffer(array: ByteArray, offset: Int, size: Int): Buffer {
    checkNBufferWrap(array, offset, size)
    return Buffer(DataView(array.unsafeCast<Int8Array>().buffer, offset, size))
}
actual val Buffer.byteOffset: Int get() = this.dataView.byteOffset
actual val Buffer.sizeInBytes: Int get() = this.dataView.byteLength
actual fun Buffer.sliceInternal(start: Int, end: Int): Buffer = Buffer(DataView(this.buffer, this.byteOffset + start, end - start))

// Unaligned versions

actual fun Buffer.getUnalignedInt8(byteOffset: Int): Byte = dataView.getInt8(byteOffset)
actual fun Buffer.getUnalignedInt16(byteOffset: Int): Short = dataView.getInt16(byteOffset, currentIsLittleEndian)
actual fun Buffer.getUnalignedInt32(byteOffset: Int): Int = dataView.getInt32(byteOffset, currentIsLittleEndian)
actual fun Buffer.getUnalignedInt64(byteOffset: Int): Long {
    val v0 = getUnalignedInt32(byteOffset).toLong() and 0xFFFFFFFFL
    val v1 = getUnalignedInt32(byteOffset + 4).toLong() and 0xFFFFFFFFL
    return if (currentIsLittleEndian) (v1 shl 32) or v0 else (v0 shl 32) or v1
}
actual fun Buffer.getUnalignedFloat32(byteOffset: Int): Float = dataView.getFloat32(byteOffset, currentIsLittleEndian)
actual fun Buffer.getUnalignedFloat64(byteOffset: Int): Double = dataView.getFloat64(byteOffset, currentIsLittleEndian)

actual fun Buffer.setUnalignedInt8(byteOffset: Int, value: Byte) = dataView.setInt8(byteOffset, value)
actual fun Buffer.setUnalignedInt16(byteOffset: Int, value: Short) = dataView.setInt16(byteOffset, value, currentIsLittleEndian)
actual fun Buffer.setUnalignedInt32(byteOffset: Int, value: Int) = dataView.setInt32(byteOffset, value, currentIsLittleEndian)
actual fun Buffer.setUnalignedInt64(byteOffset: Int, value: Long) {
    setUnalignedInt32(byteOffset, if (currentIsLittleEndian) value.toInt() else (value ushr 32).toInt())
    setUnalignedInt32(byteOffset + 4, if (!currentIsLittleEndian) value.toInt() else (value ushr 32).toInt())
}
actual fun Buffer.setUnalignedFloat32(byteOffset: Int, value: Float) = dataView.setFloat32(byteOffset, value, currentIsLittleEndian)
actual fun Buffer.setUnalignedFloat64(byteOffset: Int, value: Double) = dataView.setFloat64(byteOffset, value, currentIsLittleEndian)

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

val Buffer.arrayUByte: Uint8Array get() = Uint8Array(this.buffer, byteOffset, sizeInBytes)
val Buffer.arrayByte: Int8Array get() = Int8Array(buffer, byteOffset, sizeInBytes)
val Buffer.arrayShort: Int16Array get() = Int16Array(buffer, byteOffset, sizeInBytes / 2)
val Buffer.arrayInt: Int32Array get() = Int32Array(buffer, byteOffset, sizeInBytes / 4)
val Buffer.arrayFloat: Float32Array get() = Float32Array(buffer, byteOffset, sizeInBytes / 4)
val Buffer.arrayDouble: Float64Array get() = Float64Array(buffer, byteOffset, sizeInBytes / 8)
