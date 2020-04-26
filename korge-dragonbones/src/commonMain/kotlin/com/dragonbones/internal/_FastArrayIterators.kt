package com.dragonbones.internal

inline fun <T> List<T>.fastForEach(callback: (T) -> Unit) {
	var n = 0
	while (n < size) {
		callback(this[n++])
	}
}

inline fun <T> Array<T>.fastForEach(callback: (T) -> Unit) {
	var n = 0
	while (n < size) {
		callback(this[n++])
	}
}
