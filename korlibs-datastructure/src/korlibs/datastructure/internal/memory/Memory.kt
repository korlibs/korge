package korlibs.datastructure.internal.memory

internal object Memory {

    /** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
    fun <T> arraycopy(src: Array<out T>, srcPos: Int, dst: Array<out T>, dstPos: Int, size: Int) {
        src.copyInto(dst as Array<T>, dstPos, srcPos, srcPos + size)
    }

    /** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
    fun arraycopy(src: BooleanArray, srcPos: Int, dst: BooleanArray, dstPos: Int, size: Int) {
        src.copyInto(dst, dstPos, srcPos, srcPos + size)
    }

    /** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
    fun arraycopy(src: LongArray, srcPos: Int, dst: LongArray, dstPos: Int, size: Int) {
        src.copyInto(dst, dstPos, srcPos, srcPos + size)
    }

    /** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
    fun arraycopy(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, size: Int) {
        src.copyInto(dst, dstPos, srcPos, srcPos + size)
    }

    /** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
    fun arraycopy(src: ShortArray, srcPos: Int, dst: ShortArray, dstPos: Int, size: Int) {
        src.copyInto(dst, dstPos, srcPos, srcPos + size)
    }

    /** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
    fun arraycopy(src: CharArray, srcPos: Int, dst: CharArray, dstPos: Int, size: Int) {
        src.copyInto(dst, dstPos, srcPos, srcPos + size)
    }

    /** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
    fun arraycopy(src: IntArray, srcPos: Int, dst: IntArray, dstPos: Int, size: Int) {
        src.copyInto(dst, dstPos, srcPos, srcPos + size)
    }

    /** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
    fun arraycopy(src: FloatArray, srcPos: Int, dst: FloatArray, dstPos: Int, size: Int) {
        src.copyInto(dst, dstPos, srcPos, srcPos + size)
    }

    /** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
    fun arraycopy(src: DoubleArray, srcPos: Int, dst: DoubleArray, dstPos: Int, size: Int) {
        src.copyInto(dst, dstPos, srcPos, srcPos + size)
    }

    /** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
    fun <T> arraycopy(src: List<T>, srcPos: Int, dst: MutableList<T>, dstPos: Int, size: Int) {
        if (src === dst) error("Not supporting the same array")
        for (n in 0 until size) {
            dst[dstPos + n] = src[srcPos]
        }
    }

    inline fun <T> arraycopy(
        size: Int,
        src: Any?,
        srcPos: Int,
        dst: Any?,
        dstPos: Int,
        setDst: (Int, T) -> Unit,
        getSrc: (Int) -> T
    ) {
        val overlapping = src === dst && dstPos > srcPos
        if (overlapping) {
            var n = size
            while (--n >= 0) setDst(dstPos + n, getSrc(srcPos + n))
        } else {
            for (n in 0 until size) setDst(dstPos + n, getSrc(srcPos + n))
        }
    }


    /** Returns the number of leading zeros of the bits of [this] integer */
    inline fun Int.countLeadingZeros(): Int = this.countLeadingZeroBits()
}
