@file:Suppress("PackageDirectoryMismatch")

package korlibs.memory.arrays

import korlibs.math.*
import korlibs.memory.*

actual interface BufferDataSource

actual class ArrayBuffer(val data: ByteArray) : BufferDataSource {
    actual constructor(length: Int) : this(ByteArray(length))
    actual val byteLength: Int get() = data.size
}

actual interface ArrayBufferView : BufferDataSource {
    actual val buffer: ArrayBuffer
    actual val byteOffset: Int
    actual val byteLength: Int
}

val ArrayBufferView.data: ByteArray get() = buffer.data

actual fun ArrayBufferDirect(size: Int): ArrayBuffer = ArrayBuffer(size)
actual fun ArrayBufferWrap(data: ByteArray): ArrayBuffer = ArrayBuffer(data)
internal actual fun ArrayBuffer_copy(src: ArrayBuffer, srcPos: Int, dst: ArrayBuffer, dstPos: Int, length: Int) {
    arraycopy(src.data, srcPos, dst.data, dstPos, length)
}

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
    override val byteLength: Int get() = length * 1
    actual constructor(length: Int) : this(ArrayBuffer(length), 0, length)
}
actual class Int16Array(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    override val byteLength: Int get() = length * 2
    actual constructor(length: Int) : this(ArrayBuffer(length * 2), 0, length)
}
actual class Int32Array(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    override val byteLength: Int get() = length * 4
    actual constructor(length: Int) : this(ArrayBuffer(length * 4), 0, length)
}
actual class Float32Array(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    override val byteLength: Int get() = length * 4
    actual constructor(length: Int) : this(ArrayBuffer(length * 4), 0, length)
}
actual class Float64Array(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    override val byteLength: Int get() = length * 8
    actual constructor(length: Int) : this(ArrayBuffer(length * 8), 0, length)
}
actual class Uint8ClampedArray(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    override val byteLength: Int get() = length * 1
    actual constructor(length: Int) : this(ArrayBuffer(length * 1), 0, length)
}
actual class Uint8Array(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    override val byteLength: Int get() = length * 1
    actual constructor(length: Int) : this(ArrayBuffer(length * 1), 0, length)
}
actual class Uint16Array(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    override val byteLength: Int get() = length * 2
    actual constructor(length: Int) : this(ArrayBuffer(length * 2), 0, length)
}
actual class DataView(override val buffer: ArrayBuffer, override val byteOffset: Int, override val byteLength: Int) : ArrayBufferView {
}

actual fun DataView.getInt8(byteOffset: Int): Byte = data.getS8(byteOffset(byteOffset, 1)).toByte()
actual fun DataView.getInt16(byteOffset: Int, littleEndian: Boolean): Short = data.getS16(byteOffset(byteOffset, 2), littleEndian).toShort()
actual fun DataView.getInt32(byteOffset: Int, littleEndian: Boolean): Int = data.getS32(byteOffset(byteOffset, 4), littleEndian)
actual fun DataView.getFloat32(byteOffset: Int, littleEndian: Boolean): Float = data.getF32(byteOffset(byteOffset, 4), littleEndian)
actual fun DataView.getFloat64(byteOffset: Int, littleEndian: Boolean): Double = data.getF64(byteOffset(byteOffset, 8), littleEndian)
actual fun DataView.setInt8(byteOffset: Int, value: Byte) { data.set8(byteOffset(byteOffset, 1), value.toInt()) }
actual fun DataView.setInt16(byteOffset: Int, value: Short, littleEndian: Boolean) { data.set16(byteOffset(byteOffset, 2), value.toInt(), littleEndian) }
actual fun DataView.setInt32(byteOffset: Int, value: Int, littleEndian: Boolean) { data.set32(byteOffset(byteOffset, 4), value, littleEndian) }
actual fun DataView.setFloat32(byteOffset: Int, value: Float, littleEndian: Boolean) { data.setF32(byteOffset(byteOffset, 4), value, littleEndian) }
actual fun DataView.setFloat64(byteOffset: Int, value: Double, littleEndian: Boolean) { data.setF64(byteOffset(byteOffset, 8), value, littleEndian) }

actual operator fun Int8Array.get(index: Int): Byte = data[byteOffset(index)]
actual operator fun Int16Array.get(index: Int): Short = data.getS16LE(byteOffset(index)).toShort()
actual operator fun Int32Array.get(index: Int): Int = data.getS32LE(byteOffset(index))
actual operator fun Float32Array.get(index: Int): Float = data.getF32LE(byteOffset(index))
actual operator fun Float64Array.get(index: Int): Double = data.getF64LE(byteOffset(index))
actual operator fun Uint8ClampedArray.get(index: Int): Int = data[byteOffset(index)].toInt() and 0xFF
actual operator fun Uint8Array.get(index: Int): Int = data[byteOffset(index)].toInt() and 0xFF
actual operator fun Uint16Array.get(index: Int): Int = data.getS16LE(byteOffset(index)) and 0xFFFF

actual operator fun Int8Array.set(index: Int, value: Byte) { data[byteOffset(index)] = value }
actual operator fun Int16Array.set(index: Int, value: Short) { data.set16LE(byteOffset(index), value.toInt()) }
actual operator fun Int32Array.set(index: Int, value: Int) { data.set32LE(byteOffset(index), value) }
actual operator fun Float32Array.set(index: Int, value: Float) { data.setF32LE(byteOffset(index), value) }
actual operator fun Float64Array.set(index: Int, value: Double) { data.setF64LE(byteOffset(index), value) }
actual operator fun Uint8ClampedArray.set(index: Int, value: Int) { data.set(byteOffset(index), value.clamp(0, 255).toByte()) }
actual operator fun Uint8Array.set(index: Int, value: Int) { data.set(byteOffset(index), value.clamp(0, 255).toByte()) }
actual operator fun Uint16Array.set(index: Int, value: Int) { data.set16LE(byteOffset(index), value) }
