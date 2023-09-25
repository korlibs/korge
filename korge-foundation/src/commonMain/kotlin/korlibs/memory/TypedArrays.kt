@file:Suppress("RemoveRedundantCallsOfConversionMethods", "PackageDirectoryMismatch")

package korlibs.memory.arrays

import korlibs.math.*

expect class ArrayBuffer(length: Int) {
    val byteLength: Int
    fun slice(begin: Int, end: Int): ArrayBuffer
}

expect interface ArrayBufferView {
    val buffer: ArrayBuffer
    val byteOffset: Int
    val byteLength: Int
}
expect class Int8Array : ArrayBufferView {
    constructor(length: Int)
    constructor(buffer: ArrayBuffer, byteOffset: Int, length: Int)
    val length: Int
}
expect class Int16Array : ArrayBufferView {
    constructor(length: Int)
    constructor(buffer: ArrayBuffer, byteOffset: Int, length: Int)
    val length: Int
}
expect class Int32Array : ArrayBufferView {
    constructor(length: Int)
    constructor(buffer: ArrayBuffer, byteOffset: Int, length: Int)
    val length: Int
}
expect class Float32Array : ArrayBufferView {
    constructor(length: Int)
    constructor(buffer: ArrayBuffer, byteOffset: Int, length: Int)
    val length: Int
}
expect class Float64Array : ArrayBufferView {
    constructor(length: Int)
    constructor(buffer: ArrayBuffer, byteOffset: Int, length: Int)
    val length: Int
}
expect class Uint8ClampedArray : ArrayBufferView {
    constructor(length: Int)
    constructor(buffer: ArrayBuffer, byteOffset: Int, length: Int)
    val length: Int
}
expect class Uint8Array : ArrayBufferView {
    constructor(length: Int)
    constructor(buffer: ArrayBuffer, byteOffset: Int, length: Int)
    val length: Int
}
expect class Uint16Array : ArrayBufferView {
    constructor(length: Int)
    constructor(buffer: ArrayBuffer, byteOffset: Int, length: Int)
    val length: Int
}
expect class DataView : ArrayBufferView {
    constructor(buffer: ArrayBuffer, byteOffset: Int, byteLength: Int)
    fun getInt8(byteOffset: Int): Byte
    fun getInt16(byteOffset: Int, littleEndian: Boolean): Short
    fun getInt32(byteOffset: Int, littleEndian: Boolean): Int
    fun getFloat32(byteOffset: Int, littleEndian: Boolean): Float
    fun getFloat64(byteOffset: Int, littleEndian: Boolean): Double
    fun setInt8(byteOffset: Int, value: Byte)
    fun setInt16(byteOffset: Int, value: Short, littleEndian: Boolean)
    fun setInt32(byteOffset: Int, value: Int, littleEndian: Boolean)
    fun setFloat32(byteOffset: Int, value: Float, littleEndian: Boolean)
    fun setFloat64(byteOffset: Int, value: Double, littleEndian: Boolean)
}

expect operator fun Int8Array.get(index: Int): Byte
expect operator fun Int16Array.get(index: Int): Short
expect operator fun Int32Array.get(index: Int): Int
expect operator fun Float32Array.get(index: Int): Float
expect operator fun Float64Array.get(index: Int): Double
expect operator fun Uint8ClampedArray.get(index: Int): Int
expect operator fun Uint8Array.get(index: Int): Int
expect operator fun Uint16Array.get(index: Int): Int

expect operator fun Int8Array.set(index: Int, value: Byte)
expect operator fun Int16Array.set(index: Int, value: Short)
expect operator fun Int32Array.set(index: Int, value: Int)
expect operator fun Float32Array.set(index: Int, value: Float)
expect operator fun Float64Array.set(index: Int, value: Double)
expect operator fun Uint8ClampedArray.set(index: Int, value: Int)
expect operator fun Uint8Array.set(index: Int, value: Int)
expect operator fun Uint16Array.set(index: Int, value: Int)

fun ArrayBuffer.slice(begin: Int): ArrayBuffer = slice(begin, byteLength)

fun Int8Array(buffer: ArrayBuffer, byteOffset: Int = 0): Int8Array = Int8Array(buffer, byteOffset, buffer.byteLength - byteOffset)
fun Int16Array(buffer: ArrayBuffer, byteOffset: Int = 0): Int16Array = Int16Array(buffer, byteOffset, buffer.byteLength - byteOffset)
fun Int32Array(buffer: ArrayBuffer, byteOffset: Int = 0): Int32Array = Int32Array(buffer, byteOffset, buffer.byteLength - byteOffset)
fun Float32Array(buffer: ArrayBuffer, byteOffset: Int = 0): Float32Array = Float32Array(buffer, byteOffset, buffer.byteLength - byteOffset)
fun Float64Array(buffer: ArrayBuffer, byteOffset: Int = 0): Float64Array = Float64Array(buffer, byteOffset, buffer.byteLength - byteOffset)
fun Uint8ClampedArray(buffer: ArrayBuffer, byteOffset: Int = 0): Uint8ClampedArray = Uint8ClampedArray(buffer, byteOffset, buffer.byteLength - byteOffset)
fun Uint8Array(buffer: ArrayBuffer, byteOffset: Int = 0): Uint8Array = Uint8Array(buffer, byteOffset, buffer.byteLength - byteOffset)
fun Uint16Array(buffer: ArrayBuffer, byteOffset: Int = 0): Uint16Array = Uint16Array(buffer, byteOffset, buffer.byteLength - byteOffset)

fun DataView(size: Int): DataView {
    check(size.isMultipleOf(8)) { "size=$size not multiple of 8"}
    return DataView(ArrayBuffer(size))
}
fun DataView(buffer: ArrayBuffer): DataView = DataView(buffer, 0, buffer.byteLength)

fun DataView.getUint8(byteOffset: Int): Int = getInt8(byteOffset).toInt() and 0xFF
fun DataView.getUint16(byteOffset: Int, littleEndian: Boolean): Int = getInt16(byteOffset, littleEndian).toInt() and 0xFFFF
fun DataView.getUint32(byteOffset: Int, littleEndian: Boolean): UInt = getInt32(byteOffset, littleEndian).toUInt()

fun DataView.setUint8(byteOffset: Int, value: Int) = setInt8(byteOffset, value.toByte())
fun DataView.setUint16(byteOffset: Int, value: Int, littleEndian: Boolean) = setInt16(byteOffset, value.toShort(), littleEndian)
fun DataView.setUint32(byteOffset: Int, value: UInt, littleEndian: Boolean) = setInt32(byteOffset, value.toInt(), littleEndian)


fun DataView.getInt16LE(byteOffset: Int): Short = getInt16(byteOffset, true)
fun DataView.getUint16LE(byteOffset: Int): Int = getUint16(byteOffset, true).toInt()
fun DataView.getInt32LE(byteOffset: Int): Int = getInt32(byteOffset, true)
fun DataView.getUint32LE(byteOffset: Int): UInt = getUint32(byteOffset, true).toUInt()
fun DataView.getFloat32LE(byteOffset: Int): Float = getFloat32(byteOffset, true)
fun DataView.getFloat64LE(byteOffset: Int): Double = getFloat64(byteOffset, true)
fun DataView.getInt16BE(byteOffset: Int): Short = getInt16(byteOffset, false)
fun DataView.getUint16BE(byteOffset: Int): Int = getUint16(byteOffset, false).toInt()
fun DataView.getInt32BE(byteOffset: Int): Int = getInt32(byteOffset, false)
fun DataView.getUint32BE(byteOffset: Int): UInt = getUint32(byteOffset, false).toUInt()
fun DataView.getFloat32BE(byteOffset: Int): Float = getFloat32(byteOffset, false)
fun DataView.getFloat64BE(byteOffset: Int): Double = getFloat64(byteOffset, false)

fun DataView.setInt16LE(byteOffset: Int, value: Short) { setInt16(byteOffset, value, true) }
fun DataView.setUint16LE(byteOffset: Int, value: Int) { setUint16(byteOffset, value, true) }
fun DataView.setInt32LE(byteOffset: Int, value: Int) { setInt32(byteOffset, value, true) }
fun DataView.setUint32LE(byteOffset: Int, value: UInt) { setUint32(byteOffset, value, true) }
fun DataView.setFloat32LE(byteOffset: Int, value: Float) { setFloat32(byteOffset, value, true) }
fun DataView.setFloat64LE(byteOffset: Int, value: Double) { setFloat64(byteOffset, value, true) }
fun DataView.setInt16BE(byteOffset: Int, value: Short) { setInt16(byteOffset, value, false) }
fun DataView.setUint16BE(byteOffset: Int, value: Int) { setUint16(byteOffset, value, false) }
fun DataView.setInt32BE(byteOffset: Int, value: Int) { setInt32(byteOffset, value, false) }
fun DataView.setUint32BE(byteOffset: Int, value: UInt) { setUint32(byteOffset, value, false) }
fun DataView.setFloat32BE(byteOffset: Int, value: Float) { setFloat32(byteOffset, value, false) }
fun DataView.setFloat64BE(byteOffset: Int, value: Double) { setFloat64(byteOffset, value, false) }
