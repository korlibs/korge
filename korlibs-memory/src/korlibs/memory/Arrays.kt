package korlibs.memory

private inline fun _arraycmp(srcPos: Int, dstPos: Int, size: Int, cmp: (Int, Int) -> Int): Int {
    for (n in 0 until size) {
        cmp(srcPos + n, dstPos + n).also { if (it != 0) return it }
    }
    return 0
}
private inline fun _arrayequal(srcPos: Int, dstPos: Int, size: Int, cmp: (Int, Int) -> Boolean): Boolean {
    for (n in 0 until size) {
        if (!cmp(srcPos + n, dstPos + n)) {
            //println("Failed at $n : ${srcPos + n}, ${dstPos + n}")
            return false
        }
    }
    return true
}

public fun arrayfill(array: Buffer, value: Int, start: Int = 0, end: Int = array.size): Unit {
    for (n in start until end) array.setUInt8(n, value)
}

fun <T> arrayequal(src: Array<T>, srcPos: Int, dst: Array<T>, dstPos: Int, size: Int): Boolean = _arrayequal(srcPos, dstPos, size) { s, d -> src[s] == dst[d]}
fun <T> arrayequal(src: List<T>, srcPos: Int, dst: List<T>, dstPos: Int, size: Int): Boolean = _arrayequal(srcPos, dstPos, size) { s, d -> src[s] == dst[d]}
fun arrayequal(src: Buffer, srcPos: Int, dst: Buffer, dstPos: Int, size: Int): Boolean = Buffer.equals(src, srcPos, dst, dstPos, size)
fun arrayequal(src: BooleanArray, srcPos: Int, dst: BooleanArray, dstPos: Int, size: Int): Boolean = _arrayequal(srcPos, dstPos, size) { s, d -> src[s] == dst[d]}
fun arrayequal(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, size: Int): Boolean = _arrayequal(srcPos, dstPos, size) { s, d -> src[s] == dst[d]}
fun arrayequal(src: ShortArray, srcPos: Int, dst: ShortArray, dstPos: Int, size: Int): Boolean = _arrayequal(srcPos, dstPos, size) { s, d -> src[s] == dst[d]}
fun arrayequal(src: CharArray, srcPos: Int, dst: CharArray, dstPos: Int, size: Int): Boolean = _arrayequal(srcPos, dstPos, size) { s, d -> src[s] == dst[d]}
fun arrayequal(src: IntArray, srcPos: Int, dst: IntArray, dstPos: Int, size: Int): Boolean = _arrayequal(srcPos, dstPos, size) { s, d -> src[s] == dst[d]}
fun arrayequal(src: LongArray, srcPos: Int, dst: LongArray, dstPos: Int, size: Int): Boolean = _arrayequal(srcPos, dstPos, size) { s, d -> src[s] == dst[d]}
fun arrayequal(src: FloatArray, srcPos: Int, dst: FloatArray, dstPos: Int, size: Int): Boolean = _arrayequal(srcPos, dstPos, size) { s, d -> src[s] == dst[d]}
fun arrayequal(src: DoubleArray, srcPos: Int, dst: DoubleArray, dstPos: Int, size: Int): Boolean = _arrayequal(srcPos, dstPos, size) { s, d -> src[s] == dst[d]}

fun <T : Comparable<T>> arraycmp(src: Array<T>, srcPos: Int, dst: Array<T>, dstPos: Int, size: Int): Int = _arraycmp(srcPos, dstPos, size) { s, d -> src[s] compareTo dst[d]}
fun <T : Comparable<T>> arraycmp(src: List<T>, srcPos: Int, dst: List<T>, dstPos: Int, size: Int): Int = _arraycmp(srcPos, dstPos, size) { s, d -> src[s] compareTo dst[d]}
fun arraycmp(src: Buffer, srcPos: Int, dst: Buffer, dstPos: Int, size: Int): Int = _arraycmp(srcPos, dstPos, size) { s, d -> src.getInt8(s) compareTo dst.getInt8(d) }
fun arraycmp(src: BooleanArray, srcPos: Int, dst: BooleanArray, dstPos: Int, size: Int): Int = _arraycmp(srcPos, dstPos, size) { s, d -> src[s] compareTo dst[d]}
fun arraycmp(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, size: Int): Int = _arraycmp(srcPos, dstPos, size) { s, d -> src[s] compareTo dst[d]}
fun arraycmp(src: ShortArray, srcPos: Int, dst: ShortArray, dstPos: Int, size: Int): Int = _arraycmp(srcPos, dstPos, size) { s, d -> src[s] compareTo dst[d]}
fun arraycmp(src: CharArray, srcPos: Int, dst: CharArray, dstPos: Int, size: Int): Int = _arraycmp(srcPos, dstPos, size) { s, d -> src[s] compareTo dst[d]}
fun arraycmp(src: IntArray, srcPos: Int, dst: IntArray, dstPos: Int, size: Int): Int = _arraycmp(srcPos, dstPos, size) { s, d -> src[s] compareTo dst[d]}
fun arraycmp(src: LongArray, srcPos: Int, dst: LongArray, dstPos: Int, size: Int): Int = _arraycmp(srcPos, dstPos, size) { s, d -> src[s] compareTo dst[d]}
fun arraycmp(src: FloatArray, srcPos: Int, dst: FloatArray, dstPos: Int, size: Int): Int = _arraycmp(srcPos, dstPos, size) { s, d -> src[s] compareTo dst[d]}
fun arraycmp(src: DoubleArray, srcPos: Int, dst: DoubleArray, dstPos: Int, size: Int): Int = _arraycmp(srcPos, dstPos, size) { s, d -> src[s] compareTo dst[d]}

public fun arrayadd(array: ByteArray, value: Byte, start: Int = 0, end: Int = array.size) { for (n in start until end) array[n] = (array[n] + value).toByte() }
public fun arrayadd(array: ShortArray, value: Short, start: Int = 0, end: Int = array.size) { for (n in start until end) array[n] = (array[n] + value).toShort() }
public fun arrayadd(array: IntArray, value: Int, start: Int = 0, end: Int = array.size) { for (n in start until end) array[n] = array[n] + value }
public fun arrayadd(array: LongArray, value: Long, start: Int = 0, end: Int = array.size) { for (n in start until end) array[n] = array[n] + value }
public fun arrayadd(array: FloatArray, value: Float, start: Int = 0, end: Int = array.size) { for (n in start until end) array[n] = array[n] + value }
public fun arrayadd(array: DoubleArray, value: Double, start: Int = 0, end: Int = array.size) { for (n in start until end) array[n] = array[n] + value }

public fun arrayadd(array: Uint8Buffer, value: Byte, start: Int = 0, end: Int = array.size) { for (n in start until end) array[n] = (array[n] + value) }
public fun arrayadd(array: Uint16Buffer, value: Short, start: Int = 0, end: Int = array.size) { for (n in start until end) array[n] = (array[n] + value) }
public fun arrayadd(array: Int8Buffer, value: Byte, start: Int = 0, end: Int = array.size) { for (n in start until end) array[n] = (array[n] + value).toByte() }
public fun arrayadd(array: Int16Buffer, value: Short, start: Int = 0, end: Int = array.size) { for (n in start until end) array[n] = (array[n] + value).toShort() }
public fun arrayadd(array: Int32Buffer, value: Int, start: Int = 0, end: Int = array.size) { for (n in start until end) array[n] = array[n] + value }
public fun arrayadd(array: Float32Buffer, value: Float, start: Int = 0, end: Int = array.size) { for (n in start until end) array[n] = array[n] + value }
public fun arrayadd(array: Float64Buffer, value: Double, start: Int = 0, end: Int = array.size) { for (n in start until end) array[n] = array[n] + value }


/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
public fun <T> arrayfill(array: Array<T>, value: T, start: Int = 0, end: Int = array.size): Unit = array.fill(value, start, end)
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
public fun arrayfill(array: BooleanArray, value: Boolean, start: Int = 0, end: Int = array.size): Unit = array.fill(value, start, end)
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
public fun arrayfill(array: LongArray, value: Long, start: Int = 0, end: Int = array.size): Unit = array.fill(value, start, end)
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
public fun arrayfill(array: ByteArray, value: Byte, start: Int = 0, end: Int = array.size): Unit = array.fill(value, start, end)
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
public fun arrayfill(array: ShortArray, value: Short, start: Int = 0, end: Int = array.size): Unit = array.fill(value, start, end)
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
public fun arrayfill(array: IntArray, value: Int, start: Int = 0, end: Int = array.size): Unit = array.fill(value, start, end)
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
public fun arrayfill(array: FloatArray, value: Float, start: Int = 0, end: Int = array.size): Unit = array.fill(value, start, end)
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
public fun arrayfill(array: DoubleArray, value: Double, start: Int = 0, end: Int = array.size): Unit = array.fill(value, start, end)

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

@PublishedApi
internal inline fun array_indexOf(starting: Int, selfSize: Int, subSize: Int, crossinline equal: (n: Int, m: Int) -> Boolean): Int {
    for (n in starting until selfSize - subSize) {
        var eq = 0
        for (m in 0 until subSize) {
            if (!equal(n + m, m)) {
                break
            }
            eq++
        }
        if (eq == subSize) {
            return n
        }
    }
    return -1
}

@PublishedApi
internal inline fun array_lastIndexOf(starting: Int, selfSize: Int, subSize: Int, crossinline equal: (n: Int, m: Int) -> Boolean): Int {
    for (n in (selfSize - subSize - 1) downTo starting) {
        var eq = 0
        for (m in 0 until subSize) {
            if (!equal(n + m, m)) {
                break
            }
            eq++
        }
        if (eq == subSize) {
            return n
        }
    }
    return -1
}

public fun BooleanArray.indexOf(sub: BooleanArray, starting: Int = 0): Int = array_indexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }
public fun ByteArray.indexOf(sub: ByteArray, starting: Int = 0): Int = array_indexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }
public fun ShortArray.indexOf(sub: ShortArray, starting: Int = 0): Int = array_indexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }
public fun CharArray.indexOf(sub: CharArray, starting: Int = 0): Int = array_indexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }
public fun IntArray.indexOf(sub: IntArray, starting: Int = 0): Int = array_indexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }
public fun LongArray.indexOf(sub: LongArray, starting: Int = 0): Int = array_indexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }
public fun FloatArray.indexOf(sub: FloatArray, starting: Int = 0): Int = array_indexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }
public fun DoubleArray.indexOf(sub: DoubleArray, starting: Int = 0): Int = array_indexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }
public fun <T> Array<T>.indexOf(sub: Array<T>, starting: Int = 0): Int = array_indexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }

public fun BooleanArray.lastIndexOf(sub: BooleanArray, starting: Int = 0): Int = array_lastIndexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }
public fun ByteArray.lastIndexOf(sub: ByteArray, starting: Int = 0): Int = array_lastIndexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }
public fun ShortArray.lastIndexOf(sub: ShortArray, starting: Int = 0): Int = array_lastIndexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }
public fun CharArray.lastIndexOf(sub: CharArray, starting: Int = 0): Int = array_lastIndexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }
public fun IntArray.lastIndexOf(sub: IntArray, starting: Int = 0): Int = array_lastIndexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }
public fun LongArray.lastIndexOf(sub: LongArray, starting: Int = 0): Int = array_lastIndexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }
public fun FloatArray.lastIndexOf(sub: FloatArray, starting: Int = 0): Int = array_lastIndexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }
public fun DoubleArray.lastIndexOf(sub: DoubleArray, starting: Int = 0): Int = array_lastIndexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }
public fun <T> Array<T>.lastIndexOf(sub: Array<T>, starting: Int = 0): Int = array_lastIndexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }

public inline fun <T> arraycopyStride(src: (Int) -> T, srcPos: Int, srcStride: Int, dst: (Int, T) -> Unit, dstPos: Int, dstStride: Int, size: Int) {
    for (n in 0 until size) dst(dstPos + dstStride * n, src(srcPos + srcStride * n))
}

public fun arraycopyStride(src: ByteArray, srcPos: Int, srcStride: Int, dst: ByteArray, dstPos: Int, dstStride: Int, size: Int) {
    for (n in 0 until size) dst[dstPos + dstStride * n] = src[srcPos + srcStride * n]
}

public fun arraycopyStride(src: ShortArray, srcPos: Int, srcStride: Int, dst: ShortArray, dstPos: Int, dstStride: Int, size: Int) {
    for (n in 0 until size) dst[dstPos + dstStride * n] = src[srcPos + srcStride * n]
}

public fun arraycopyStride(src: IntArray, srcPos: Int, srcStride: Int, dst: IntArray, dstPos: Int, dstStride: Int, size: Int) {
    for (n in 0 until size) dst[dstPos + dstStride * n] = src[srcPos + srcStride * n]
}

public fun arraycopyStride(src: FloatArray, srcPos: Int, srcStride: Int, dst: FloatArray, dstPos: Int, dstStride: Int, size: Int) {
    for (n in 0 until size) dst[dstPos + dstStride * n] = src[srcPos + srcStride * n]
}

public fun arrayinterleave(
    out: ByteArray, outPos: Int,
    array1: ByteArray, array1Pos: Int,
    array2: ByteArray, array2Pos: Int,
    size: Int,
) {
    var m = outPos
    for (n in 0 until size) {
        out[m++] = array1[array1Pos + n]
        out[m++] = array2[array2Pos + n]
    }
}

public fun arrayinterleave(
    out: ShortArray, outPos: Int,
    array1: ShortArray, array1Pos: Int,
    array2: ShortArray, array2Pos: Int,
    size: Int,
) {
    var m = outPos
    for (n in 0 until size) {
        out[m++] = array1[array1Pos + n]
        out[m++] = array2[array2Pos + n]
    }
}

public fun arrayinterleave(
    out: IntArray, outPos: Int,
    array1: IntArray, array1Pos: Int,
    array2: IntArray, array2Pos: Int,
    size: Int,
) {
    var m = outPos
    for (n in 0 until size) {
        out[m++] = array1[array1Pos + n]
        out[m++] = array2[array2Pos + n]
    }
}


public fun arrayinterleave(
    out: FloatArray, outPos: Int,
    array1: FloatArray, array1Pos: Int,
    array2: FloatArray, array2Pos: Int,
    size: Int,
) {
    var m = outPos
    for (n in 0 until size) {
        out[m++] = array1[array1Pos + n]
        out[m++] = array2[array2Pos + n]
    }
}

/** View of [bytes] [ByteArray] reinterpreted as [Int] */
public inline class UByteArrayInt(public val data: ByteArray) {
    val bytes: ByteArray get() = data

    /** Creates a new [UByteArrayInt] view of [size] bytes */
    constructor(size: Int) : this(ByteArray(size))
    constructor(size: Int, gen: (Int) -> Int) : this(ByteArray(size) { gen(it).toByte() })

    public val size: Int get() = data.size
    public operator fun get(index: Int): Int = data[index].toInt() and 0xFF
    public operator fun set(index: Int, value: Int) { data[index] = value.toByte() }
    public operator fun set(index: Int, value: UByte) { data[index] = value.toByte() }

    fun fill(value: Int, fromIndex: Int = 0, toIndex: Int = size) = data.fill(value.toByte(), fromIndex, toIndex)
}
/** Creates a view of [this] reinterpreted as [Int] */
public fun ByteArray.asUByteArrayInt(): UByteArrayInt = UByteArrayInt(this)
/** Gets the underlying array of [this] */
public fun UByteArrayInt.asByteArray(): ByteArray = this.data
fun arraycopy(src: UByteArrayInt, srcPos: Int, dst: UByteArrayInt, dstPos: Int, size: Int) = arraycopy(src.data, srcPos, dst.data, dstPos, size)
fun ubyteArrayIntOf(vararg values: Int): UByteArrayInt = UByteArrayInt(values.size) { values[it] }



/** View of [shorts] [ShortArray] reinterpreted as [Int] */
public inline class UShortArrayInt(public val data: ShortArray) {
    val shorts: ShortArray get() = data

    /** Creates a new [UShortArrayInt] view of [size] bytes */
    constructor(size: Int) : this(ShortArray(size))
    constructor(size: Int, gen: (Int) -> Int) : this(ShortArray(size) { gen(it).toShort() })

    public val size: Int get() = data.size
    public operator fun get(index: Int): Int = data[index].toInt() and 0xFFFF
    public operator fun set(index: Int, value: Int) { data[index] = value.toShort() }
    public operator fun set(index: Int, value: UShort) { data[index] = value.toShort() }

    fun fill(value: Int, fromIndex: Int = 0, toIndex: Int = size) = this.data.fill(value.toShort(), fromIndex, toIndex)
}
/** Creates a view of [this] reinterpreted as [Int] */
public fun ShortArray.asUShortArrayInt(): UShortArrayInt = UShortArrayInt(this)
/** Gets the underlying array of [this] */
public fun UShortArrayInt.asShortArray(): ShortArray = this.data
fun arraycopy(src: UShortArrayInt, srcPos: Int, dst: UShortArrayInt, dstPos: Int, size: Int) = arraycopy(src.data, srcPos, dst.data, dstPos, size)
fun ushortArrayIntOf(vararg values: Int): UShortArrayInt = UShortArrayInt(values.size) { values[it] }




/** View of [base] [IntArray] reinterpreted as [Float] */
public inline class FloatArrayFromIntArray(public val base: IntArray) {
    public operator fun get(i: Int): Float = base[i].reinterpretAsFloat()
    public operator fun set(i: Int, v: Float) { base[i] = v.reinterpretAsInt() }
}

/** Creates a view of [this] reinterpreted as [Float] */
public fun IntArray.asFloatArray(): FloatArrayFromIntArray = FloatArrayFromIntArray(this)
/** Gets the underlying array of [this] */
public fun FloatArrayFromIntArray.asIntArray(): IntArray = base

internal inline fun <T> getSampledGeneric(index: Float, get: (Int) -> T, scale: (T, Float) -> Float, convert: (Float) -> T, unit: Unit = Unit): T {
    val index0 = index.toInt()
    val v0 = get(index0)
    if (index0.toFloat() == index) return v0
    val v1 = get(index0 + 1)
    val ratio = index % 1f
    val o0 = scale(v0, 1f - ratio)
    val o1 = scale(v1, ratio)
    return convert(o0 + o1)
}


fun ByteArray.getSampled(index: Float): Byte = getSampledGeneric(index, get = { this[it] }, scale = { value, scale -> value * scale }, convert = { it.toInt().toByte() })
fun UByteArray.getSampled(index: Float): UByte = getSampledGeneric(index, get = { this[it] }, scale = { value, scale -> value.toInt() * scale }, convert = { it.toInt().toUByte() })
fun ShortArray.getSampled(index: Float): Short = getSampledGeneric(index, get = { this[it] }, scale = { value, scale -> value * scale }, convert = { it.toInt().toShort() })
fun UShortArray.getSampled(index: Float): UShort = getSampledGeneric(index, get = { this[it] }, scale = { value, scale -> value.toInt() * scale }, convert = { it.toInt().toUShort() })
fun CharArray.getSampled(index: Float): Char = getSampledGeneric(index, get = { this[it] }, scale = { value, scale -> value.toInt() * scale }, convert = { it.toInt().toChar() })
fun FloatArray.getSampled(index: Float): Float = getSampledGeneric(index, get = { this[it] }, scale = { value, scale -> value * scale }, convert = { it })
