package com.soywiz.kmem

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
fun arrayequal(src: Buffer, srcPos: Int, dst: Buffer, dstPos: Int, size: Int): Boolean = _arrayequal(srcPos, dstPos, size) { s, d ->
    //println("src.getInt8($s) == dst.getInt8($d) : ${src.getInt8(s)} == ${dst.getInt8(d)}")
    src.getInt8(s) == dst.getInt8(d)
}
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
