package com.soywiz.korge.internal

@Deprecated("Use kds fastForEach")
@PublishedApi
internal inline fun <T> List<T>.fastForEachWithIndex(callback: (index: Int, value: T) -> Unit) {
	var n = 0
	while (n < size) {
		callback(n, this[n])
		n++
	}
}

@Deprecated("Use kds fastForEach")
@PublishedApi
internal inline fun <T> List<T>.fastForEach(callback: (T) -> Unit) {
	var n = 0
	while (n < size) {
		callback(this[n++])
	}
}

@Deprecated("Use kds fastForEach")
@PublishedApi
internal inline fun <T> Array<T>.fastForEach(callback: (T) -> Unit) {
	var n = 0
	while (n < size) {
		callback(this[n++])
	}
}

@Deprecated("Use kds fastForEach")
@PublishedApi
internal inline fun <T> List<T>.fastForEachReverse(callback: (T) -> Unit) {
	var n = 0
	while (n < size) {
		callback(this[size - n - 1])
		n++
	}
}
