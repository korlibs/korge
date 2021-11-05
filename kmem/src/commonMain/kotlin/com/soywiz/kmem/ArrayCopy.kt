package com.soywiz.kmem

/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public fun <T> arraycopy(src: Array<out T>, srcPos: Int, dst: Array<out T>, dstPos: Int, size: Int): Unit {
	src.copyInto(dst as Array<T>, dstPos, srcPos, srcPos + size)
}

/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public fun arraycopy(src: BooleanArray, srcPos: Int, dst: BooleanArray, dstPos: Int, size: Int): Unit {
	src.copyInto(dst, dstPos, srcPos, srcPos + size)
}

/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public fun arraycopy(src: LongArray, srcPos: Int, dst: LongArray, dstPos: Int, size: Int): Unit {
	src.copyInto(dst, dstPos, srcPos, srcPos + size)
}

/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public fun arraycopy(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, size: Int): Unit {
	src.copyInto(dst, dstPos, srcPos, srcPos + size)
}

/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public fun arraycopy(src: ShortArray, srcPos: Int, dst: ShortArray, dstPos: Int, size: Int): Unit {
	src.copyInto(dst, dstPos, srcPos, srcPos + size)
}

/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public fun arraycopy(src: CharArray, srcPos: Int, dst: CharArray, dstPos: Int, size: Int): Unit {
	src.copyInto(dst, dstPos, srcPos, srcPos + size)
}

/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public fun arraycopy(src: IntArray, srcPos: Int, dst: IntArray, dstPos: Int, size: Int): Unit {
	src.copyInto(dst, dstPos, srcPos, srcPos + size)
}

/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public fun arraycopy(src: FloatArray, srcPos: Int, dst: FloatArray, dstPos: Int, size: Int): Unit {
	src.copyInto(dst, dstPos, srcPos, srcPos + size)
}

/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public fun arraycopy(src: DoubleArray, srcPos: Int, dst: DoubleArray, dstPos: Int, size: Int): Unit {
	src.copyInto(dst, dstPos, srcPos, srcPos + size)
}

/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public fun <T> arraycopy(src: List<T>, srcPos: Int, dst: MutableList<T>, dstPos: Int, size: Int): Unit {
    if (src === dst) error("Not supporting the same array")
    for (n in 0 until size) {
        dst[dstPos + n] = src[srcPos]
    }
}
