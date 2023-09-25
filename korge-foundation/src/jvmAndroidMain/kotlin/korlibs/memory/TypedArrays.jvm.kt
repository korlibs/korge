@file:Suppress("PackageDirectoryMismatch")

package korlibs.memory.arrays

import korlibs.math.*
import java.nio.*

// Invariants: mark <= position <= limit <= capacity
actual class ArrayBuffer(val buffer: ByteBuffer) {
    init { buffer.order(ByteOrder.LITTLE_ENDIAN) }
    actual constructor(length: Int) : this(ByteBuffer.allocate(length))
    actual val byteLength: Int get() = buffer.limit() - buffer.position()
    actual fun slice(begin: Int, end: Int): ArrayBuffer {
        val slice = buffer.slice()
        slice.position(begin)
        slice.limit(end)
        return ArrayBuffer(slice)
    }
}

actual interface ArrayBufferView {
    actual val buffer: ArrayBuffer
    actual val byteOffset: Int
    actual val byteLength: Int
}

val ArrayBufferView.jbuffer: ByteBuffer get() = buffer.buffer

actual class Int8Array actual constructor(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    override val byteLength: Int get() = length * 1
    actual constructor(length: Int) : this(ArrayBuffer(length), 0, length)
}
actual class Int16Array actual constructor(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    override val byteLength: Int get() = length * 2
    actual constructor(length: Int) : this(ArrayBuffer(length * 2), 0, length)
}
actual class Int32Array actual constructor(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    override val byteLength: Int get() = length * 4
    actual constructor(length: Int) : this(ArrayBuffer(length * 4), 0, length)
}
actual class Float32Array actual constructor(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    override val byteLength: Int get() = length * 4
    actual constructor(length: Int) : this(ArrayBuffer(length * 4), 0, length)
}
actual class Float64Array actual constructor(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    override val byteLength: Int get() = length * 8
    actual constructor(length: Int) : this(ArrayBuffer(length * 8), 0, length)
}
actual class Uint8ClampedArray actual constructor(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    override val byteLength: Int get() = length * 1
    actual constructor(length: Int) : this(ArrayBuffer(length * 1), 0, length)
}
actual class Uint8Array actual constructor(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    override val byteLength: Int get() = length * 1
    actual constructor(length: Int) : this(ArrayBuffer(length * 1), 0, length)
}
actual class Uint16Array actual constructor(override val buffer: ArrayBuffer, override val byteOffset: Int, actual val length: Int) : ArrayBufferView {
    override val byteLength: Int get() = length * 2
    actual constructor(length: Int) : this(ArrayBuffer(length * 2), 0, length)
}
actual class DataView actual constructor(override val buffer: ArrayBuffer, override val byteOffset: Int, override val byteLength: Int) : ArrayBufferView {
    val bufferLE = jbuffer.slice().order(ByteOrder.LITTLE_ENDIAN)
    val bufferBE = jbuffer.slice().order(ByteOrder.BIG_ENDIAN)
    private fun jbuffer(littleEndian: Boolean): ByteBuffer = if (littleEndian) bufferLE else bufferBE

    actual fun getInt8(byteOffset: Int): Byte = bufferLE.get(byteOffset)
    actual fun getInt16(byteOffset: Int, littleEndian: Boolean): Short = jbuffer(littleEndian).getShort(byteOffset)
    actual fun getInt32(byteOffset: Int, littleEndian: Boolean): Int = jbuffer(littleEndian).getInt(byteOffset)
    actual fun getFloat32(byteOffset: Int, littleEndian: Boolean): Float = jbuffer(littleEndian).getFloat(byteOffset)
    actual fun getFloat64(byteOffset: Int, littleEndian: Boolean): Double = jbuffer(littleEndian).getDouble(byteOffset)
    actual fun setInt8(byteOffset: Int, value: Byte) { bufferLE.put(byteOffset, value) }
    actual fun setInt16(byteOffset: Int, value: Short, littleEndian: Boolean) { jbuffer(littleEndian).putShort(byteOffset, value) }
    actual fun setInt32(byteOffset: Int, value: Int, littleEndian: Boolean) { jbuffer(littleEndian).putInt(byteOffset, value) }
    actual fun setFloat32(byteOffset: Int, value: Float, littleEndian: Boolean) { jbuffer(littleEndian).putFloat(byteOffset, value) }
    actual fun setFloat64(byteOffset: Int, value: Double, littleEndian: Boolean) { jbuffer(littleEndian).putDouble(byteOffset, value) }
}

actual operator fun Int8Array.get(index: Int): Byte = jbuffer.get(index * 1)
actual operator fun Int16Array.get(index: Int): Short = jbuffer.getShort(index * 2)
actual operator fun Int32Array.get(index: Int): Int = jbuffer.getInt(index * 4)
actual operator fun Float32Array.get(index: Int): Float = jbuffer.getFloat(index * 4)
actual operator fun Float64Array.get(index: Int): Double = jbuffer.getDouble(index * 8)
actual operator fun Uint8ClampedArray.get(index: Int): Int = jbuffer.get(index * 1).toInt() and 0xFF
actual operator fun Uint8Array.get(index: Int): Int = jbuffer.get(index * 1).toInt() and 0xFF
actual operator fun Uint16Array.get(index: Int): Int = jbuffer.getShort(index * 2).toInt() and 0xFFFF

actual operator fun Int8Array.set(index: Int, value: Byte) { jbuffer.put(index * 1, value) }
actual operator fun Int16Array.set(index: Int, value: Short) { jbuffer.putShort(index * 2, value) }
actual operator fun Int32Array.set(index: Int, value: Int) { jbuffer.putInt(index * 4, value) }
actual operator fun Float32Array.set(index: Int, value: Float) { jbuffer.putFloat(index * 4, value) }
actual operator fun Float64Array.set(index: Int, value: Double) { jbuffer.putDouble(index * 8, value) }
actual operator fun Uint8ClampedArray.set(index: Int, value: Int) { jbuffer.put(index * 1, value.clamp(0, 255).toByte()) }
actual operator fun Uint8Array.set(index: Int, value: Int) { jbuffer.put(index * 1, value.toByte()) }
actual operator fun Uint16Array.set(index: Int, value: Int) { jbuffer.putShort(index * 2, value.toShort()) }
