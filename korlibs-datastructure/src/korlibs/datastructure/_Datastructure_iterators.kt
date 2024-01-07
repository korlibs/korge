@file:Suppress("PackageDirectoryMismatch")

package korlibs.datastructure.iterators

import korlibs.datastructure.DoubleArrayList
import korlibs.datastructure.FastArrayList
import korlibs.datastructure.FloatArrayList
import korlibs.datastructure.IntArrayList
import korlibs.datastructure.toFastList


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

inline fun <T> FastArrayList<T>.fastForEachWithTemp(temp: FastArrayList<T>, callback: (T) -> Unit) {
    this.toFastList(temp)
    try {
        temp.fastForEach(callback)
    } finally {
        temp.clear()
    }
}

inline fun <T> List<T>.fastForEachWithTemp(temp: FastArrayList<T>, callback: (T) -> Unit) {
    this.toFastList(temp)
    try {
        temp.fastForEach(callback)
    } finally {
        temp.clear()
    }
}

inline fun <T> List<T>.fastForEach(callback: (T) -> Unit) {
	var n = 0
	while (n < size) callback(this[n++])
}

inline fun <T> Array<T>.fastForEach(callback: (T) -> Unit) {
	var n = 0
	while (n < size) callback(this[n++])
}

inline fun IntArray.fastForEach(callback: (Int) -> Unit) {
    var n = 0
    while (n < size) callback(this[n++])
}

inline fun FloatArray.fastForEach(callback: (Float) -> Unit) {
    var n = 0
    while (n < size) callback(this[n++])
}

inline fun DoubleArray.fastForEach(callback: (Double) -> Unit) {
    var n = 0
    while (n < size) callback(this[n++])
}

inline fun IntArrayList.fastForEach(callback: (Int) -> Unit) {
    var n = 0
    while (n < size) {
        callback(this.getAt(n++))
    }
}

inline fun FloatArrayList.fastForEach(callback: (Float) -> Unit) {
    var n = 0
    while (n < size) {
        callback(this.getAt(n++))
    }
}

inline fun DoubleArrayList.fastForEach(callback: (Double) -> Unit) {
    var n = 0
    while (n < size) {
        callback(this.getAt(n++))
    }
}

inline fun <T> List<T>.fastForEachWithIndex(callback: (index: Int, value: T) -> Unit) {
	var n = 0
	while (n < size) {
		callback(n, this[n])
		n++
	}
}

inline fun <T> Array<T>.fastForEachWithIndex(callback: (index: Int, value: T) -> Unit) {
	var n = 0
	while (n < size) {
		callback(n, this[n])
		n++
	}
}

inline fun IntArrayList.fastForEachWithIndex(callback: (index: Int, value: Int) -> Unit) {
    var n = 0
    while (n < size) {
        callback(n, this.getAt(n))
        n++
    }
}

inline fun FloatArrayList.fastForEachWithIndex(callback: (index: Int, value: Float) -> Unit) {
    var n = 0
    while (n < size) {
        callback(n, this.getAt(n))
        n++
    }
}

inline fun DoubleArrayList.fastForEachWithIndex(callback: (index: Int, value: Double) -> Unit) {
    var n = 0
    while (n < size) {
        callback(n, this.getAt(n))
        n++
    }
}

inline fun <T> List<T>.fastForEachReverse(callback: (T) -> Unit) {
	var n = 0
	while (n < size) {
		callback(this[size - n - 1])
		n++
	}
}

inline fun <T> MutableList<T>.fastIterateRemove(callback: (T) -> Boolean): MutableList<T> {
	var n = 0
	var m = 0
	while (n < size) {
		if (m >= 0 && m != n) this[m] = this[n]
		if (callback(this[n])) m--
		n++
		m++
	}
	while (this.size > m) this.removeAt(this.size - 1)
	return this
}
