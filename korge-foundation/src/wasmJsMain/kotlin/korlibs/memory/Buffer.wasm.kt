package korlibs.memory

import korlibs.memory.wasm.*
import org.khronos.webgl.*

fun ArrayBuffer.asUint8ClampedArray(): Uint8ClampedArray = Uint8ClampedArray(this)
fun ArrayBuffer.asUint8Array(): Uint8Array = Uint8Array(this)
fun ArrayBuffer.asInt8Array(): Int8Array = Int8Array(this)
fun ArrayBuffer.asInt16Array(): Int16Array = Int16Array(this)
fun ArrayBuffer.asInt32Array(): Int32Array = Int32Array(this)
fun ArrayBuffer.asFloat32Array(): Float32Array = Float32Array(this)
fun ArrayBuffer.asFloat64Array(): Float64Array = Float64Array(this)

fun ArrayBuffer.toUByteArray(): UByteArray = asUint8Array().toByteArray().asUByteArray()
fun ArrayBuffer.toByteArray(): ByteArray = asInt8Array().toByteArray()
fun ArrayBuffer.toShortArray(): ShortArray = asInt16Array().toShortArray()
fun ArrayBuffer.toIntArray(): IntArray = asInt32Array().toIntArray()
fun ArrayBuffer.toFloatArray(): FloatArray = asFloat32Array().toFloatArray()
fun ArrayBuffer.toDoubleArray(): DoubleArray = asFloat64Array().toDoubleArray()

val Buffer.buffer: ArrayBuffer get() = data.buffer
val Buffer.arrayUByte: Uint8Array get() = Uint8Array(this.buffer, byteOffset, sizeInBytes)
val Buffer.arrayByte: Int8Array get() = Int8Array(buffer, byteOffset, sizeInBytes)
val Buffer.arrayShort: Int16Array get() = Int16Array(buffer, byteOffset, sizeInBytes / 2)
val Buffer.arrayInt: Int32Array get() = Int32Array(buffer, byteOffset, sizeInBytes / 4)
val Buffer.arrayFloat: Float32Array get() = Float32Array(buffer, byteOffset, sizeInBytes / 4)
val Buffer.arrayDouble: Float64Array get() = Float64Array(buffer, byteOffset, sizeInBytes / 8)
