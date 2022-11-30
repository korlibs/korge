package com.soywiz.kmem

/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public fun <T> arraycopy(src: Array<out T>, srcPos: Int, dst: Array<out T>, dstPos: Int, size: Int) {
	src.copyInto(dst as Array<T>, dstPos, srcPos, srcPos + size)
}

/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public fun arraycopy(src: BooleanArray, srcPos: Int, dst: BooleanArray, dstPos: Int, size: Int) {
	src.copyInto(dst, dstPos, srcPos, srcPos + size)
}

/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public fun arraycopy(src: LongArray, srcPos: Int, dst: LongArray, dstPos: Int, size: Int) {
	src.copyInto(dst, dstPos, srcPos, srcPos + size)
}

/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public fun arraycopy(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, size: Int) {
	src.copyInto(dst, dstPos, srcPos, srcPos + size)
}

/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public fun arraycopy(src: ShortArray, srcPos: Int, dst: ShortArray, dstPos: Int, size: Int) {
	src.copyInto(dst, dstPos, srcPos, srcPos + size)
}

/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public fun arraycopy(src: CharArray, srcPos: Int, dst: CharArray, dstPos: Int, size: Int) {
	src.copyInto(dst, dstPos, srcPos, srcPos + size)
}

/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public fun arraycopy(src: IntArray, srcPos: Int, dst: IntArray, dstPos: Int, size: Int) {
	src.copyInto(dst, dstPos, srcPos, srcPos + size)
}

/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public fun arraycopy(src: FloatArray, srcPos: Int, dst: FloatArray, dstPos: Int, size: Int) {
	src.copyInto(dst, dstPos, srcPos, srcPos + size)
}

/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public fun arraycopy(src: DoubleArray, srcPos: Int, dst: DoubleArray, dstPos: Int, size: Int) {
	src.copyInto(dst, dstPos, srcPos, srcPos + size)
}

/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public fun <T> arraycopy(src: List<T>, srcPos: Int, dst: MutableList<T>, dstPos: Int, size: Int) {
    if (src === dst) error("Not supporting the same array")
    for (n in 0 until size) {
        dst[dstPos + n] = src[srcPos]
    }
}

public inline fun <T> arraycopy(size: Int, src: Any?, srcPos: Int, dst: Any?, dstPos: Int, setDst: (Int, T) -> Unit, getSrc: (Int) -> T) {
    val overlapping = src === dst && dstPos > srcPos
    if (overlapping) {
        var n = size
        while (--n >= 0) setDst(dstPos + n, getSrc(srcPos + n))
    } else {
        for (n in 0 until size) setDst(dstPos + n, getSrc(srcPos + n))
    }
}

// NBuffer variants
fun arraycopy(src: NBuffer, srcPos: Int, dst: NBuffer, dstPos: Int, size: Int) {
    NBuffer.copy(src, srcPos, dst, dstPos, size)
}
fun arraycopy(src: NBufferUInt8, srcPos: Int, dst: NBufferUInt8, dstPos: Int, size: Int) = arraycopy(src.buffer, srcPos * 1, dst.buffer, dstPos * 1, size * 1)
fun arraycopy(src: NBufferUInt8, srcPos: Int, dst: UByteArrayInt, dstPos: Int, size: Int) { src.getArray(srcPos, dst, dstPos, size) }
fun arraycopy(src: UByteArrayInt, srcPos: Int, dst: NBufferUInt8, dstPos: Int, size: Int) { dst.setArray(dstPos, src, srcPos, size) }

fun arraycopy(src: NBufferUInt16, srcPos: Int, dst: NBufferUInt16, dstPos: Int, size: Int) = arraycopy(src.buffer, srcPos * 2, dst.buffer, dstPos * 2, size * 2)
fun arraycopy(src: NBufferUInt16, srcPos: Int, dst: UShortArrayInt, dstPos: Int, size: Int) { src.getArray(srcPos, dst, dstPos, size) }
fun arraycopy(src: UShortArrayInt, srcPos: Int, dst: NBufferUInt16, dstPos: Int, size: Int) { dst.setArray(dstPos, src, srcPos, size) }

fun arraycopy(src: NBufferInt8, srcPos: Int, dst: NBufferInt8, dstPos: Int, size: Int) = arraycopy(src.buffer, srcPos * 1, dst.buffer, dstPos * 1, size * 1)
fun arraycopy(src: NBufferInt8, srcPos: Int, dst: ByteArray, dstPos: Int, size: Int) { src.getArray(srcPos, dst, dstPos, size) }
fun arraycopy(src: ByteArray, srcPos: Int, dst: NBufferInt8, dstPos: Int, size: Int) { dst.setArray(dstPos, src, srcPos, size) }

fun arraycopy(src: NBufferInt16, srcPos: Int, dst: NBufferInt16, dstPos: Int, size: Int) = arraycopy(src.buffer, srcPos * 2, dst.buffer, dstPos * 2, size * 2)
fun arraycopy(src: NBufferInt16, srcPos: Int, dst: ShortArray, dstPos: Int, size: Int) { src.getArray(srcPos, dst, dstPos, size) }
fun arraycopy(src: ShortArray, srcPos: Int, dst: NBufferInt16, dstPos: Int, size: Int) { dst.setArray(dstPos, src, srcPos, size) }

fun arraycopy(src: NBufferInt32, srcPos: Int, dst: NBufferInt32, dstPos: Int, size: Int) = arraycopy(src.buffer, srcPos * 4, dst.buffer, dstPos * 4, size * 4)
fun arraycopy(src: NBufferInt32, srcPos: Int, dst: IntArray, dstPos: Int, size: Int) { src.getArray(srcPos, dst, dstPos, size) }
fun arraycopy(src: IntArray, srcPos: Int, dst: NBufferInt32, dstPos: Int, size: Int) { dst.setArray(dstPos, src, srcPos, size) }

fun arraycopy(src: NBufferFloat32, srcPos: Int, dst: NBufferFloat32, dstPos: Int, size: Int) = arraycopy(src.buffer, srcPos * 4, dst.buffer, dstPos * 4, size * 4)
fun arraycopy(src: NBufferFloat32, srcPos: Int, dst: FloatArray, dstPos: Int, size: Int) { src.getArray(srcPos, dst, dstPos, size) }
fun arraycopy(src: FloatArray, srcPos: Int, dst: NBufferFloat32, dstPos: Int, size: Int) { dst.setArray(dstPos, src, srcPos, size) }

fun arraycopy(src: NBufferFloat64, srcPos: Int, dst: NBufferFloat64, dstPos: Int, size: Int) = arraycopy(src.buffer, srcPos * 8, dst.buffer, dstPos * 8, size * 8)
fun arraycopy(src: NBufferFloat64, srcPos: Int, dst: DoubleArray, dstPos: Int, size: Int) { src.getArray(srcPos, dst, dstPos, size) }
fun arraycopy(src: DoubleArray, srcPos: Int, dst: NBufferFloat64, dstPos: Int, size: Int) { dst.setArray(dstPos, src, srcPos, size) }

fun arraycopy(src: NBufferInt64, srcPos: Int, dst: NBufferInt64, dstPos: Int, size: Int) = arraycopy(src.buffer, srcPos * 8, dst.buffer, dstPos * 8, size * 8)
fun arraycopy(src: NBufferInt64, srcPos: Int, dst: LongArray, dstPos: Int, size: Int) { src.getArray(srcPos, dst, dstPos, size) }
fun arraycopy(src: LongArray, srcPos: Int, dst: NBufferInt64, dstPos: Int, size: Int) { dst.setArray(dstPos, src, srcPos, size) }
