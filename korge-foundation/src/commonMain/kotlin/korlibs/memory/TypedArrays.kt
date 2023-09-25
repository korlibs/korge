@file:Suppress("RemoveRedundantCallsOfConversionMethods", "PackageDirectoryMismatch")

package korlibs.memory.arrays

import korlibs.math.*
import korlibs.memory.*

expect interface BufferDataSource

expect class ArrayBuffer(length: Int) : BufferDataSource {
    val byteLength: Int
}
//fun ArrayBuffer.subArray(begin: Int, end: Int = byteLength): ArrayBuffer {
//    TODO()
//}

expect interface ArrayBufferView : BufferDataSource {
    val buffer: ArrayBuffer
    val byteOffset: Int
    val byteLength: Int
}

fun ArrayBuffer(size: Int, direct: Boolean): ArrayBuffer = if (direct) ArrayBufferDirect(size) else ArrayBuffer(size)
expect fun ArrayBufferDirect(size: Int): ArrayBuffer
expect fun ArrayBufferWrap(data: ByteArray): ArrayBuffer
internal expect fun ArrayBuffer_copy(src: ArrayBuffer, srcPos: Int, dst: ArrayBuffer, dstPos: Int, length: Int)

fun ArrayBuffer.readBytes(bytes: ByteArray, start: Int = 0, end: Int = bytes.size, offset: Int = 0) {
    ArrayBuffer_copy(ArrayBufferWrap(bytes), start, this, offset, end - start)
}
fun ArrayBuffer.writeBytes(bytes: ByteArray, start: Int = 0, end: Int = bytes.size, offset: Int = 0) {
    ArrayBuffer_copy(this, offset, ArrayBufferWrap(bytes), start, end - start)
}

fun ArrayBufferView.setByteOffset(typedArray: ArrayBufferView, targetOffsetBytes: Int = 0) =
    ArrayBuffer_copy(typedArray.buffer, typedArray.byteOffset, this.buffer, this.byteOffset + targetOffsetBytes, typedArray.byteLength)

fun Uint8ClampedArray.set(typedArray: ArrayBufferView, targetOffset: Int = 0) = setByteOffset(typedArray, targetOffset * 1)
fun Uint8Array.set(typedArray: ArrayBufferView, targetOffset: Int = 0) = setByteOffset(typedArray, targetOffset * 1)
fun Uint16Array.set(typedArray: ArrayBufferView, targetOffset: Int = 0) = setByteOffset(typedArray, targetOffset * 2)
fun Int8Array.set(typedArray: ArrayBufferView, targetOffset: Int = 0) = setByteOffset(typedArray, targetOffset * 1)
fun Int16Array.set(typedArray: ArrayBufferView, targetOffset: Int = 0) = setByteOffset(typedArray, targetOffset * 2)
fun Int32Array.set(typedArray: ArrayBufferView, targetOffset: Int = 0) = setByteOffset(typedArray, targetOffset * 4)
fun Int64Array.set(typedArray: ArrayBufferView, targetOffset: Int = 0) = setByteOffset(typedArray, targetOffset * 8)
fun Float32Array.set(typedArray: ArrayBufferView, targetOffset: Int = 0) = setByteOffset(typedArray, targetOffset * 4)
fun Float64Array.set(typedArray: ArrayBufferView, targetOffset: Int = 0) = setByteOffset(typedArray, targetOffset * 8)

fun Uint8ClampedArray.subarray(begin: Int, end: Int = length): Uint8ClampedArray = Uint8ClampedArray(buffer, byteOffset + begin * 1, end - begin)
fun Uint8Array.subarray(begin: Int, end: Int = length): Uint8Array = Uint8Array(buffer, byteOffset + begin * 1, end - begin)
fun Uint16Array.subarray(begin: Int, end: Int = length): Uint16Array = Uint16Array(buffer, byteOffset + begin * 2, end - begin)
fun Int8Array.subarray(begin: Int, end: Int = length): Int8Array = Int8Array(buffer, byteOffset + begin * 1, end - begin)
fun Int16Array.subarray(begin: Int, end: Int = length): Int16Array = Int16Array(buffer, byteOffset + begin * 2, end - begin)
fun Int32Array.subarray(begin: Int, end: Int = length): Int32Array = Int32Array(buffer, byteOffset + begin * 4, end - begin)
fun Float32Array.subarray(begin: Int, end: Int = length): Float32Array = Float32Array(buffer, byteOffset + begin * 4, end - begin)
fun Float64Array.subarray(begin: Int, end: Int = length): Float64Array = Float64Array(buffer, byteOffset + begin * 8, end - begin)

fun ArrayBufferView.asUint8ClampedArray(): Uint8ClampedArray = Uint8ClampedArray(buffer, byteOffset, byteLength / 1)
fun ArrayBufferView.asUint8Array(): Uint8Array = Uint8Array(buffer, byteOffset, byteLength / 1)
fun ArrayBufferView.asUint16Array(): Uint16Array = Uint16Array(buffer, byteOffset, byteLength / 2)
fun ArrayBufferView.asInt8Array(): Int8Array = Int8Array(buffer, byteOffset, byteLength / 1)
fun ArrayBufferView.asInt16Array(): Int16Array = Int16Array(buffer, byteOffset, byteLength / 2)
fun ArrayBufferView.asInt32Array(): Int32Array = Int32Array(buffer, byteOffset, byteLength / 4)
fun ArrayBufferView.asFloat32Array(): Float32Array = Float32Array(buffer, byteOffset, byteLength / 4)
fun ArrayBufferView.asFloat64Array(): Float64Array = Float64Array(buffer, byteOffset, byteLength / 8)

private fun ArrayBufferView._offsetS(index: Int, size: Int): Int {
    val roffset = index * size
    if (roffset !in 0 .. (byteLength - size)) error("Out of bounds")
    return (byteOffset + roffset)
}
@PublishedApi internal fun Uint8ClampedArray.byteOffset(index: Int): Int = _offsetS(index, 1)
@PublishedApi internal fun Uint8Array.byteOffset(index: Int): Int = _offsetS(index, 1)
@PublishedApi internal fun Uint16Array.byteOffset(index: Int): Int = _offsetS(index, 2)
@PublishedApi internal fun Int8Array.byteOffset(index: Int): Int = _offsetS(index, 1)
@PublishedApi internal fun Int16Array.byteOffset(index: Int): Int = _offsetS(index, 2)
@PublishedApi internal fun Int32Array.byteOffset(index: Int): Int = _offsetS(index, 4)
@PublishedApi internal fun Float32Array.byteOffset(index: Int): Int = _offsetS(index, 4)
@PublishedApi internal fun Float64Array.byteOffset(index: Int): Int = _offsetS(index, 8)
@PublishedApi internal fun DataView.byteOffset(offset: Int, size: Int): Int {
    if (offset < 0 || offset + size > byteLength) error("Offset is outside the bounds of the DataView")
    return byteOffset + offset
}

expect inline fun ArrayBuffer.uint8ClampedArray(byteOffset: Int = 0, length: Int = this.byteLength - byteOffset): Uint8ClampedArray
expect inline fun ArrayBuffer.uint8Array(byteOffset: Int = 0, length: Int = this.byteLength - byteOffset): Uint8Array
expect inline fun ArrayBuffer.uint16Array(byteOffset: Int = 0, length: Int = (this.byteLength - byteOffset) / 2): Uint16Array
expect inline fun ArrayBuffer.int8Array(byteOffset: Int = 0, length: Int = this.byteLength - byteOffset): Int8Array
expect inline fun ArrayBuffer.int16Array(byteOffset: Int = 0, length: Int = (this.byteLength - byteOffset) / 2): Int16Array
expect inline fun ArrayBuffer.int32Array(byteOffset: Int = 0, length: Int = (this.byteLength - byteOffset) / 4): Int32Array
expect inline fun ArrayBuffer.float32Array(byteOffset: Int = 0, length: Int = (this.byteLength - byteOffset) / 4): Float32Array
expect inline fun ArrayBuffer.float64Array(byteOffset: Int = 0, length: Int = (this.byteLength - byteOffset) / 8): Float64Array
expect inline fun ArrayBuffer.dataView(byteOffset: Int = 0, length: Int = this.byteLength - byteOffset): DataView

expect class Int8Array : ArrayBufferView {
    constructor(length: Int)
    val length: Int
}
expect class Int16Array : ArrayBufferView {
    constructor(length: Int)
    val length: Int
}
expect class Int32Array : ArrayBufferView {
    constructor(length: Int)
    val length: Int
}
expect class Float32Array : ArrayBufferView {
    constructor(length: Int)
    val length: Int
}
expect class Float64Array : ArrayBufferView {
    constructor(length: Int)
    val length: Int
}
expect class Uint8ClampedArray : ArrayBufferView {
    constructor(length: Int)
    val length: Int
}
expect class Uint8Array : ArrayBufferView {
    constructor(length: Int)
    val length: Int
}
expect class Uint16Array : ArrayBufferView {
    constructor(length: Int)
    val length: Int
}
expect class DataView : ArrayBufferView

expect fun DataView.getInt8(byteOffset: Int): Byte
expect fun DataView.getInt16(byteOffset: Int, littleEndian: Boolean): Short
expect fun DataView.getInt32(byteOffset: Int, littleEndian: Boolean): Int
expect fun DataView.getFloat32(byteOffset: Int, littleEndian: Boolean): Float
expect fun DataView.getFloat64(byteOffset: Int, littleEndian: Boolean): Double
expect fun DataView.setInt8(byteOffset: Int, value: Byte)
expect fun DataView.setInt16(byteOffset: Int, value: Short, littleEndian: Boolean)
expect fun DataView.setInt32(byteOffset: Int, value: Int, littleEndian: Boolean)
expect fun DataView.setFloat32(byteOffset: Int, value: Float, littleEndian: Boolean)
expect fun DataView.setFloat64(byteOffset: Int, value: Double, littleEndian: Boolean)

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

operator fun Int64Array.get(index: Int): Long {
    return Long.fromLowHigh(ints[index * 2 + 0], ints[index * 2 + 1])
}
operator fun Int64Array.set(index: Int, value: Long) {
    ints[index * 2 + 0] = value.low
    ints[index * 2 + 1] = value.high
}

class Int64Array(override val buffer: ArrayBuffer, override val byteOffset: Int = 0, val length: Int = (buffer.byteLength - byteOffset) / 8, unit: Unit = Unit) : ArrayBufferView {
    val ints = Int32Array(buffer, byteOffset, length * 2)
    override val byteLength: Int = length * 8
    constructor(length: Int) : this(ArrayBuffer(length * 8))
}

fun Int8Array(buffer: ArrayBuffer, byteOffset: Int = 0, length: Int = (buffer.byteLength - byteOffset) / 1, unit: Unit = Unit): Int8Array = buffer.int8Array(byteOffset, length)
fun Int16Array(buffer: ArrayBuffer, byteOffset: Int = 0, length: Int = (buffer.byteLength - byteOffset) / 2, unit: Unit = Unit): Int16Array = buffer.int16Array(byteOffset, length)
fun Int32Array(buffer: ArrayBuffer, byteOffset: Int = 0, length: Int = (buffer.byteLength - byteOffset) / 4, unit: Unit = Unit): Int32Array = buffer.int32Array(byteOffset, length)
fun Float32Array(buffer: ArrayBuffer, byteOffset: Int = 0, length: Int = (buffer.byteLength - byteOffset) / 4, unit: Unit = Unit): Float32Array = buffer.float32Array(byteOffset, length)
fun Float64Array(buffer: ArrayBuffer, byteOffset: Int = 0, length: Int = (buffer.byteLength - byteOffset) / 8, unit: Unit = Unit): Float64Array = buffer.float64Array(byteOffset, length)
fun Uint8ClampedArray(buffer: ArrayBuffer, byteOffset: Int = 0, length: Int = (buffer.byteLength - byteOffset) / 1, unit: Unit = Unit): Uint8ClampedArray = buffer.uint8ClampedArray(byteOffset, length)
fun Uint8Array(buffer: ArrayBuffer, byteOffset: Int = 0, length: Int = (buffer.byteLength - byteOffset) / 1, unit: Unit = Unit): Uint8Array = buffer.uint8Array(byteOffset, length)
fun Uint16Array(buffer: ArrayBuffer, byteOffset: Int = 0, length: Int = (buffer.byteLength - byteOffset) / 2, unit: Unit = Unit): Uint16Array = buffer.uint16Array(byteOffset, length)

fun DataView(size: Int): DataView {
    check(size.isMultipleOf(8)) { "size=$size not multiple of 8"}
    return DataView(ArrayBuffer(size))
}
fun DataView(buffer: ArrayBuffer, byteOffset: Int = 0, byteLength: Int = buffer.byteLength - byteOffset, unit: Unit = Unit): DataView = buffer.dataView(byteOffset, byteLength)

fun DataView.getUint8(byteOffset: Int): Int = getInt8(byteOffset).toInt() and 0xFF
fun DataView.getUint16(byteOffset: Int, littleEndian: Boolean): Int = getInt16(byteOffset, littleEndian).toInt() and 0xFFFF
fun DataView.getUint32(byteOffset: Int, littleEndian: Boolean): UInt = getInt32(byteOffset, littleEndian).toUInt()

fun DataView.setUint8(byteOffset: Int, value: Int) = setInt8(byteOffset, value.toByte())
fun DataView.setUint16(byteOffset: Int, value: Int, littleEndian: Boolean) = setInt16(byteOffset, value.toShort(), littleEndian)
fun DataView.setUint32(byteOffset: Int, value: UInt, littleEndian: Boolean) = setInt32(byteOffset, value.toInt(), littleEndian)

fun DataView.setS8(byteOffset: Int, value: Byte) = setInt8(byteOffset, value.toByte())
fun DataView.setS8(byteOffset: Int, value: Int) = setInt8(byteOffset, value.toByte())
fun DataView.setU8(byteOffset: Int, value: Int) = setInt8(byteOffset, value.toByte())

fun DataView.getS8(byteOffset: Int): Byte = getInt8(byteOffset)
fun DataView.getU8(byteOffset: Int): Int = getUint8(byteOffset).toInt()

fun DataView.getS16LE(byteOffset: Int): Short = getInt16(byteOffset, true)
fun DataView.getU16LE(byteOffset: Int): Int = getUint16(byteOffset, true).toInt()
fun DataView.getS32LE(byteOffset: Int): Int = getInt32(byteOffset, true)
fun DataView.getU32LE(byteOffset: Int): UInt = getUint32(byteOffset, true).toUInt()
fun DataView.getF32LE(byteOffset: Int): Float = getFloat32(byteOffset, true)
fun DataView.getF64LE(byteOffset: Int): Double = getFloat64(byteOffset, true)
fun DataView.getS16BE(byteOffset: Int): Short = getInt16(byteOffset, false)
fun DataView.getU16BE(byteOffset: Int): Int = getUint16(byteOffset, false).toInt()
fun DataView.getS32BE(byteOffset: Int): Int = getInt32(byteOffset, false)
fun DataView.getU32BE(byteOffset: Int): UInt = getUint32(byteOffset, false).toUInt()
fun DataView.getF32BE(byteOffset: Int): Float = getFloat32(byteOffset, false)
fun DataView.getF64BE(byteOffset: Int): Double = getFloat64(byteOffset, false)

fun DataView.setS16LE(byteOffset: Int, value: Short) { setInt16(byteOffset, value, true) }
fun DataView.setU16LE(byteOffset: Int, value: Int) { setUint16(byteOffset, value, true) }
fun DataView.setS32LE(byteOffset: Int, value: Int) { setInt32(byteOffset, value, true) }
fun DataView.setU32LE(byteOffset: Int, value: UInt) { setUint32(byteOffset, value, true) }
fun DataView.setF32LE(byteOffset: Int, value: Float) { setFloat32(byteOffset, value, true) }
fun DataView.setF64LE(byteOffset: Int, value: Double) { setFloat64(byteOffset, value, true) }
fun DataView.setS16BE(byteOffset: Int, value: Short) { setInt16(byteOffset, value, false) }
fun DataView.setU16BE(byteOffset: Int, value: Int) { setUint16(byteOffset, value, false) }
fun DataView.setS32BE(byteOffset: Int, value: Int) { setInt32(byteOffset, value, false) }
fun DataView.setU32BE(byteOffset: Int, value: UInt) { setUint32(byteOffset, value, false) }
fun DataView.setF32BE(byteOffset: Int, value: Float) { setFloat32(byteOffset, value, false) }
fun DataView.setF64BE(byteOffset: Int, value: Double) { setFloat64(byteOffset, value, false) }

fun DataView.setS64LE(byteOffset: Int, value: Long) {
    setS32LE(byteOffset + 0, value.low)
    setS32LE(byteOffset + 4, value.high)
}
fun DataView.setS64BE(byteOffset: Int, value: Long) {
    setS32BE(byteOffset + 0, value.high)
    setS32BE(byteOffset + 4, value.low)
}

fun DataView.getS64LE(byteOffset: Int): Long = Long.fromLowHigh(getS32LE(byteOffset + 0), getS32LE(byteOffset + 4))
fun DataView.getS64BE(byteOffset: Int): Long = Long.fromLowHigh(getS32BE(byteOffset + 4), getS32BE(byteOffset + 0))

fun Uint8ClampedArray(size: Int, direct: Boolean): Uint8ClampedArray = Uint8ClampedArray(ArrayBuffer(size * 1, direct))
fun Uint8Array(size: Int, direct: Boolean): Uint8Array = Uint8Array(ArrayBuffer(size * 1, direct))
fun Uint16Array(size: Int, direct: Boolean): Uint16Array = Uint16Array(ArrayBuffer(size * 2, direct))
fun Int8Array(size: Int, direct: Boolean): Int8Array = Int8Array(ArrayBuffer(size * 1, direct))
fun Int16Array(size: Int, direct: Boolean): Int16Array = Int16Array(ArrayBuffer(size * 2, direct))
fun Int32Array(size: Int, direct: Boolean): Int32Array = Int32Array(ArrayBuffer(size * 4, direct))
fun Int64Array(size: Int, direct: Boolean): Int64Array = Int64Array(ArrayBuffer(size * 8, direct))
fun Float32Array(size: Int, direct: Boolean): Float32Array = Float32Array(ArrayBuffer(size * 4, direct))
fun Float64Array(size: Int, direct: Boolean): Float64Array = Float64Array(ArrayBuffer(size * 8, direct))

inline fun Uint8ClampedArray(size: Int, direct: Boolean = false, block: (Int) -> Int): Uint8ClampedArray = Uint8ClampedArray(size, direct).also { for (n in 0 until size) it[n] = block(n) }
inline fun Uint8Array(size: Int, direct: Boolean = false, block: (Int) -> Int): Uint8Array = Uint8Array(size, direct).also { for (n in 0 until size) it[n] = block(n) }
inline fun Uint16Array(size: Int, direct: Boolean = false, block: (Int) -> Int): Uint16Array = Uint16Array(size, direct).also { for (n in 0 until size) it[n] = block(n) }
inline fun Int8Array(size: Int, direct: Boolean = false, block: (Int) -> Byte): Int8Array = Int8Array(size, direct).also { for (n in 0 until size) it[n] = block(n) }
inline fun Int16Array(size: Int, direct: Boolean = false, block: (Int) -> Short): Int16Array = Int16Array(size, direct).also { for (n in 0 until size) it[n] = block(n) }
inline fun Int32Array(size: Int, direct: Boolean = false, block: (Int) -> Int): Int32Array = Int32Array(size, direct).also { for (n in 0 until size) it[n] = block(n) }
inline fun Int64Array(size: Int, direct: Boolean = false, block: (Int) -> Long): Int64Array = Int64Array(size, direct).also { for (n in 0 until size) it[n] = block(n) }
inline fun Float32Array(size: Int, direct: Boolean = false, block: (Int) -> Float): Float32Array = Float32Array(size, direct).also { for (n in 0 until size) it[n] = block(n) }
inline fun Float64Array(size: Int, direct: Boolean = false, block: (Int) -> Double): Float64Array = Float64Array(size, direct).also { for (n in 0 until size) it[n] = block(n) }

fun ByteArray.toInt8Array(): Int8Array = ArrayBufferWrap(this.copyOf()).int8Array()
fun Int8Array.toByteArray(): ByteArray = ByteArray(length) { this[it] }

fun UByteArray.toUint8ClampedArray(direct: Boolean = false): Uint8ClampedArray = Uint8ClampedArray(size, direct) { this[it].toInt() }
fun UByteArray.toUint8Array(direct: Boolean = false): Uint8Array = Uint8Array(size, direct) { this[it].toInt() }
fun UShortArray.toUint16Array(direct: Boolean = false): Uint16Array = Uint16Array(size, direct) { this[it].toInt() }
fun ShortArray.toInt16Array(direct: Boolean = false): Int16Array = Int16Array(size, direct) { this[it] }
fun IntArray.toInt32Array(direct: Boolean = false): Int32Array = Int32Array(size, direct) { this[it] }
fun LongArray.toInt64Array(direct: Boolean = false): Int64Array = Int64Array(size, direct) { this[it] }
fun FloatArray.toFloat32Array(direct: Boolean = false): Float32Array = Float32Array(size, direct) { this[it] }
fun DoubleArray.toFloat64Array(direct: Boolean = false): Float64Array = Float64Array(size, direct) { this[it] }

fun Uint8ClampedArray.toList(): List<UByte> = toUByteArray().toList()
fun Uint8Array.toList(): List<UByte> = toUByteArray().toList()
fun Uint16Array.toList(): List<UShort> = toUShortArray().toList()
fun Int8Array.toList(): List<Byte> = toByteArray().toList()
fun Int16Array.toList(): List<Short> = toShortArray().toList()
fun Int32Array.toList(): List<Int> = toIntArray().toList()
fun Float32Array.toList(): List<Float> = toFloatArray().toList()
fun Float64Array.toList(): List<Double> = toDoubleArray().toList()

fun Uint8ClampedArray.toUByteArray(): UByteArray = UByteArray(length) { this[it].toUByte() }
fun Uint8Array.toUByteArray(): UByteArray = UByteArray(length) { this[it].toUByte() }
fun Uint16Array.toUShortArray(): UShortArray = UShortArray(length) { this[it].toUShort() }
fun Int16Array.toShortArray(): ShortArray = ShortArray(length) { this[it].toShort() }
fun Int32Array.toIntArray(): IntArray = IntArray(length) { this[it].toInt() }
fun Int64Array.toLongArray(): LongArray = LongArray(length) { this[it].toLong() }
fun Float32Array.toFloatArray(): FloatArray = FloatArray(length) { this[it].toFloat() }
fun Float64Array.toDoubleArray(): DoubleArray = DoubleArray(length) { this[it].toDouble() }

//fun Uint8ClampedArray.toUByteArray(): UByteArray = asInt8Array().toByteArray().asUByteArray()
//fun Uint8Array.toUByteArray(): UByteArray = asInt8Array().toByteArray().asUByteArray()
//fun Uint16Array.toUShortArray(): UShortArray = UShortArray(length) { this[it].toUShort() }
//fun Int16Array.toShortArray(): ShortArray = ShortArray(length) { this[it] }
//fun Int32Array.toIntArray(): IntArray = IntArray(length) { this[it] }
//fun Int64Array.toLongArray(): LongArray = LongArray(length) { this[it] }
//fun Float32Array.toFloatArray(): FloatArray = FloatArray(length) { this[it] }
//fun Float64Array.toDoubleArray(): DoubleArray = DoubleArray(length) { this[it] }
