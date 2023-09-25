@file:Suppress("PackageDirectoryMismatch")

package korlibs.memory.arrays

actual typealias ArrayBuffer = org.khronos.webgl.ArrayBuffer
actual typealias ArrayBufferView = org.khronos.webgl.ArrayBufferView

actual typealias Int8Array = org.khronos.webgl.Int8Array
actual typealias Int16Array = org.khronos.webgl.Int16Array
actual typealias Int32Array = org.khronos.webgl.Int32Array
actual typealias Float32Array = org.khronos.webgl.Float32Array
actual typealias Float64Array = org.khronos.webgl.Float64Array
actual typealias Uint8ClampedArray = org.khronos.webgl.Uint8ClampedArray
actual typealias Uint8Array = org.khronos.webgl.Uint8Array
actual typealias Uint16Array = org.khronos.webgl.Uint16Array

actual typealias DataView = org.khronos.webgl.DataView

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
