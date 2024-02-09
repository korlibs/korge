package korlibs.memory

import korlibs.memory.wasm.*
import korlibs.platform.*
import korlibs.platform.Platform.Companion.isLittleEndian
import org.khronos.webgl.*

actual class Buffer(val dataView: org.khronos.webgl.DataView) {
    actual constructor(size: Int, direct: Boolean) : this(org.khronos.webgl.DataView(ArrayBuffer(checkNBufferSize(size))))
    actual constructor(array: ByteArray, offset: Int, size: Int) : this(
        //DataView(checkNBufferWrap(array, offset, size).unsafeCast<Int8Array>().buffer, offset, size)
        // @TODO: Can't wrap, so we perform a copy
        org.khronos.webgl.DataView(checkNBufferWrap(array, offset, size).toInt8Array().buffer, offset, size)
    )

    actual val byteOffset: Int get() = this.dataView.byteOffset
    actual val sizeInBytes: Int get() = this.dataView.byteLength

    actual fun sliceInternal(start: Int, end: Int): Buffer =
        Buffer(org.khronos.webgl.DataView(buffer, byteOffset + start, end - start))

    fun sliceUint8Array(offset: Int = 0, size: Int = dataView.byteLength - offset): Uint8Array =
        Uint8Array(buffer, dataView.byteOffset + offset, size)

    actual fun transferBytes(bufferOffset: Int, array: ByteArray, arrayOffset: Int, len: Int, toArray: Boolean) {
        if (toArray) {
            for (n in 0 until len) array[arrayOffset + n] = this.getS8(bufferOffset + n)
        } else {
            for (n in 0 until len) this.set8(bufferOffset + n, array[arrayOffset + n])
        }
    }

    actual fun getS8(byteOffset: Int): Byte = dataView.getInt8(byteOffset)
    actual fun getS16LE(byteOffset: Int): Short = dataView.getInt16(byteOffset, true)
    actual fun getS32LE(byteOffset: Int): Int = dataView.getInt32(byteOffset, true)
    actual fun getS64LE(byteOffset: Int): Long {
        val v0 = getS32LE(byteOffset).toLong() and 0xFFFFFFFFL
        val v1 = getS32LE(byteOffset + 4).toLong() and 0xFFFFFFFFL
        return if (isLittleEndian) (v1 shl 32) or v0 else (v0 shl 32) or v1
    }
    actual fun getF32LE(byteOffset: Int): Float = dataView.getFloat32(byteOffset, true)
    actual fun getF64LE(byteOffset: Int): Double = dataView.getFloat64(byteOffset, true)
    actual fun getS16BE(byteOffset: Int): Short = dataView.getInt16(byteOffset, false)
    actual fun getS32BE(byteOffset: Int): Int = dataView.getInt32(byteOffset, false)
    actual fun getS64BE(byteOffset: Int): Long {
        val v0 = getS32BE(byteOffset).toLong() and 0xFFFFFFFFL
        val v1 = getS32BE(byteOffset + 4).toLong() and 0xFFFFFFFFL
        return (v0 shl 32) or v1
    }
    actual fun getF32BE(byteOffset: Int): Float = dataView.getFloat32(byteOffset, false)
    actual fun getF64BE(byteOffset: Int): Double = dataView.getFloat64(byteOffset, false)

    actual fun set8(byteOffset: Int, value: Byte) = dataView.setInt8(byteOffset, value)
    actual fun set16LE(byteOffset: Int, value: Short) = dataView.setInt16(byteOffset, value, true)
    actual fun set32LE(byteOffset: Int, value: Int) = dataView.setInt32(byteOffset, value, true)
    actual fun set64LE(byteOffset: Int, value: Long) {
        set32LE(byteOffset, value.toInt())
        set32LE(byteOffset + 4, (value ushr 32).toInt())
    }
    actual fun setF32LE(byteOffset: Int, value: Float) = dataView.setFloat32(byteOffset, value, true)
    actual fun setF64LE(byteOffset: Int, value: Double) = dataView.setFloat64(byteOffset, value, true)

    actual fun set16BE(byteOffset: Int, value: Short) = dataView.setInt16(byteOffset, value, false)
    actual fun set32BE(byteOffset: Int, value: Int) = dataView.setInt32(byteOffset, value, false)
    actual fun set64BE(byteOffset: Int, value: Long) {
        set32BE(byteOffset, (value ushr 32).toInt())
        set32BE(byteOffset + 4, value.toInt())
    }
    actual fun setF32BE(byteOffset: Int, value: Float) = dataView.setFloat32(byteOffset, value, false)
    actual fun setF64BE(byteOffset: Int, value: Double) = dataView.setFloat64(byteOffset, value, false)

    override fun hashCode(): Int = hashCodeCommon(this)
    override fun equals(other: Any?): Boolean = equalsCommon(this, other)
    override fun toString(): String = NBuffer_toString(this)

    actual companion object {
        actual fun copy(src: Buffer, srcPosBytes: Int, dst: Buffer, dstPosBytes: Int, sizeInBytes: Int) {
            dst.sliceUint8Array(dstPosBytes, sizeInBytes).set(src.sliceUint8Array(srcPosBytes, sizeInBytes), 0)
        }

        actual fun equals(src: Buffer, srcPosBytes: Int, dst: Buffer, dstPosBytes: Int, sizeInBytes: Int): Boolean = equalsCommon(src, srcPosBytes, dst, dstPosBytes, sizeInBytes, use64 = false)
    }
}

val Buffer.buffer: ArrayBuffer get() = dataView.buffer


fun ArrayBuffer.asUint8ClampedArray(): Uint8ClampedArray = Uint8ClampedArray(this)
fun ArrayBuffer.asUint8Array(): Uint8Array = Uint8Array(this)
fun ArrayBuffer.asInt8Array(): Int8Array = Int8Array(this)
fun ArrayBuffer.asInt16Array(): Int16Array = Int16Array(this)
fun ArrayBuffer.asInt32Array(): Int32Array = Int32Array(this)
fun ArrayBuffer.asFloat32Array(): Float32Array = Float32Array(this)
fun ArrayBuffer.asFloat64Array(): Float64Array = Float64Array(this)

fun ArrayBuffer.toUByteArray(): UByteArray = asUint8Array().toByteArray().asUByteArray()
fun ArrayBuffer.toByteArray(): ByteArray = asInt8Array().toByteArray()
fun ArrayBuffer.toShortArray(): ShortArray = asInt16Array().toShortArray()
fun ArrayBuffer.toIntArray(): IntArray = asInt32Array().toIntArray()
fun ArrayBuffer.toFloatArray(): FloatArray = asFloat32Array().toFloatArray()
fun ArrayBuffer.toDoubleArray(): DoubleArray = asFloat64Array().toDoubleArray()

val Buffer.arrayUByte: Uint8Array get() = Uint8Array(this.buffer, byteOffset, sizeInBytes)
val Buffer.arrayByte: Int8Array get() = Int8Array(buffer, byteOffset, sizeInBytes)
val Buffer.arrayShort: Int16Array get() = Int16Array(buffer, byteOffset, sizeInBytes / 2)
val Buffer.arrayInt: Int32Array get() = Int32Array(buffer, byteOffset, sizeInBytes / 4)
val Buffer.arrayFloat: Float32Array get() = Float32Array(buffer, byteOffset, sizeInBytes / 4)
val Buffer.arrayDouble: Float64Array get() = Float64Array(buffer, byteOffset, sizeInBytes / 8)
