@file:Suppress("PackageDirectoryMismatch")

package korlibs.memory.arrays

import korlibs.math.*
import korlibs.memory.*


actual interface BufferDataSource

actual class ArrayBuffer(val data: ByteArray, val byteOffset: Int, actual val byteLength: Int) : BufferDataSource {
    actual constructor(length: Int) : this(ByteArray(length), 0, length)
    fun index(index: Int, size: Int = 1): Int = byteOffset + index
    fun indexS(index: Int, size: Int): Int = byteOffset + index * size
}

actual interface ArrayBufferView : BufferDataSource {
    actual val buffer: ArrayBuffer
    actual val byteOffset: Int
    actual val byteLength: Int
}

val ArrayBufferView.data: ByteArray get() = buffer.data
fun ArrayBufferView.index(index: Int, size: Int = 1): Int = buffer.index(index, size)
fun ArrayBufferView.indexS(index: Int, size: Int): Int = buffer.indexS(index, size)

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

actual fun DataView.getInt8(byteOffset: Int): Byte = data.getS8(index(byteOffset, 1)).toByte()
actual fun DataView.getInt16(byteOffset: Int, littleEndian: Boolean): Short = data.getS16(index(byteOffset, 2), littleEndian).toShort()
actual fun DataView.getInt32(byteOffset: Int, littleEndian: Boolean): Int = data.getS32(index(byteOffset, 4), littleEndian)
actual fun DataView.getFloat32(byteOffset: Int, littleEndian: Boolean): Float = data.getF32(index(byteOffset, 4), littleEndian)
actual fun DataView.getFloat64(byteOffset: Int, littleEndian: Boolean): Double = data.getF64(index(byteOffset, 8), littleEndian)
actual fun DataView.setInt8(byteOffset: Int, value: Byte) { data.set8(index(byteOffset, 1), value.toInt()) }
actual fun DataView.setInt16(byteOffset: Int, value: Short, littleEndian: Boolean) { data.set16(index(byteOffset, 2), value.toInt(), littleEndian) }
actual fun DataView.setInt32(byteOffset: Int, value: Int, littleEndian: Boolean) { data.set32(index(byteOffset, 4), value, littleEndian) }
actual fun DataView.setFloat32(byteOffset: Int, value: Float, littleEndian: Boolean) { data.setF32(index(byteOffset, 4), value, littleEndian) }
actual fun DataView.setFloat64(byteOffset: Int, value: Double, littleEndian: Boolean) { data.setF64(index(byteOffset, 8), value, littleEndian) }

actual operator fun Int8Array.get(index: Int): Byte = data[indexS(index, 1)]
actual operator fun Int16Array.get(index: Int): Short = data.getS16LE(indexS(index, 2)).toShort()
actual operator fun Int32Array.get(index: Int): Int = data.getS32LE(indexS(index, 4))
actual operator fun Float32Array.get(index: Int): Float = data.getF32LE(indexS(index, 4))
actual operator fun Float64Array.get(index: Int): Double = data.getF64LE(indexS(index, 8))
actual operator fun Uint8ClampedArray.get(index: Int): Int = data[indexS(index, 1)].toInt() and 0xFF
actual operator fun Uint8Array.get(index: Int): Int = data[indexS(index, 1)].toInt() and 0xFF
actual operator fun Uint16Array.get(index: Int): Int = data.getS16LE(indexS(index, 2)) and 0xFFFF

actual operator fun Int8Array.set(index: Int, value: Byte) { data[indexS(index, 1)] = value }
actual operator fun Int16Array.set(index: Int, value: Short) { data.set16LE(indexS(index, 2), value.toInt()) }
actual operator fun Int32Array.set(index: Int, value: Int) { data.set32LE(indexS(index, 4), value) }
actual operator fun Float32Array.set(index: Int, value: Float) { data.setF32LE(indexS(index, 4), value) }
actual operator fun Float64Array.set(index: Int, value: Double) { data.setF64LE(indexS(index, 8), value) }
actual operator fun Uint8ClampedArray.set(index: Int, value: Int) { data.set(indexS(index, 1), value.clamp(0, 255).toByte()) }
actual operator fun Uint8Array.set(index: Int, value: Int) { data.set(indexS(index, 1), value.clamp(0, 255).toByte()) }
actual operator fun Uint16Array.set(index: Int, value: Int) { data.set16LE(indexS(index, 2), value) }
