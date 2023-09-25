@file:Suppress("PackageDirectoryMismatch", "NOTHING_TO_INLINE")

package korlibs.memory.arrays

import korlibs.math.*
import java.nio.*

actual interface BufferDataSource

// Invariants: mark <= position <= limit <= capacity
actual class ArrayBuffer(val buffer: ByteBuffer) : BufferDataSource {
    init { buffer.order(ByteOrder.LITTLE_ENDIAN) }
    actual constructor(length: Int) : this(ByteBuffer.allocateDirect(length))
    actual val byteLength: Int get() = buffer.limit() - buffer.position()
}

actual interface ArrayBufferView : BufferDataSource {
    actual val buffer: ArrayBuffer
    actual val byteOffset: Int
    actual val byteLength: Int
}

val ArrayBufferView.jbuffer: ByteBuffer get() = buffer.buffer

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
    fun offset(index: Int): Int = byteOffset + index * 1
    actual constructor(length: Int) : this(ArrayBuffer(length), 0, length)
}
actual class Int16Array(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    override val byteLength: Int get() = length * 2
    fun offset(index: Int): Int = byteOffset + index * 2
    actual constructor(length: Int) : this(ArrayBuffer(length * 2), 0, length)
}
actual class Int32Array(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    override val byteLength: Int get() = length * 4
    fun offset(index: Int): Int = byteOffset + index * 4
    actual constructor(length: Int) : this(ArrayBuffer(length * 4), 0, length)
}
actual class Float32Array(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    override val byteLength: Int get() = length * 4
    fun offset(index: Int): Int = byteOffset + index * 4
    actual constructor(length: Int) : this(ArrayBuffer(length * 4), 0, length)
}
actual class Float64Array(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    override val byteLength: Int get() = length * 8
    fun offset(index: Int): Int = byteOffset + index * 8
    actual constructor(length: Int) : this(ArrayBuffer(length * 8), 0, length)
}
actual class Uint8ClampedArray(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    override val byteLength: Int get() = length * 1
    fun offset(index: Int): Int = byteOffset + index * 1
    actual constructor(length: Int) : this(ArrayBuffer(length * 1), 0, length)
}
actual class Uint8Array(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    override val byteLength: Int get() = length * 1
    fun offset(index: Int): Int = byteOffset + index * 1
    actual constructor(length: Int) : this(ArrayBuffer(length * 1), 0, length)
}
actual class Uint16Array(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    override val byteLength: Int get() = length * 2
    fun offset(index: Int): Int = byteOffset + index * 2
    actual constructor(length: Int) : this(ArrayBuffer(length * 2), 0, length)
}
actual class DataView(override val buffer: ArrayBuffer, override val byteOffset: Int, override val byteLength: Int) : ArrayBufferView {
    val bufferLE = jbuffer.slice().order(ByteOrder.LITTLE_ENDIAN)
    val bufferBE = jbuffer.slice().order(ByteOrder.BIG_ENDIAN)
    fun jbuffer(littleEndian: Boolean): ByteBuffer = if (littleEndian) bufferLE else bufferBE
    fun offset(offset: Int, size: Int): Int {
        if (offset < 0 || offset + size > byteLength) {
            error("Offset is outside the bounds of the DataView")
        }
        return byteOffset + offset
    }
}

actual inline fun DataView.getInt8(byteOffset: Int): Byte = bufferLE.get(offset(byteOffset, 1))
actual inline fun DataView.getInt16(byteOffset: Int, littleEndian: Boolean): Short = jbuffer(littleEndian).getShort(offset(byteOffset, 2))
actual inline fun DataView.getInt32(byteOffset: Int, littleEndian: Boolean): Int = jbuffer(littleEndian).getInt(offset(byteOffset, 4))
actual inline fun DataView.getFloat32(byteOffset: Int, littleEndian: Boolean): Float = jbuffer(littleEndian).getFloat(offset(byteOffset, 4))
actual inline fun DataView.getFloat64(byteOffset: Int, littleEndian: Boolean): Double = jbuffer(littleEndian).getDouble(offset(byteOffset, 8))
actual inline fun DataView.setInt8(byteOffset: Int, value: Byte) { bufferLE.put(offset(byteOffset, 1), value) }
actual inline fun DataView.setInt16(byteOffset: Int, value: Short, littleEndian: Boolean) { jbuffer(littleEndian).putShort(offset(byteOffset, 2), value) }
actual inline fun DataView.setInt32(byteOffset: Int, value: Int, littleEndian: Boolean) { jbuffer(littleEndian).putInt(offset(byteOffset, 4), value) }
actual inline fun DataView.setFloat32(byteOffset: Int, value: Float, littleEndian: Boolean) { jbuffer(littleEndian).putFloat(offset(byteOffset, 4), value) }
actual inline fun DataView.setFloat64(byteOffset: Int, value: Double, littleEndian: Boolean) { jbuffer(littleEndian).putDouble(offset(byteOffset, 8), value) }

actual operator fun Int8Array.get(index: Int): Byte = jbuffer.get(offset(index))
actual operator fun Int16Array.get(index: Int): Short = jbuffer.getShort(offset(index))
actual operator fun Int32Array.get(index: Int): Int = jbuffer.getInt(offset(index))
actual operator fun Float32Array.get(index: Int): Float = jbuffer.getFloat(offset(index))
actual operator fun Float64Array.get(index: Int): Double = jbuffer.getDouble(offset(index))
actual operator fun Uint8ClampedArray.get(index: Int): Int = jbuffer.get(offset(index)).toInt() and 0xFF
actual operator fun Uint8Array.get(index: Int): Int = jbuffer.get(offset(index)).toInt() and 0xFF
actual operator fun Uint16Array.get(index: Int): Int = jbuffer.getShort(offset(index)).toInt() and 0xFFFF

actual operator fun Int8Array.set(index: Int, value: Byte) { jbuffer.put(offset(index), value) }
actual operator fun Int16Array.set(index: Int, value: Short) { jbuffer.putShort(offset(index), value) }
actual operator fun Int32Array.set(index: Int, value: Int) { jbuffer.putInt(offset(index), value) }
actual operator fun Float32Array.set(index: Int, value: Float) { jbuffer.putFloat(offset(index), value) }
actual operator fun Float64Array.set(index: Int, value: Double) { jbuffer.putDouble(offset(index), value) }
actual operator fun Uint8ClampedArray.set(index: Int, value: Int) { jbuffer.put(offset(index), value.clamp(0, 255).toByte()) }
actual operator fun Uint8Array.set(index: Int, value: Int) { jbuffer.put(offset(index), value.toByte()) }
actual operator fun Uint16Array.set(index: Int, value: Int) { jbuffer.putShort(offset(index), value.toShort()) }
