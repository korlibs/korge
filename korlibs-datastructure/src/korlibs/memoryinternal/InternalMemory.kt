package korlibs.memoryinternal

internal object InternalMemory {
    /** Extracts 4 bits at [offset] from [this] [Int] */
    inline fun Int.extract4(offset: Int): Int = (this ushr offset) and 0b1111

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

    // Buffer variants
    fun arraycopy(src: Buffer, srcPos: Int, dst: ByteArray, dstPos: Int, size: Int) {
        src.transferBytes(srcPos, dst, dstPos, size, toArray = true)
    }
    fun arraycopy(src: ByteArray, srcPos: Int, dst: Buffer, dstPos: Int, size: Int) {
        dst.transferBytes(dstPos, src, srcPos, size, toArray = false)
    }
    fun arraycopy(src: Buffer, srcPos: Int, dst: Buffer, dstPos: Int, size: Int) {
        Buffer.copy(src, srcPos, dst, dstPos, size)
    }
    fun arraycopy(src: Uint8Buffer, srcPos: Int, dst: Uint8Buffer, dstPos: Int, size: Int) = arraycopy(src.buffer, srcPos * 1, dst.buffer, dstPos * 1, size * 1)
    fun arraycopy(src: Uint8Buffer, srcPos: Int, dst: UByteArrayInt, dstPos: Int, size: Int) { src.getArray(srcPos, dst, dstPos, size) }
    fun arraycopy(src: UByteArrayInt, srcPos: Int, dst: Uint8Buffer, dstPos: Int, size: Int) { dst.setArray(dstPos, src, srcPos, size) }

    fun arraycopy(src: Uint16Buffer, srcPos: Int, dst: Uint16Buffer, dstPos: Int, size: Int) = arraycopy(src.buffer, srcPos * 2, dst.buffer, dstPos * 2, size * 2)
    fun arraycopy(src: Uint16Buffer, srcPos: Int, dst: UShortArrayInt, dstPos: Int, size: Int) { src.getArray(srcPos, dst, dstPos, size) }
    fun arraycopy(src: UShortArrayInt, srcPos: Int, dst: Uint16Buffer, dstPos: Int, size: Int) { dst.setArray(dstPos, src, srcPos, size) }

    fun arraycopy(src: Int8Buffer, srcPos: Int, dst: Int8Buffer, dstPos: Int, size: Int) = arraycopy(src.buffer, srcPos * 1, dst.buffer, dstPos * 1, size * 1)
    fun arraycopy(src: Int8Buffer, srcPos: Int, dst: ByteArray, dstPos: Int, size: Int) { src.getArray(srcPos, dst, dstPos, size) }
    fun arraycopy(src: ByteArray, srcPos: Int, dst: Int8Buffer, dstPos: Int, size: Int) { dst.setArray(dstPos, src, srcPos, size) }

    fun arraycopy(src: Int16Buffer, srcPos: Int, dst: Int16Buffer, dstPos: Int, size: Int) = arraycopy(src.buffer, srcPos * 2, dst.buffer, dstPos * 2, size * 2)
    fun arraycopy(src: Int16Buffer, srcPos: Int, dst: ShortArray, dstPos: Int, size: Int) { src.getArray(srcPos, dst, dstPos, size) }
    fun arraycopy(src: ShortArray, srcPos: Int, dst: Int16Buffer, dstPos: Int, size: Int) { dst.setArray(dstPos, src, srcPos, size) }

    fun arraycopy(src: Int32Buffer, srcPos: Int, dst: Int32Buffer, dstPos: Int, size: Int) = arraycopy(src.buffer, srcPos * 4, dst.buffer, dstPos * 4, size * 4)
    fun arraycopy(src: Int32Buffer, srcPos: Int, dst: IntArray, dstPos: Int, size: Int) { src.getArray(srcPos, dst, dstPos, size) }
    fun arraycopy(src: IntArray, srcPos: Int, dst: Int32Buffer, dstPos: Int, size: Int) { dst.setArray(dstPos, src, srcPos, size) }

    fun arraycopy(src: Float32Buffer, srcPos: Int, dst: Float32Buffer, dstPos: Int, size: Int) = arraycopy(src.buffer, srcPos * 4, dst.buffer, dstPos * 4, size * 4)
    fun arraycopy(src: Float32Buffer, srcPos: Int, dst: FloatArray, dstPos: Int, size: Int) { src.getArray(srcPos, dst, dstPos, size) }
    fun arraycopy(src: FloatArray, srcPos: Int, dst: Float32Buffer, dstPos: Int, size: Int) { dst.setArray(dstPos, src, srcPos, size) }

    fun arraycopy(src: Float64Buffer, srcPos: Int, dst: Float64Buffer, dstPos: Int, size: Int) = arraycopy(src.buffer, srcPos * 8, dst.buffer, dstPos * 8, size * 8)
    fun arraycopy(src: Float64Buffer, srcPos: Int, dst: DoubleArray, dstPos: Int, size: Int) { src.getArray(srcPos, dst, dstPos, size) }
    fun arraycopy(src: DoubleArray, srcPos: Int, dst: Float64Buffer, dstPos: Int, size: Int) { dst.setArray(dstPos, src, srcPos, size) }

    fun arraycopy(src: Int64Buffer, srcPos: Int, dst: Int64Buffer, dstPos: Int, size: Int) = arraycopy(src.buffer, srcPos * 8, dst.buffer, dstPos * 8, size * 8)
    fun arraycopy(src: Int64Buffer, srcPos: Int, dst: LongArray, dstPos: Int, size: Int) { src.getArray(srcPos, dst, dstPos, size) }
    fun arraycopy(src: LongArray, srcPos: Int, dst: Int64Buffer, dstPos: Int, size: Int) { dst.setArray(dstPos, src, srcPos, size) }


    /** Returns the number of leading zeros of the bits of [this] integer */
    inline fun Int.countLeadingZeros(): Int = this.countLeadingZeroBits()
}
