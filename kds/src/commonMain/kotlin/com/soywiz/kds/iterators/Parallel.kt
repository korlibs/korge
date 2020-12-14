package com.soywiz.kds.iterators

expect val CONCURRENCY_COUNT: Int

expect inline fun parallelForeach(count: Int, crossinline block: (n: Int) -> Unit): Unit

inline fun <T, reified R> List<T>.parallelMap(crossinline transform: (T) -> R): List<R> {
    val out = arrayOfNulls<R>(size)
    parallelForeach(size) {
        out[it] = transform(this[it])
    }
    @Suppress("UNCHECKED_CAST")
    return out.toList() as List<R>
}
