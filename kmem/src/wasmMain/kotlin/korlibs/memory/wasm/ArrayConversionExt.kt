package korlibs.memory.wasm

import org.khronos.webgl.*

internal fun ArrayBuffer.toByteArray(): ByteArray = Int8Array(this).toByteArray()
internal fun Uint8Array.toByteArray(): ByteArray {
    return Int8Array(this.buffer).toByteArray()
}
internal fun Int8Array.toByteArray(): ByteArray {
    val out = ByteArray(this.length)
    for (n in out.indices) out[n] = this[n]
    return out
}
internal fun Int16Array.toShortArray(): ShortArray {
    val out = ShortArray(this.length)
    for (n in out.indices) out[n] = this[n]
    return out
}
internal fun Int32Array.toIntArray(): IntArray {
    val out = IntArray(this.length)
    for (n in out.indices) out[n] = this[n]
    return out
}
internal fun Float32Array.toFloatArray(): FloatArray {
    val out = FloatArray(this.length)
    for (n in out.indices) out[n] = this[n]
    return out
}
internal fun Float64Array.toDoubleArray(): DoubleArray {
    val out = DoubleArray(this.length)
    for (n in out.indices) out[n] = this[n]
    return out
}

internal fun ByteArray.toInt8Array(): Int8Array {
    //val tout = this.asDynamic()
    //if (tout is Int8Array) {
    //    return tout.unsafeCast<Int8Array>()
    //} else {
    val out = Int8Array(this.size)
    for (n in 0 until out.length) out[n] = this[n]
    return out
    //}
}

internal fun ByteArray.toUint8Array(): Uint8Array {
    return Uint8Array(toInt8Array().buffer)
}
