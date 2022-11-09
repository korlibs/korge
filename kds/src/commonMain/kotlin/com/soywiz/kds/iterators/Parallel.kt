package com.soywiz.kds.iterators

import com.soywiz.kds.*
import com.soywiz.kds.lock.*

expect val CONCURRENCY_COUNT: Int

expect inline fun parallelForeach(count: Int, crossinline block: (n: Int) -> Unit): Unit

@Suppress("UNCHECKED_CAST")
inline fun <T, reified R> List<T>.parallelMap(crossinline transform: (T) -> R): List<R> = arrayOfNulls<R>(size).also { out ->
    parallelForeach(size) { out[it] = transform(this[it]) }
}.toList() as List<R>

inline fun <T> List<T>.parallelMapInt(crossinline transform: (T) -> Int): IntArray = IntArray(size).also { out ->
    parallelForeach(size) { out[it] = transform(this[it]) }
}

inline fun IntArray.parallelMapInt(crossinline transform: (Int) -> Int): IntArray = IntArray(size).also { out ->
    parallelForeach(size) { out[it] = transform(this[it]) }
}

inline fun IntArrayList.parallelMapInt(crossinline transform: (Int) -> Int): IntArray = IntArray(size).also { out ->
    parallelForeach(size) { out[it] = transform(this[it]) }
}

inline fun IntRange.parallelMapInt(crossinline transform: (Int) -> Int): IntArray {
    val size = ((this.last - this.first) + 1) / step
    return IntArray(size.coerceAtLeast(0)).also { out ->
        parallelForeach(size) {
            out[it] = transform(this.first + this.step * it)
        }
    }
}
