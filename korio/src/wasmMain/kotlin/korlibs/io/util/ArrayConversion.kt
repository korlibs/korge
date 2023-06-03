package korlibs.io.util

import org.khronos.webgl.*

fun ArrayBuffer.toByteArray(): ByteArray = Int8Array(this).toByteArray()
fun Uint8Array.toByteArray(): ByteArray {
    return Int8Array(this.buffer).toByteArray()
}
fun Int8Array.toByteArray(): ByteArray {
    //val tout = this.asDynamic()
    //if (tout is ByteArray) {
    //    return tout.unsafeCast<ByteArray>()
    //} else {
        val out = ByteArray(this.length)
        for (n in out.indices) out[n] = this[n]
        return out
    //}
}

fun ByteArray.toInt8Array(): Int8Array {
    //val tout = this.asDynamic()
    //if (tout is Int8Array) {
    //    return tout.unsafeCast<Int8Array>()
    //} else {
        val out = Int8Array(this.size)
        for (n in 0 until out.length) out[n] = this[n]
        return out
    //}
}

fun ByteArray.toUint8Array(): Uint8Array {
    return Uint8Array(toInt8Array().buffer)
}

fun Int32Array.toIntArray(): IntArray {
    //val tout = this.asDynamic()
    //if (tout is ByteArray) {
    //    return tout.unsafeCast<ByteArray>()
    //} else {
    val out = IntArray(this.length)
    for (n in out.indices) out[n] = this[n]
    return out
    //}
}

inline fun <T> Int32Array.useIntArray(block: (IntArray) -> T): T {
    val buffer = this
    val array = IntArray(buffer.length)
    for (n in 0 until array.size) array[n] = buffer[n]
    try {
        return block(array)
    } finally {
        for (n in 0 until array.size) buffer[n] = array[n]
    }
}

public fun arraycopy(src: Int32Array, srcPos: Int, dst: IntArray, dstPos: Int, size: Int) {
    for (n in 0 until size) dst[dstPos + n] = src[srcPos + n]
}
public fun arraycopy(src: IntArray, srcPos: Int, dst: Int32Array, dstPos: Int, size: Int) {
    for (n in 0 until size) dst[dstPos + n] = src[srcPos + n]
}
