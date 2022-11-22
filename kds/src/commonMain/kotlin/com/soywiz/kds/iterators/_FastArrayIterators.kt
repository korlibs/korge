package com.soywiz.kds.iterators

import com.soywiz.kds.DoubleArrayList
import com.soywiz.kds.FastArrayList
import com.soywiz.kds.FloatArrayList
import com.soywiz.kds.IntArrayList
import com.soywiz.kds.toFastList

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
