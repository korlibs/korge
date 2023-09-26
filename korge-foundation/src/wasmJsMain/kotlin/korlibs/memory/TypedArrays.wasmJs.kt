@file:Suppress("PackageDirectoryMismatch")

package korlibs.memory.arrays

import korlibs.math.*
import org.khronos.webgl.get as getNative
import org.khronos.webgl.set as setNative

actual interface BufferDataSource

actual class ArrayBuffer(val jsBuffer: org.khronos.webgl.ArrayBuffer) : BufferDataSource {
    actual constructor(length: Int) : this(org.khronos.webgl.ArrayBuffer(length))
    actual val byteLength: Int get() = jsBuffer.byteLength
}
//fun ArrayBuffer.subArray(begin: Int, end: Int = byteLength): ArrayBuffer {
//    TODO()
//}

actual interface ArrayBufferView : BufferDataSource {
    actual val buffer: ArrayBuffer
    actual val byteOffset: Int
    actual val byteLength: Int
}

actual fun ArrayBufferDirect(size: Int): ArrayBuffer = ArrayBuffer(size)
actual fun ArrayBufferWrap(data: ByteArray): ArrayBuffer {
    //val int8Array = data.unsafeCast<Int8Array>()
    val int8Array = data.toInt8Array()
    check(int8Array.byteOffset == 0)
    return ArrayBuffer(int8Array.buffer.jsBuffer)
}
internal actual fun ArrayBuffer_copy(src: ArrayBuffer, srcPos: Int, dst: ArrayBuffer, dstPos: Int, length: Int) {
    Int8Array(dst, dstPos, length).set(Int8Array(src, srcPos, length), 0)
}
internal actual fun ArrayBuffer_equals(src: ArrayBuffer, srcPos: Int, dst: ArrayBuffer, dstPos: Int, length: Int): Boolean =
    ArrayBuffer_equals_common(src, srcPos, dst, dstPos, length)

actual inline fun ArrayBuffer.uint8ClampedArray(byteOffset: Int, length: Int): Uint8ClampedArray = Uint8ClampedArray(this, byteOffset, length)
actual inline fun ArrayBuffer.uint8Array(byteOffset: Int, length: Int): Uint8Array = Uint8Array(this, byteOffset, length)
actual inline fun ArrayBuffer.uint16Array(byteOffset: Int, length: Int): Uint16Array = Uint16Array(this, byteOffset, length)
actual inline fun ArrayBuffer.int8Array(byteOffset: Int, length: Int): Int8Array = Int8Array(this, byteOffset, length)
actual inline fun ArrayBuffer.int16Array(byteOffset: Int, length: Int): Int16Array = Int16Array(this, byteOffset, length)
actual inline fun ArrayBuffer.int32Array(byteOffset: Int, length: Int): Int32Array = Int32Array(this, byteOffset, length)
actual inline fun ArrayBuffer.float32Array(byteOffset: Int, length: Int): Float32Array = Float32Array(this, byteOffset, length)
actual inline fun ArrayBuffer.float64Array(byteOffset: Int, length: Int): Float64Array = Float64Array(this, byteOffset, length)
actual inline fun ArrayBuffer.dataView(byteOffset: Int, length: Int): DataView = DataView(this, byteOffset, length)

actual class Int8Array(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    val data = org.khronos.webgl.Int8Array(buffer.jsBuffer, byteOffset, length)
    override val byteLength: Int get() = length * 1
    actual constructor(length: Int) : this(ArrayBuffer(length), 0, length)
}
actual class Int16Array(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    val data = org.khronos.webgl.Int16Array(buffer.jsBuffer, byteOffset, length)
    override val byteLength: Int get() = length * 2
    actual constructor(length: Int) : this(ArrayBuffer(length * 2), 0, length)
}
actual class Int32Array(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    val data = org.khronos.webgl.Int32Array(buffer.jsBuffer, byteOffset, length)
    override val byteLength: Int get() = length * 4
    actual constructor(length: Int) : this(ArrayBuffer(length * 4), 0, length)
}
actual class Float32Array(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    val data = org.khronos.webgl.Float32Array(buffer.jsBuffer, byteOffset, length)
    override val byteLength: Int get() = length * 4
    actual constructor(length: Int) : this(ArrayBuffer(length * 4), 0, length)
}
actual class Float64Array(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    val data = org.khronos.webgl.Float64Array(buffer.jsBuffer, byteOffset, length)
    override val byteLength: Int get() = length * 8
    actual constructor(length: Int) : this(ArrayBuffer(length * 8), 0, length)
}
actual class Uint8ClampedArray(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    val data = org.khronos.webgl.Uint8ClampedArray(buffer.jsBuffer, byteOffset, length)
    override val byteLength: Int get() = length * 1
    actual constructor(length: Int) : this(ArrayBuffer(length * 1), 0, length)
}
actual class Uint8Array(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    val data = org.khronos.webgl.Uint8Array(buffer.jsBuffer, byteOffset, length)
    override val byteLength: Int get() = length * 1
    actual constructor(length: Int) : this(ArrayBuffer(length * 1), 0, length)
}
actual class Uint16Array(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    val data = org.khronos.webgl.Uint16Array(buffer.jsBuffer, byteOffset, length)
    override val byteLength: Int get() = length * 2
    actual constructor(length: Int) : this(ArrayBuffer(length * 2), 0, length)
}
actual class DataView(override val buffer: ArrayBuffer, override val byteOffset: Int, override val byteLength: Int) : ArrayBufferView {
    val data = org.khronos.webgl.DataView(buffer.jsBuffer, byteOffset, byteLength)
    val jbuffer = buffer.jsBuffer
}

actual inline fun DataView.getS8(byteOffset: Int): Byte = (this.data.getInt8(byteOffset))
actual inline fun DataView.getS16(byteOffset: Int, littleEndian: Boolean): Short = (this.data.getInt16(byteOffset, littleEndian))
actual inline fun DataView.getS32(byteOffset: Int, littleEndian: Boolean): Int = (this.data.getInt32(byteOffset, littleEndian))
actual inline fun DataView.getF32(byteOffset: Int, littleEndian: Boolean): Float = (this.data.getFloat32(byteOffset, littleEndian))
actual inline fun DataView.getF64(byteOffset: Int, littleEndian: Boolean): Double = (this.data.getFloat64(byteOffset, littleEndian))
actual inline fun DataView.setS8(byteOffset: Int, value: Byte) { (this.data.setInt8(byteOffset, value)) }
actual inline fun DataView.setS16(byteOffset: Int, value: Short, littleEndian: Boolean) { (this.data.setInt16(byteOffset, value, littleEndian)) }
actual inline fun DataView.setS32(byteOffset: Int, value: Int, littleEndian: Boolean) { (this.data.setInt32(byteOffset, value, littleEndian)) }
actual inline fun DataView.getF32(byteOffset: Int, value: Float, littleEndian: Boolean) { (this.data.setFloat32(byteOffset, value, littleEndian)) }
actual inline fun DataView.setF64(byteOffset: Int, value: Double, littleEndian: Boolean) { (this.data.setFloat64(byteOffset, value, littleEndian)) }

actual inline operator fun Int8Array.get(index: Int): Byte = this.data.getNative(index)
actual inline operator fun Int16Array.get(index: Int): Short = this.data.getNative(index)
actual inline operator fun Int32Array.get(index: Int): Int = this.data.getNative(index)
actual inline operator fun Float32Array.get(index: Int): Float = this.data.getNative(index)
actual inline operator fun Float64Array.get(index: Int): Double = this.data.getNative(index)
actual inline operator fun Uint8ClampedArray.get(index: Int): Int = this.data.getNative(index).unsigned
actual inline operator fun Uint8Array.get(index: Int): Int = this.data.getNative(index).unsigned
actual inline operator fun Uint16Array.get(index: Int): Int = this.data.getNative(index).unsigned

actual inline operator fun Int8Array.set(index: Int, value: Byte) { this.data.setNative(index, value) }
actual inline operator fun Int16Array.set(index: Int, value: Short) { this.data.setNative(index, value) }
actual inline operator fun Int32Array.set(index: Int, value: Int) { this.data.setNative(index, value) }
actual inline operator fun Float32Array.set(index: Int, value: Float) { this.data.setNative(index, value) }
actual inline operator fun Float64Array.set(index: Int, value: Double) { this.data.setNative(index, value) }
actual inline operator fun Uint8ClampedArray.set(index: Int, value: Int) { this.data.setNative(index, value.toByte()) }
actual inline operator fun Uint8Array.set(index: Int, value: Int) { this.data.setNative(index, value.toByte()) }
actual inline operator fun Uint16Array.set(index: Int, value: Int) { this.data.setNative(index, value.toShort()) }
