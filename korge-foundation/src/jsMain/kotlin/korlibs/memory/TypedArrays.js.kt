@file:Suppress("PackageDirectoryMismatch")

package korlibs.memory.arrays

actual typealias BufferDataSource = org.khronos.webgl.BufferDataSource
actual typealias ArrayBuffer = org.khronos.webgl.ArrayBuffer
actual typealias ArrayBufferView = org.khronos.webgl.ArrayBufferView

actual fun ArrayBufferDirect(size: Int): ArrayBuffer = ArrayBuffer(size)
actual fun ArrayBufferWrap(data: ByteArray): ArrayBuffer {
    val int8Array = data.unsafeCast<Int8Array>()
    check(int8Array.byteOffset == 0)
    return int8Array.buffer
}
internal actual fun ArrayBuffer_copy(src: ArrayBuffer, srcPos: Int, dst: ArrayBuffer, dstPos: Int, length: Int) {
    Int8Array(dst, dstPos, length).set(Int8Array(src, srcPos, length), 0)
}

actual inline fun ArrayBuffer.uint8ClampedArray(byteOffset: Int, length: Int): Uint8ClampedArray = org.khronos.webgl.Uint8ClampedArray(this, byteOffset, length)
actual inline fun ArrayBuffer.uint8Array(byteOffset: Int, length: Int): Uint8Array = org.khronos.webgl.Uint8Array(this, byteOffset, length)
actual inline fun ArrayBuffer.uint16Array(byteOffset: Int, length: Int): Uint16Array = org.khronos.webgl.Uint16Array(this, byteOffset, length)
actual inline fun ArrayBuffer.int8Array(byteOffset: Int, length: Int): Int8Array = org.khronos.webgl.Int8Array(this, byteOffset, length)
actual inline fun ArrayBuffer.int16Array(byteOffset: Int, length: Int): Int16Array = org.khronos.webgl.Int16Array(this, byteOffset, length)
actual inline fun ArrayBuffer.int32Array(byteOffset: Int, length: Int): Int32Array = org.khronos.webgl.Int32Array(this, byteOffset, length)
actual inline fun ArrayBuffer.float32Array(byteOffset: Int, length: Int): Float32Array = org.khronos.webgl.Float32Array(this, byteOffset, length)
actual inline fun ArrayBuffer.float64Array(byteOffset: Int, length: Int): Float64Array = org.khronos.webgl.Float64Array(this, byteOffset, length)
actual inline fun ArrayBuffer.dataView(byteOffset: Int, length: Int): DataView = org.khronos.webgl.DataView(this, byteOffset, length)

actual typealias Int8Array = org.khronos.webgl.Int8Array
actual typealias Int16Array = org.khronos.webgl.Int16Array
actual typealias Int32Array = org.khronos.webgl.Int32Array
actual typealias Float32Array = org.khronos.webgl.Float32Array
actual typealias Float64Array = org.khronos.webgl.Float64Array
actual typealias Uint8ClampedArray = org.khronos.webgl.Uint8ClampedArray
actual typealias Uint8Array = org.khronos.webgl.Uint8Array
actual typealias Uint16Array = org.khronos.webgl.Uint16Array

actual typealias DataView = org.khronos.webgl.DataView

actual inline fun DataView.getInt8(byteOffset: Int): Byte = (this.unsafeCast<org.khronos.webgl.DataView>().getInt8(byteOffset))
actual inline fun DataView.getInt16(byteOffset: Int, littleEndian: Boolean): Short = (this.unsafeCast<org.khronos.webgl.DataView>().getInt16(byteOffset, littleEndian))
actual inline fun DataView.getInt32(byteOffset: Int, littleEndian: Boolean): Int = (this.unsafeCast<org.khronos.webgl.DataView>().getInt32(byteOffset, littleEndian))
actual inline fun DataView.getFloat32(byteOffset: Int, littleEndian: Boolean): Float = (this.unsafeCast<org.khronos.webgl.DataView>().getFloat32(byteOffset, littleEndian))
actual inline fun DataView.getFloat64(byteOffset: Int, littleEndian: Boolean): Double = (this.unsafeCast<org.khronos.webgl.DataView>().getFloat64(byteOffset, littleEndian))
actual inline fun DataView.setInt8(byteOffset: Int, value: Byte) { (this.unsafeCast<org.khronos.webgl.DataView>().setInt8(byteOffset, value)) }
actual inline fun DataView.setInt16(byteOffset: Int, value: Short, littleEndian: Boolean) { (this.unsafeCast<org.khronos.webgl.DataView>().setInt16(byteOffset, value, littleEndian)) }
actual inline fun DataView.setInt32(byteOffset: Int, value: Int, littleEndian: Boolean) { (this.unsafeCast<org.khronos.webgl.DataView>().setInt32(byteOffset, value, littleEndian)) }
actual inline fun DataView.setFloat32(byteOffset: Int, value: Float, littleEndian: Boolean) { (this.unsafeCast<org.khronos.webgl.DataView>().setFloat32(byteOffset, value, littleEndian)) }
actual inline fun DataView.setFloat64(byteOffset: Int, value: Double, littleEndian: Boolean) { (this.unsafeCast<org.khronos.webgl.DataView>().setFloat64(byteOffset, value, littleEndian)) }

actual inline operator fun Int8Array.get(index: Int): Byte = asDynamic()[index]
actual inline operator fun Int16Array.get(index: Int): Short = asDynamic()[index]
actual inline operator fun Int32Array.get(index: Int): Int = asDynamic()[index]
actual inline operator fun Float32Array.get(index: Int): Float = asDynamic()[index]
actual inline operator fun Float64Array.get(index: Int): Double = asDynamic()[index]
actual inline operator fun Uint8ClampedArray.get(index: Int): Int = asDynamic()[index]
actual inline operator fun Uint8Array.get(index: Int): Int = asDynamic()[index]
actual inline operator fun Uint16Array.get(index: Int): Int = asDynamic()[index]

actual inline operator fun Int8Array.set(index: Int, value: Byte) { asDynamic()[index] = value }
actual inline operator fun Int16Array.set(index: Int, value: Short) { asDynamic()[index] = value }
actual inline operator fun Int32Array.set(index: Int, value: Int) { asDynamic()[index] = value }
actual inline operator fun Float32Array.set(index: Int, value: Float) { asDynamic()[index] = value }
actual inline operator fun Float64Array.set(index: Int, value: Double) { asDynamic()[index] = value }
actual inline operator fun Uint8ClampedArray.set(index: Int, value: Int) { asDynamic()[index] = value }
actual inline operator fun Uint8Array.set(index: Int, value: Int) { asDynamic()[index] = value }
actual inline operator fun Uint16Array.set(index: Int, value: Int) { asDynamic()[index] = value }

fun Int8Array.asByteArray(): ByteArray = unsafeCast<ByteArray>()
fun Int16Array.asShortArray(): ShortArray = unsafeCast<ShortArray>()
fun Int32Array.asIntArray(): IntArray = unsafeCast<IntArray>()
fun Float32Array.asFloatArray(): FloatArray = unsafeCast<FloatArray>()
fun Float64Array.asDoubleArray(): DoubleArray = unsafeCast<DoubleArray>()
fun Uint16Array.asCharArray(): CharArray = unsafeCast<CharArray>()

fun ByteArray.asInt8Array(): Int8Array = unsafeCast<Int8Array>()
fun ShortArray.asInt16Array(): Int16Array = unsafeCast<Int16Array>()
fun IntArray.asInt32Array(): Int32Array = unsafeCast<Int32Array>()
fun FloatArray.asFloat32Array(): Float32Array = unsafeCast<Float32Array>()
fun DoubleArray.asFloat64Array(): Float64Array = unsafeCast<Float64Array>()
fun CharArray.asUint16Array(): Uint16Array = unsafeCast<Uint16Array>()
