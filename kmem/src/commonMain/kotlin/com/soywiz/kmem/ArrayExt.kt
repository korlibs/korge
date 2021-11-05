package com.soywiz.kmem

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

public fun BooleanArray.indexOf(sub: BooleanArray, starting: Int = 0): Int = array_indexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }
public fun ByteArray.indexOf(sub: ByteArray, starting: Int = 0): Int = array_indexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }
public fun ShortArray.indexOf(sub: ShortArray, starting: Int = 0): Int = array_indexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }
public fun CharArray.indexOf(sub: CharArray, starting: Int = 0): Int = array_indexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }
public fun IntArray.indexOf(sub: IntArray, starting: Int = 0): Int = array_indexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }
public fun LongArray.indexOf(sub: LongArray, starting: Int = 0): Int = array_indexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }
public fun FloatArray.indexOf(sub: FloatArray, starting: Int = 0): Int = array_indexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }
public fun DoubleArray.indexOf(sub: DoubleArray, starting: Int = 0): Int = array_indexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }
public fun <T> Array<T>.indexOf(sub: Array<T>, starting: Int = 0): Int = array_indexOf(starting, size, sub.size) { n, m -> this[n] == sub[m] }
