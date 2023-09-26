package korlibs.memory

import korlibs.memory.arrays.*
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Float64Array
import org.khronos.webgl.Int16Array
import org.khronos.webgl.Int32Array
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.Uint8ClampedArray

fun ArrayBuffer.asUint8ClampedArray(): Uint8ClampedArray = Uint8ClampedArray(this)
fun ArrayBuffer.asUint8Array(): Uint8Array = Uint8Array(this)
fun ArrayBuffer.asInt8Array(): Int8Array = Int8Array(this)
fun ArrayBuffer.asInt16Array(): Int16Array = Int16Array(this)
fun ArrayBuffer.asInt32Array(): Int32Array = Int32Array(this)
fun ArrayBuffer.asFloat32Array(): Float32Array = Float32Array(this)
fun ArrayBuffer.asFloat64Array(): Float64Array = Float64Array(this)

fun ArrayBuffer.asUByteArray(): UByteArray = asUint8Array().unsafeCast<ByteArray>().asUByteArray()
fun ArrayBuffer.asByteArray(): ByteArray = asInt8Array().unsafeCast<ByteArray>()
fun ArrayBuffer.asShortArray(): ShortArray = asInt16Array().unsafeCast<ShortArray>()
fun ArrayBuffer.asIntArray(): IntArray = asInt32Array().unsafeCast<IntArray>()
fun ArrayBuffer.asFloatArray(): FloatArray = asFloat32Array().unsafeCast<FloatArray>()
fun ArrayBuffer.asDoubleArray(): DoubleArray = asFloat64Array().unsafeCast<DoubleArray>()

val Buffer.buffer: ArrayBuffer get() = data.buffer
val Buffer.arrayUByte: Uint8Array get() = Uint8Array(buffer, byteOffset, sizeInBytes)
val Buffer.arrayByte: Int8Array get() = Int8Array(buffer, byteOffset, sizeInBytes)
val Buffer.arrayShort: Int16Array get() = Int16Array(buffer, byteOffset, sizeInBytes / 2)
val Buffer.arrayInt: Int32Array get() = Int32Array(buffer, byteOffset, sizeInBytes / 4)
val Buffer.arrayFloat: Float32Array get() = Float32Array(buffer, byteOffset, sizeInBytes / 4)
val Buffer.arrayDouble: Float64Array get() = Float64Array(buffer, byteOffset, sizeInBytes / 8)
